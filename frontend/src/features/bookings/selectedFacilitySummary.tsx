import {Link} from 'react-router'
import {Button} from '@/components/ui/button'
import {formatCurrency} from '@/lib/format'
import type {FacilityResponse} from '@/types/api'

export function SelectedFacilitySummary({
	facility
}: {
	facility: FacilityResponse
}) {
	return (
		<div className='rounded-xl border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'>
			<div className='grid gap-3 sm:grid-cols-3'>
				<div>
					<div className='text-slate-500'>Facility</div>
					<div className='mt-1 text-white'>{facility.name}</div>
				</div>
				<div>
					<div className='text-slate-500'>Base price</div>
					<div className='mt-1 text-white'>
						{formatCurrency(facility.basePricePerHour)}/h
					</div>
				</div>
				<div>
					<div className='text-slate-500'>Hours</div>
					<div className='mt-1 text-white'>
						{facility.workingHoursStart} - {facility.workingHoursEnd}
					</div>
				</div>
			</div>
			<Link
				className='block w-full sm:w-auto'
				to={`/facilities/${facility.id}`}
			>
				<Button
					className='mt-4 w-full sm:w-auto'
					size='sm'
					type='button'
					variant='outline'
				>
					View details
				</Button>
			</Link>
		</div>
	)
}
