import {type FormEvent, useMemo, useState} from 'react'
import {
	useMutation,
	useQuery,
	useQueryClient
} from '@tanstack/react-query'
import {useNavigate} from 'react-router'
import {useAuth} from '@/auth/authContext'
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
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {createBooking} from '@/features/bookings/api'
import {
	getFacilityPriceQuote,
	listFacilities
} from '@/features/resources/api'
import {formatCurrency, getErrorMessage} from '@/lib/format'
import type {PaymentMethod} from '@/types/api'

export function BookingPage() {
	const {session} = useAuth()
	const navigate = useNavigate()
	const queryClient = useQueryClient()
	const [errorMessage, setErrorMessage] = useState<string | null>(null)
	const [form, setForm] = useState({
		endTime: '',
		facilityId: '',
		paymentMethod: 'CREDIT_CARD' as PaymentMethod,
		startTime: ''
	})

	const facilitiesQuery = useQuery({
		queryFn: listFacilities,
		queryKey: ['facilities']
	})

	const isTimeRangeValid = useMemo(() => {
		if (!form.startTime || !form.endTime) {
			return false
		}

		return new Date(form.endTime).valueOf() > new Date(form.startTime).valueOf()
	}, [form.endTime, form.startTime])

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

	const selectedFacility = useMemo(
		() =>
			(facilitiesQuery.data || []).find(
				facility => String(facility.id) === form.facilityId
			),
		[facilitiesQuery.data, form.facilityId]
	)

	const bookingMutation = useMutation({
		mutationFn: async () => {
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
			await queryClient.invalidateQueries({queryKey: ['bookings', session?.userId]})
			await queryClient.invalidateQueries({
				queryKey: ['notifications', session?.userId]
			})
			navigate('/dashboard')
		}
	})

	async function handleSubmit(event: FormEvent<HTMLFormElement>) {
		event.preventDefault()
		setErrorMessage(null)

		try {
			await bookingMutation.mutateAsync()
		} catch (error) {
			setErrorMessage(getErrorMessage(error))
		}
	}

	return (
		<div className='mx-auto max-w-3xl space-y-6'>
			<Card>
				<CardHeader>
					<CardTitle>Create a booking</CardTitle>
					<CardDescription>
						The frontend quotes the slot through Resource Service and then sends
						the booking to the orchestrated Booking endpoint, where price is
						confirmed again and payment is triggered.
					</CardDescription>
				</CardHeader>
				<CardContent>
					{facilitiesQuery.isPending ? (
						<LoadingOrError title='Loading facilities' />
					) : facilitiesQuery.isError ? (
						<LoadingOrError error={facilitiesQuery.error} />
					) : (
						<form className='space-y-5' onSubmit={handleSubmit}>
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
									{facilitiesQuery.data
										.filter(facility => facility.status === 'ACTIVE')
										.map(facility => (
											<option key={facility.id} value={facility.id}>
												{facility.name} ({facility.type})
											</option>
										))}
								</select>
							</div>

							<div className='grid gap-5 md:grid-cols-2'>
								<div className='space-y-2'>
									<Label htmlFor='startTime'>Start time</Label>
									<Input
										id='startTime'
										onChange={event =>
											setForm(currentForm => ({
												...currentForm,
												startTime: event.target.value
											}))
										}
										required={true}
										type='datetime-local'
										value={form.startTime}
									/>
								</div>

								<div className='space-y-2'>
									<Label htmlFor='endTime'>End time</Label>
									<Input
										id='endTime'
										onChange={event =>
											setForm(currentForm => ({
												...currentForm,
												endTime: event.target.value
											}))
										}
										required={true}
										type='datetime-local'
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
									End time must be after start time.
								</Alert>
							) : null}

							{quoteQuery.isPending ? (
								<LoadingOrError title='Calculating price' />
							) : quoteQuery.isError ? (
								<LoadingOrError error={quoteQuery.error} />
							) : quoteQuery.data ? (
								<Alert variant='success'>
									<div className='font-medium'>
										Quoted total: {formatCurrency(quoteQuery.data.totalPrice)}
									</div>
									<div className='mt-1 text-sm'>
										{quoteQuery.data.hours} hours × multiplier{' '}
										{quoteQuery.data.multiplier}
									</div>
								</Alert>
							) : null}

							{selectedFacility ? (
								<div className='rounded-xl border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'>
									<div className='flex items-center justify-between'>
										<span>Facility</span>
										<span>{selectedFacility.name}</span>
									</div>
									<div className='mt-2 flex items-center justify-between'>
										<span>Base price</span>
										<span>{formatCurrency(selectedFacility.basePricePerHour)}/h</span>
									</div>
								</div>
							) : null}

							{errorMessage ? <Alert variant='destructive'>{errorMessage}</Alert> : null}

							<Button
								className='w-full'
								disabled={
									bookingMutation.isPending ||
									!selectedFacility ||
									!quoteQuery.data ||
									!isTimeRangeValid
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
