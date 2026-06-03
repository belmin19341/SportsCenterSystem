import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {useState} from 'react'
import {useAuth} from '@/auth/authContext'
import {useFeedback} from '@/components/feedback'
import {
	createFacility,
	listFacilities,
	listOwnerFacilities,
	updateFacility
} from '@/features/resources/api'
import {getErrorMessage} from '@/lib/format'
import {
	validateBasePriceField,
	validateCapacityField,
	validateFacilityNameField,
	validateWorkingHoursField
} from '@/lib/validation'
import type {FacilityRequest, FacilityResponse} from '@/types/api'

export interface FacilityFieldErrors {
	name: string | null
	capacity: string | null
	basePricePerHour: string | null
	workingHours: string | null
}

function emptyFieldErrors(): FacilityFieldErrors {
	return {basePricePerHour: null, capacity: null, name: null, workingHours: null}
}

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
	}
}

function facilityToForm(facility: FacilityResponse): FacilityRequest {
	return {
		basePricePerHour: facility.basePricePerHour,
		capacity: facility.capacity,
		description: facility.description ?? '',
		imageUrl: facility.imageUrl ?? '',
		name: facility.name,
		ownerId: facility.ownerId,
		status: facility.status,
		type: facility.type,
		workingHoursEnd: facility.workingHoursEnd,
		workingHoursStart: facility.workingHoursStart
	}
}

function normalizeForm(form: FacilityRequest, ownerId: number): FacilityRequest {
	return {
		...form,
		description: form.description?.trim() || null,
		imageUrl: form.imageUrl?.trim() || null,
		name: form.name.trim(),
		ownerId
	}
}

function validateFields(form: FacilityRequest): FacilityFieldErrors {
	return {
		basePricePerHour: validateBasePriceField(form.basePricePerHour),
		capacity: validateCapacityField(form.capacity),
		name: validateFacilityNameField(form.name),
		workingHours: validateWorkingHoursField(
			form.workingHoursStart,
			form.workingHoursEnd
		)
	}
}

export function useOwnerFacilitiesData() {
	const {session} = useAuth()
	const {showFeedback} = useFeedback()
	const queryClient = useQueryClient()
	const ownerId = session?.userId ?? 0

	// ── Edit mode ──────────────────────────────────────────────────────────────
	const [editingFacility, setEditingFacility] = useState<FacilityResponse | null>(null)
	const isEditing = editingFacility !== null

	// ── Create form ────────────────────────────────────────────────────────────
	const [createForm, setCreateForm] = useState<FacilityRequest>(() =>
		createDefaultForm(ownerId)
	)
	const [createFieldErrors, setCreateFieldErrors] =
		useState<FacilityFieldErrors>(emptyFieldErrors)
	const [createApiError, setCreateApiError] = useState<string | null>(null)

	// ── Edit form ──────────────────────────────────────────────────────────────
	const [editForm, setEditForm] = useState<FacilityRequest>(() =>
		createDefaultForm(ownerId)
	)
	const [editFieldErrors, setEditFieldErrors] =
		useState<FacilityFieldErrors>(emptyFieldErrors)
	const [editApiError, setEditApiError] = useState<string | null>(null)

	// ── Active form (unified surface for the page) ─────────────────────────────
	const activeForm = isEditing ? editForm : createForm
	const setActiveForm = isEditing ? setEditForm : setCreateForm
	const activeFieldErrors = isEditing ? editFieldErrors : createFieldErrors
	const activeApiError = isEditing ? editApiError : createApiError

	// ── Queries ────────────────────────────────────────────────────────────────
	const facilitiesQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () =>
			session?.role === 'ADMIN'
				? listFacilities()
				: listOwnerFacilities(ownerId),
		queryKey: ['owner-facilities', ownerId, session?.role]
	})

	// ── Mutations ──────────────────────────────────────────────────────────────
	const createMutation = useMutation({
		mutationFn: (input: FacilityRequest) => createFacility(input),
		onSuccess: async () => {
			setCreateForm(createDefaultForm(ownerId))
			setCreateFieldErrors(emptyFieldErrors())
			showFeedback({
				description: 'The facility list and public catalog were refreshed.',
				title: 'Facility created',
				variant: 'success'
			})
			await queryClient.invalidateQueries({queryKey: ['owner-facilities']})
			await queryClient.invalidateQueries({queryKey: ['facilities']})
		}
	})

	const updateMutation = useMutation({
		mutationFn: ({facilityId, payload}: {facilityId: number; payload: FacilityRequest}) =>
			updateFacility(facilityId, payload),
		onSuccess: async () => {
			setEditingFacility(null)
			setEditFieldErrors(emptyFieldErrors())
			showFeedback({
				description: 'Changes were saved and the facility list was refreshed.',
				title: 'Facility updated',
				variant: 'success'
			})
			await queryClient.invalidateQueries({queryKey: ['owner-facilities']})
			await queryClient.invalidateQueries({queryKey: ['facilities']})
		}
	})

	const isSubmitting = isEditing ? updateMutation.isPending : createMutation.isPending

	// ── Blur handler ───────────────────────────────────────────────────────────
	function handleBlur(field: keyof FacilityFieldErrors) {
		const form = isEditing ? editForm : createForm
		const prev = isEditing ? editFieldErrors : createFieldErrors
		const next = {...prev}

		switch (field) {
			case 'name':
				next.name = validateFacilityNameField(form.name)
				break
			case 'capacity':
				next.capacity = validateCapacityField(form.capacity)
				break
			case 'basePricePerHour':
				next.basePricePerHour = validateBasePriceField(form.basePricePerHour)
				break
			case 'workingHours':
				next.workingHours = validateWorkingHoursField(
					form.workingHoursStart,
					form.workingHoursEnd
				)
				break
		}

		if (isEditing) setEditFieldErrors(next)
		else setCreateFieldErrors(next)
	}

	// ── Submit handlers ────────────────────────────────────────────────────────
	const handleCreateSubmit = async (e: {preventDefault(): void}) => {
		e.preventDefault()
		setCreateApiError(null)
		const payload = normalizeForm(createForm, ownerId)
		const errors = validateFields(payload)
		setCreateFieldErrors(errors)
		if (Object.values(errors).some(err => err !== null)) return
		try {
			await createMutation.mutateAsync(payload)
		} catch (error) {
			setCreateApiError(getErrorMessage(error))
		}
	}

	const handleEditSubmit = async (e: {preventDefault(): void}) => {
		e.preventDefault()
		if (!editingFacility) return
		setEditApiError(null)
		const payload = normalizeForm(editForm, editingFacility.ownerId)
		const errors = validateFields(payload)
		setEditFieldErrors(errors)
		if (Object.values(errors).some(err => err !== null)) return
		try {
			await updateMutation.mutateAsync({facilityId: editingFacility.id, payload})
		} catch (error) {
			setEditApiError(getErrorMessage(error))
		}
	}

	const handleSubmit = isEditing ? handleEditSubmit : handleCreateSubmit

	// ── Edit lifecycle ─────────────────────────────────────────────────────────
	function handleStartEdit(facility: FacilityResponse) {
		setEditingFacility(facility)
		setEditForm(facilityToForm(facility))
		setEditFieldErrors(emptyFieldErrors())
		setEditApiError(null)
	}

	function handleCancelEdit() {
		setEditingFacility(null)
		setEditFieldErrors(emptyFieldErrors())
		setEditApiError(null)
	}

	return {
		session,
		facilitiesQuery,
		activeForm,
		setActiveForm,
		activeFieldErrors,
		activeApiError,
		isSubmitting,
		isEditing,
		editingFacility,
		handleBlur,
		handleSubmit,
		handleStartEdit,
		handleCancelEdit
	}
}
