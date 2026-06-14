import {Elements} from '@stripe/react-stripe-js'
import {loadStripe} from '@stripe/stripe-js'
import {useBookingData} from '@/hooks/useBookingData'
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
import {BookingBasicsFields} from '@/features/bookings/bookingBasicsFields'
import {BookingScheduleFields} from '@/features/bookings/bookingScheduleFields'
import {PaymentForm} from '@/features/bookings/paymentForm'
import {
	formatDuration,
	formatRateSummary
} from '@/features/bookings/pricingCopy'
import {SelectedFacilitySummary} from '@/features/bookings/selectedFacilitySummary'
import {formatCurrency} from '@/lib/format'

const publishableKey = import.meta.env.VITE_STRIPE_PUBLISHABLE_KEY as string | undefined
const stripePromise = publishableKey ? loadStripe(publishableKey) : null

const elementsOptions = {
	appearance: {theme: 'night' as const}
}

export function BookingPage() {
	const {
		form, setForm, facilitiesQuery, savedCardsQuery, paymentFormRef,
		quoteQuery, conflictsQuery, selectedFacility,
		isTimeRangeValid, minimumBookingDateTime, minimumEndDateTime, bookingValidationErrors,
		errorMessage, hasSubmitted, handleStartTimeChange, handleSubmit, isPending
	} = useBookingData()

	const savedCards = savedCardsQuery.data ?? []

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
							<BookingBasicsFields
								facilities={facilitiesQuery.data}
								facilityId={form.facilityId}
								onChangeFacilityId={nextFacilityId =>
									setForm(currentForm => ({
										...currentForm,
										facilityId: nextFacilityId
									}))
								}
							/>

							<BookingScheduleFields
								endTime={form.endTime}
								minimumBookingDateTime={minimumBookingDateTime}
								minimumEndDateTime={minimumEndDateTime}
								onChangeEndTime={nextEndTime =>
									setForm(currentForm => ({
										...currentForm,
										endTime: nextEndTime
									}))
								}
								onChangeStartTime={handleStartTimeChange}
								startTime={form.startTime}
								workingHoursEnd={selectedFacility?.workingHoursEnd}
								workingHoursStart={selectedFacility?.workingHoursStart}
							/>

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

							{/* Payment section */}
							<div className='rounded-lg border border-slate-800 bg-slate-900/50 p-4'>
								<Elements options={elementsOptions} stripe={stripePromise}>
									<PaymentForm
										ref={paymentFormRef}
										savedCards={savedCards}
										stripeReady={Boolean(stripePromise)}
									/>
								</Elements>
							</div>

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
									isPending ||
									!selectedFacility ||
									!quoteQuery.data ||
									!isTimeRangeValid ||
									(conflictsQuery.data?.length ?? 0) > 0
								}
								size='lg'
								type='submit'
							>
								{isPending ? 'Creating booking...' : 'Book now'}
							</Button>
						</form>
					)}
				</CardContent>
			</Card>
		</div>
	)
}
