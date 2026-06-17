import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router';
import { useAuth } from '@/auth/authContext';
import { useFeedback } from '@/components/feedback';
import { createReview, listReviewsForEntity, listUserBookings } from '@/features/bookings/api';
import { getFacility, listEquipmentByFacility, listPricingRulesForFacility } from '@/features/resources/api';
import { getErrorMessage } from '@/lib/format';
import { validateReviewForm } from '@/lib/validation';

export function useFacilityData() {
	const { facilityId } = useParams();
	const numericFacilityId = Number(facilityId);
	const { session } = useAuth();
	const { showFeedback } = useFeedback();
	const queryClient = useQueryClient();

	const [formError, setFormError] = useState<string | null>(null);
	const [reviewForm, setReviewForm] = useState({ comment: '', rating: 5 });

	const facilityQuery = useQuery({
		enabled: Number.isFinite(numericFacilityId),
		queryFn: () => getFacility(numericFacilityId),
		queryKey: ['facility', numericFacilityId]
	});

	const equipmentQuery = useQuery({
		enabled: Number.isFinite(numericFacilityId),
		queryFn: () => listEquipmentByFacility(numericFacilityId),
		queryKey: ['equipment', 'facility', numericFacilityId]
	});

	const pricingRulesQuery = useQuery({
		enabled: Number.isFinite(numericFacilityId),
		queryFn: () => listPricingRulesForFacility(numericFacilityId),
		queryKey: ['pricing-rules', numericFacilityId]
	});

	const reviewsQuery = useQuery({
		enabled: Boolean(session) && Number.isFinite(numericFacilityId),
		queryFn: () => listReviewsForEntity('FACILITY', numericFacilityId),
		queryKey: ['reviews', 'FACILITY', numericFacilityId]
	});

	const bookingsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserBookings(session?.userId ?? 0),
		queryKey: ['bookings', session?.userId]
	});

	const canReview = useMemo(() => 
		Boolean(session && bookingsQuery.data?.some(b => b.facilityId === numericFacilityId && b.status === 'COMPLETED')),
	[bookingsQuery.data, numericFacilityId, session]);

	const averageRating = useMemo(() => {
		const reviews = reviewsQuery.data || [];
		if (reviews.length === 0) return null;
		return reviews.reduce((total, review) => total + review.rating, 0) / reviews.length;
	}, [reviewsQuery.data]);

	const reviewMutation = useMutation({
		mutationFn: () => {
			if (!session) throw new Error('Sign in before submitting a review.');
			return createReview({
				comment: reviewForm.comment.trim() || null,
				rating: reviewForm.rating,
				reviewedEntityId: numericFacilityId,
				reviewedEntityType: 'FACILITY',
				reviewerId: session.userId
			});
		},
		onSuccess: () => {
			setReviewForm({ comment: '', rating: 5 });
			showFeedback({ description: 'Your review was saved.', title: 'Review submitted', variant: 'success' });
			return queryClient.invalidateQueries({ queryKey: ['reviews', 'FACILITY', numericFacilityId] });
		}
	});

	const handleReviewSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setFormError(null);
		const validationErrors = validateReviewForm(reviewForm);
		if (validationErrors.length > 0) {
			setFormError(validationErrors.join(' '));
			return;
		}
		try {
			await reviewMutation.mutateAsync();
		} catch (error) {
			setFormError(getErrorMessage(error));
		}
	};

	return {
		numericFacilityId,
		session,
		facilityQuery,
		equipmentQuery,
		pricingRulesQuery,
		reviewsQuery,
		canReview,
		averageRating,
		reviewForm,
		setReviewForm,
		formError,
		reviewMutation,
		handleReviewSubmit
	};
}