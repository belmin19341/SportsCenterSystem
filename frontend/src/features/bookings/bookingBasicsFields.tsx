import {Label} from '@/components/ui/label'
import type {PaymentMethod} from '@/types/api'

interface BookingBasicsFieldsProps {
	facilities: Array<{
		id: number
		name: string
		status: string
		type: string
	}>
	facilityId: string
	onChangeFacilityId: (value: string) => void
	onChangePaymentMethod: (value: PaymentMethod) => void
	paymentMethod: PaymentMethod
}

export function BookingBasicsFields({
	facilities,
	facilityId,
	onChangeFacilityId,
	onChangePaymentMethod,
	paymentMethod
}: BookingBasicsFieldsProps) {
	return (
		<>
			<div className='space-y-2'>
				<Label htmlFor='facilityId'>Facility</Label>
				<select
					className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
					id='facilityId'
					onChange={event => onChangeFacilityId(event.target.value)}
					required={true}
					value={facilityId}
				>
					<option value=''>Choose a facility</option>
					{facilities.map(facility => (
						<option key={facility.id} value={facility.id}>
							{facility.name} ({facility.type}, {facility.status})
						</option>
					))}
				</select>
			</div>

			<div className='space-y-2'>
				<Label htmlFor='paymentMethod'>Payment method</Label>
				<select
					className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
					id='paymentMethod'
					onChange={event =>
						onChangePaymentMethod(event.target.value as PaymentMethod)
					}
					value={paymentMethod}
				>
					<option value='CREDIT_CARD'>Credit card</option>
					<option value='DEBIT_CARD'>Debit card</option>
					<option value='PAYPAL'>PayPal</option>
				</select>
			</div>
		</>
	)
}