import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { listEquipment, listFacilities } from '@/features/resources/api';
import { useAuth } from '@/auth/authContext';
import type { FacilityStatus, FacilityType } from '@/types/api';

export function useHomeData() {
	const { isSignedIn } = useAuth();
	const [filters, setFilters] = useState<{
		q: string;
		status: FacilityStatus | '';
		type: FacilityType | '';
	}>({ q: '', status: 'ACTIVE', type: '' });

	const facilitiesQuery = useQuery({
		queryFn: () => listFacilities(filters),
		queryKey: ['facilities', filters]
	});

	const equipmentQuery = useQuery({
		queryFn: listEquipment,
		queryKey: ['equipment']
	});

	return {
		isSignedIn,
		filters,
		setFilters,
		facilitiesQuery,
		equipmentQuery,
		facilities: facilitiesQuery.data || [],
		equipment: equipmentQuery.data?.slice(0, 6) || []
	};
}