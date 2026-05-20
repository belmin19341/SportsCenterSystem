import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {type FormEvent, useEffect, useMemo, useState} from 'react'
import {useNavigate, useSearchParams} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {useFeedback} from '@/components/feedback'
import {LoadingOrError} from '@/components/loadingOrError'
import {Alert} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'
import {DateTimePicker} from '@/components/ui/dateTimePicker'
import {Label} from '@/components/ui/label'
import {createBooking, listConflictingBookings} from '@/features/bookings/api'
import {
	type BookingDraft,
	clearBookingDraft,
	createInitialBookingDraft,
	saveBookingDraft
} from '@/features/bookings/bookingDraft'
import {
	formatDuration,
	formatRateSummary
} from '@/features/bookings/pricingCopy'
import {SelectedFacilitySummary} from '@/features/bookings/selectedFacilitySummary'
import {getSuggestedEndTime, isValidDateRange} from '@/features/bookings/timeRange'
import {getFacilityPriceQuote, listFacilities} from '@/features/resources/api'
import {formatCurrency, getErrorMessage} from '@/lib/format'
import {
	addLocalDateTimeMinutes,
	formatTimeRange,
	normalizeTimeValue,
	parseLocalDateTime,
	roundDateUp,
	toLocalDateTimeValue
} from '@/lib/localDateTime'
import {validateBookingForm} from '@/lib/validation'
import type {PaymentMethod} from '@/types/api'

export function BookingPage() {
	const {session} = useAuth()
	const {showFeedback} = useFeedback()
	const navigate = useNavigate()
	const [searchParams] = useSearchParams()
	const requestedFacilityId = searchParams.get('facilityId') || ''
	const queryClient = useQueryClient()
	const [errorMessage, setErrorMessage] = useState<string | null>(null)
	const [hasSubmitted, setHasSubmitted] = useState(false)
	const [form, setForm] = useState<BookingDraft>(() =>
		createInitialBookingDraft(requestedFacilityId)
	)

	useEffect(() => {
		if (!requestedFacilityId) {
			return
		}

		setForm(currentForm =>
			currentForm.facilityId === requestedFacilityId
				? currentForm
				: {...currentForm, facilityId: requestedFacilityId}
		)
	}, [requestedFacilityId])

	useEffect(() => {
		saveBookingDraft(form)
	}, [form])

	const facilitiesQuery = useQuery({
		queryFn: () => listFacilities(),
		queryKey: ['facilities']
	})

	const isTimeRangeValid = useMemo(
		() => isValidDateRange(form.startTime, form.endTime),
		[form.endTime, form.startTime]
	)

	const quoteQuery = useQuery({
		enabled: Boolean(form.facilityId) && isTimeRangeValid,
		queryFn: () =>
			getFacilityPriceQuote({
				end: form.endTime,
				facilityId: Number(form.facilityId),
				start: form.startTime
			}),
		queryKey: ['price-quote', form.facilityId, form.startTime, form.endTime]
	})
	const conflictsQuery = useQuery({
		enabled: Boolean(form.facilityId) && isTimeRangeValid,
		queryFn: () =>
			listConflictingBookings({
				end: form.endTime,
				facilityId: Number(form.facilityId),
				start: form.startTime
			}),
		queryKey: [
			'booking-conflicts',
			form.facilityId,
			form.startTime,
			form.endTime
		]
	})

	const selectedFacility = useMemo(
		() =>
			(facilitiesQuery.data || []).find(
				facility => String(facility.id) === form.facilityId
			),
		[facilitiesQuery.data, form.facilityId]
	)
		const facilityHoursLabel = selectedFacility
			? formatTimeRange(
					selectedFacility.workingHoursStart,
					selectedFacility.workingHoursEnd
			  )
			: ''
		const minimumBookingDateTime = useMemo(
			() => toLocalDateTimeValue(roundDateUp(new Date(Date.now() + 5 * 60_000), 15)),
			[]
		)
		const minimumEndDateTime =
			addLocalDateTimeMinutes(form.startTime, 1) || minimumBookingDateTime
	const bookingValidationErrors = useMemo(
		() =>
			validateBookingForm({
				conflictCount: conflictsQuery.data?.length ?? 0,
				endTime: form.endTime,
				facility: selectedFacility,
				quote: quoteQuery.data,
				startTime: form.startTime
			}),
		[
			conflictsQuery.data?.length,
			form.endTime,
			form.startTime,
			quoteQuery.data,
			selectedFacility
		]
	)

	function handleStartTimeChange(nextStartTime: string) {
		setForm(currentForm => {
			if (!nextStartTime) {
				return {...currentForm, endTime: '', startTime: ''}
			}

			const parsedCurrentEndTime = parseLocalDateTime(currentForm.endTime)
			const parsedNextStartTime = parseLocalDateTime(nextStartTime)
			const shouldSuggestEndTime =
				!(parsedCurrentEndTime && parsedNextStartTime) ||
				parsedCurrentEndTime.valueOf() <= parsedNextStartTime.valueOf()

			return {
				...currentForm,
				endTime: shouldSuggestEndTime
					? getSuggestedEndTime(nextStartTime, selectedFacility?.workingHoursEnd)
					: currentForm.endTime,
				startTime: nextStartTime
			}
		})
	}

	const bookingMutation = useMutation({
		mutationFn: () => {
			const validationErrors = validateBookingForm({
				conflictCount: conflictsQuery.data?.length ?? 0,
				endTime: form.endTime,
				facility: selectedFacility,
				quote: quoteQuery.data,
				startTime: form.startTime
			})

			if (validationErrors.length > 0) {
				throw new Error(validationErrors.join(' '))
			}

			if (!session) {
				throw new Error('You must be signed in to create a booking.')
			}

			if (!selectedFacility) {
				throw new Error('Choose a facility before continuing.')
			}

			if (!quoteQuery.data) {
				throw new Error('A valid price quote is required before booking.')
			}

			return createBooking({
				endTime: form.endTime,
				facilityId: selectedFacility.id,
				isRecurring: false,
				paymentMethod: form.paymentMethod,
				recurringPattern: null,
				startTime: form.startTime,
				status: 'PENDING',
				totalPrice: quoteQuery.data.totalPrice,
				userId: session.userId
			})
		},
		async onSuccess() {
			clearBookingDraft()
			await queryClient.invalidateQueries({
				queryKey: ['bookings', session?.userId]
			})
			await queryClient.invalidateQueries({queryKey: ['payments']})
			await queryClient.invalidateQueries({
				queryKey: ['loyalty', session?.userId]
			})
			await queryClient.invalidateQueries({
				queryKey: ['notifications', session?.userId]
			})
			showFeedback({
				description:
					'The booking, payment, loyalty, and notification data were refreshed.',
				title: 'Booking created',
				variant: 'success'
			})
			navigate('/dashboard')
		}
	})

	async function handleSubmit(event: FormEvent<HTMLFormElement>) {
		event.preventDefault()
		setHasSubmitted(true)
		setErrorMessage(null)

		if (bookingValidationErrors.length > 0) {
			setErrorMessage(bookingValidationErrors.join(' '))
			return
		}

		try {
			await bookingMutation.mutateAsync()
		} catch (error) {
			setErrorMessage(getErrorMessage(error))
		}
	}

	return (
		<div className='mx-auto w-full max-w-4xl space-y-5 sm:space-y-6'>
			<Card>
				<CardHeader>
					<CardTitle>Create a booking</CardTitle>
					<CardDescription>
						Choose a facility and time window, then complete the reservation.
					</CardDescription>
				</CardHeader>
				<CardContent>
					{facilitiesQuery.isPending ? (
						<LoadingOrError title='Loading facilities' />
					) : facilitiesQuery.isError ? (
						<LoadingOrError error={facilitiesQuery.error} />
					) : (
						<form className='space-y-5 sm:space-y-6' onSubmit={handleSubmit}>
							<div className='space-y-2'>
								<Label htmlFor='facilityId'>Facility</Label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityId'
									onChange={event =>
										setForm(currentForm => ({
											...currentForm,
											facilityId: event.target.value
										}))
									}
									required={true}
									value={form.facilityId}
								>
									<option value=''>Choose a facility</option>
									{facilitiesQuery.data.map(facility => (
										<option key={facility.id} value={facility.id}>
											{facility.name} ({facility.type}, {facility.status})
										</option>
									))}
								</select>
							</div>

							<div className='grid gap-5 md:grid-cols-2'>
								<div className='space-y-2'>
									<Label htmlFor='startTime'>Start time</Label>
									<DateTimePicker
										caption={
											facilityHoursLabel
												? `Available ${facilityHoursLabel}`
												: undefined
										}
										id='startTime'
										maxTime={normalizeTimeValue(selectedFacility?.workingHoursEnd)}
										minTime={normalizeTimeValue(selectedFacility?.workingHoursStart)}
										minValue={minimumBookingDateTime}
										onChange={handleStartTimeChange}
										placeholder='Choose when the booking should begin'
										quickStepMinutes={60}
										value={form.startTime}
									/>
								</div>

								<div className='space-y-2'>
									<Label htmlFor='endTime'>End time</Label>
									<DateTimePicker
										caption={
											facilityHoursLabel
												? `Finish inside ${facilityHoursLabel}`
												: undefined
										}
										disabled={!form.startTime}
										id='endTime'
										maxTime={normalizeTimeValue(selectedFacility?.workingHoursEnd)}
										minTime={normalizeTimeValue(selectedFacility?.workingHoursStart)}
										minValue={minimumEndDateTime}
										onChange={nextEndTime =>
											setForm(currentForm => ({
												...currentForm,
												endTime: nextEndTime
											}))
										}
										placeholder={
											form.startTime
												? 'Choose when the booking should end'
												: 'Select the start time first'
										}
										quickStepMinutes={60}
										value={form.endTime}
									/>
								</div>
							</div>

							<div className='space-y-2'>
								<Label htmlFor='paymentMethod'>Payment method</Label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='paymentMethod'
									onChange={event =>
										setForm(currentForm => ({
											...currentForm,
											paymentMethod: event.target.value as PaymentMethod
										}))
									}
									value={form.paymentMethod}
								>
									<option value='CREDIT_CARD'>Credit card</option>
									<option value='DEBIT_CARD'>Debit card</option>
									<option value='PAYPAL'>PayPal</option>
								</select>
							</div>

							{!isTimeRangeValid && form.startTime && form.endTime ? (
								<Alert variant='destructive'>
									Start time must be in the future and end time must be after
									start.
								</Alert>
							) : null}

							{isTimeRangeValid && conflictsQuery.isPending ? (
								<LoadingOrError title='Checking conflicts' />
							) : conflictsQuery.isError ? (
								<LoadingOrError error={conflictsQuery.error} />
							) : conflictsQuery.data && conflictsQuery.data.length > 0 ? (
								<Alert variant='destructive'>
									<div className='font-medium'>
										Selected time is not available.
									</div>
									<div className='mt-1 text-sm'>
										{conflictsQuery.data.length} conflicting booking
										{conflictsQuery.data.length === 1 ? '' : 's'} found.
									</div>
								</Alert>
							) : isTimeRangeValid && conflictsQuery.data ? (
								<Alert variant='success'>Selected time is available.</Alert>
							) : null}

							{isTimeRangeValid && quoteQuery.isPending ? (
								<LoadingOrError title='Calculating price' />
							) : quoteQuery.isError ? (
								<LoadingOrError error={quoteQuery.error} />
							) : quoteQuery.data ? (
								<Alert variant='success'>
									<div className='font-medium'>
										Quoted total: {formatCurrency(quoteQuery.data.totalPrice)}
									</div>
									<div className='mt-1 text-sm'>
										Duration: {formatDuration(quoteQuery.data.hours)}.
									</div>
									<div className='mt-1 text-sm'>
										{formatRateSummary(quoteQuery.data.multiplier)}
									</div>
								</Alert>
							) : null}

							{selectedFacility ? (
								<SelectedFacilitySummary facility={selectedFacility} />
							) : null}

							{hasSubmitted && bookingValidationErrors.length > 0 ? (
								<Alert variant='destructive'>
									<div className='font-medium'>Check booking details</div>
									<ul className='mt-2 list-inside list-disc space-y-1'>
										{bookingValidationErrors.map(error => (
											<li key={error}>{error}</li>
										))}
									</ul>
								</Alert>
							) : null}

							{errorMessage ? (
								<Alert variant='destructive'>{errorMessage}</Alert>
							) : null}

							<Button
								className='w-full'
								disabled={
									bookingMutation.isPending ||
									conflictsQuery.isFetching ||
									quoteQuery.isFetching ||
									!selectedFacility ||
									!quoteQuery.data ||
									!isTimeRangeValid ||
									(conflictsQuery.data?.length ?? 0) > 0
								}
								size='lg'
								type='submit'
							>
								{bookingMutation.isPending ? 'Creating booking...' : 'Book now'}
							</Button>
						</form>
					)}
				</CardContent>
			</Card>
		</div>
	)
}
