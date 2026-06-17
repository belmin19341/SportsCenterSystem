import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { useAuth } from '@/auth/authContext';
import { useFeedback } from '@/components/feedback';
import { createBooking, getSavedCards, listConflictingBookings } from '@/features/bookings/api';
import type { PaymentFormHandle } from '@/features/bookings/paymentForm';
import {
	type BookingDraft,
	clearBookingDraft,
	createInitialBookingDraft,
	saveBookingDraft
} from '@/features/bookings/bookingDraft';
import {
	applyStartTimeToDraft,
	createMinimumBookingDateTime,
	getMinimumEndDateTime
} from '@/features/bookings/bookingScheduleLogic';
import { isValidDateRange } from '@/features/bookings/timeRange';
import { getFacilityPriceQuote, listFacilities } from '@/features/resources/api';
import { validateBookingForm } from '@/lib/validation';
import { getErrorMessage } from '@/lib/format';

export function useBookingData() {
	const { session } = useAuth();
	const { showFeedback } = useFeedback();
	const navigate = useNavigate();
	const [searchParams] = useSearchParams();
	const requestedFacilityId = searchParams.get('facilityId') || '';
	const queryClient = useQueryClient();
	
	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [hasSubmitted, setHasSubmitted] = useState(false);
	const [form, setForm] = useState<BookingDraft>(() =>
		createInitialBookingDraft(requestedFacilityId)
	);
	const paymentFormRef = useRef<PaymentFormHandle>(null);

	useEffect(() => {
		if (requestedFacilityId) {
			setForm(current => current.facilityId === requestedFacilityId ? current : { ...current, facilityId: requestedFacilityId });
		}
	}, [requestedFacilityId]);

	useEffect(() => {
		saveBookingDraft(form);
	}, [form]);

	const facilitiesQuery = useQuery({
		queryFn: () => listFacilities(),
		queryKey: ['facilities']
	});

	const savedCardsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => getSavedCards(session!.userId),
		queryKey: ['saved-cards', session?.userId]
	});

	const isTimeRangeValid = useMemo(() => isValidDateRange(form.startTime, form.endTime), [form.endTime, form.startTime]);

	const quoteQuery = useQuery({
		enabled: Boolean(form.facilityId) && isTimeRangeValid,
		queryFn: () => getFacilityPriceQuote({ end: form.endTime, facilityId: Number(form.facilityId), start: form.startTime }),
		queryKey: ['price-quote', form.facilityId, form.startTime, form.endTime]
	});

	const conflictsQuery = useQuery({
		enabled: Boolean(form.facilityId) && isTimeRangeValid,
		queryFn: () => listConflictingBookings({ end: form.endTime, facilityId: Number(form.facilityId), start: form.startTime }),
		queryKey: ['booking-conflicts', form.facilityId, form.startTime, form.endTime]
	});

	const selectedFacility = useMemo(() => 
		(facilitiesQuery.data || []).find((f: any) => String(f.id) === form.facilityId),
	[facilitiesQuery.data, form.facilityId]);

	const minimumBookingDateTime = useMemo(createMinimumBookingDateTime, []);
	const minimumEndDateTime = getMinimumEndDateTime(form.startTime, minimumBookingDateTime);

	const bookingValidationErrors = useMemo(() => validateBookingForm({
		conflictCount: conflictsQuery.data?.length ?? 0,
		endTime: form.endTime,
		facility: selectedFacility,
		quote: quoteQuery.data,
		startTime: form.startTime
	}), [conflictsQuery.data?.length, form.endTime, form.startTime, quoteQuery.data, selectedFacility]);

	const handleStartTimeChange = (nextStartTime: string) => {
		setForm(current => applyStartTimeToDraft(current, nextStartTime, selectedFacility?.workingHoursEnd));
	};

	const bookingMutation = useMutation({
		mutationFn: async () => {
			if (!session) throw new Error('You must be signed in to create a booking.');
			if (!selectedFacility) throw new Error('Choose a facility before continuing.');
			if (!quoteQuery.data) throw new Error('A valid price quote is required.');

			const paymentData = await paymentFormRef.current?.getPaymentData();

			return createBooking({
				endTime: form.endTime,
				facilityId: selectedFacility.id,
				isRecurring: false,
				recurringPattern: null,
				startTime: form.startTime,
				status: 'PENDING',
				totalPrice: quoteQuery.data.totalPrice,
				userId: session.userId,
				paymentMethod: paymentData?.paymentMethod ?? 'CREDIT_CARD',
				stripeToken: paymentData?.stripeToken,
				savedCardId: paymentData?.savedCardId,
				saveCard: paymentData?.saveCard,
				cardLast4: paymentData?.cardLast4,
				cardBrand: paymentData?.cardBrand
			});
		},
		onSuccess: async () => {
			clearBookingDraft();
			await queryClient.invalidateQueries({ queryKey: ['bookings', session?.userId] });
			await queryClient.invalidateQueries({ queryKey: ['payments'] });
			await queryClient.invalidateQueries({ queryKey: ['loyalty', session?.userId] });
			await queryClient.invalidateQueries({ queryKey: ['notifications', session?.userId] });
			showFeedback({ description: 'The booking data was refreshed.', title: 'Booking created', variant: 'success' });
			navigate('/dashboard');
		}
	});

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setHasSubmitted(true);
		setErrorMessage(null);
		if (bookingValidationErrors.length > 0) {
			setErrorMessage(bookingValidationErrors.join(' '));
			return;
		}
		try {
			await bookingMutation.mutateAsync();
		} catch (err: any) {
			setErrorMessage(getErrorMessage(err));
		}
	};

	return {
		form,
		setForm,
		facilitiesQuery,
		savedCardsQuery,
		paymentFormRef,
		quoteQuery,
		conflictsQuery,
		selectedFacility,
		isTimeRangeValid,
		minimumBookingDateTime,
		minimumEndDateTime,
		bookingValidationErrors,
		errorMessage,
		hasSubmitted,
		handleStartTimeChange,
		handleSubmit,
		isPending: bookingMutation.isPending || conflictsQuery.isFetching || quoteQuery.isFetching
	};
}