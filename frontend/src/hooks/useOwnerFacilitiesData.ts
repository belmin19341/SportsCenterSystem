import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { useFeedback } from '@/components/feedback';
import { createFacility, listFacilities, listOwnerFacilities } from '@/features/resources/api';
import { getErrorMessage } from '@/lib/format';
import { validateFacilityForm } from '@/lib/validation';
import type { FacilityRequest } from '@/types/api';

function createDefaultForm(ownerId: number): FacilityRequest {
	return {
		basePricePerHour: 40,
		capacity: 4,
		description: '',
		imageUrl: '',
		name: '',
		ownerId,
		status: 'ACTIVE',
		type: 'PADEL',
		workingHoursEnd: '23:00',
		workingHoursStart: '08:00'
	};
}

export function useOwnerFacilitiesData() {
	const { session } = useAuth();
	const { showFeedback } = useFeedback();
	const queryClient = useQueryClient();
	const ownerId = session?.userId ?? 0;

	const [errorMessage, setErrorMessage] = useState<string | null>(null);
	const [form, setForm] = useState<FacilityRequest>(() => createDefaultForm(ownerId));

	const facilitiesQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => session?.role === 'ADMIN' ? listFacilities() : listOwnerFacilities(ownerId),
		queryKey: ['owner-facilities', ownerId, session?.role]
	});

	const createMutation = useMutation({
		mutationFn: (input: FacilityRequest) => createFacility(input),
		onSuccess: async () => {
			setForm(createDefaultForm(ownerId));
			showFeedback({ description: 'The facility list and public catalog were refreshed.', title: 'Facility created', variant: 'success' });
			await queryClient.invalidateQueries({ queryKey: ['owner-facilities'] });
			await queryClient.invalidateQueries({ queryKey: ['facilities'] });
		}
	});

	const handleSubmit = async (e: React.FormEvent) => {
		e.preventDefault();
		setErrorMessage(null);
		const payload = {
			...form,
			description: form.description?.trim() || null,
			imageUrl: form.imageUrl?.trim() || null,
			name: form.name.trim(),
			ownerId
		};
		const validationErrors = validateFacilityForm(payload);
		if (validationErrors.length > 0) {
			setErrorMessage(validationErrors.join(' '));
			return;
		}
		try {
			await createMutation.mutateAsync(payload);
		} catch (error) {
			setErrorMessage(getErrorMessage(error));
		}
	};

	return {
		session,
		facilitiesQuery,
		createMutation,
		form,
		setForm,
		errorMessage,
		handleSubmit
	};
}