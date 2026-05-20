import {useQuery} from '@tanstack/react-query'
import {useState} from 'react'
import {Link} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {LoadingOrError} from '@/components/loadingOrError'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'
import {listEquipment, listFacilities} from '@/features/resources/api'
import {formatCurrency} from '@/lib/format'
import type {FacilityStatus, FacilityType} from '@/types/api'

function humanizeLabel(value: string) {
	return value.replaceAll('_', ' ').toLowerCase()
}

const facilityTypes = [
	'FOOTBALL_5V5',
	'FOOTBALL_7V7',
	'PADEL',
	'TENNIS',
	'TABLE_TENNIS'
] satisfies FacilityType[]

const facilityStatuses = [
	'ACTIVE',
	'INACTIVE',
	'MAINTENANCE'
] satisfies FacilityStatus[]

export function HomePage() {
	const {isSignedIn} = useAuth()
	const [filters, setFilters] = useState<{
		q: string
		status: FacilityStatus | ''
		type: FacilityType | ''
	}>({q: '', status: 'ACTIVE', type: ''})
	const facilitiesQuery = useQuery({
		queryFn: () => listFacilities(filters),
		queryKey: ['facilities', filters]
	})
	const equipmentQuery = useQuery({
		queryFn: listEquipment,
		queryKey: ['equipment']
	})

	const facilities = facilitiesQuery.data || []
	const equipment = equipmentQuery.data?.slice(0, 6) || []

	return (
		<div className='space-y-8 sm:space-y-10'>
			<section className='grid gap-6 lg:grid-cols-[1.4fr,0.6fr]'>
				<Card className='overflow-hidden border-sky-500/20 bg-slate-950'>
					<CardHeader className='space-y-4'>
						<Badge>Sports center booking</Badge>
						<CardTitle className='max-w-2xl text-2xl leading-tight sm:text-4xl lg:text-5xl'>
							Find courts, compare availability signals, and reserve your next
							slot.
						</CardTitle>
						<CardDescription className='max-w-2xl text-base text-slate-300'>
							Browse active facilities and equipment, then sign in when you are
							ready to book.
						</CardDescription>
					</CardHeader>
					<CardContent className='flex flex-col gap-3 sm:flex-row sm:flex-wrap'>
						<Link
							className='w-full sm:w-auto'
							to={isSignedIn ? '/bookings/new' : '/login'}
						>
							<Button className='w-full sm:w-auto' size='lg'>
								{isSignedIn ? 'Create a booking' : 'Sign in to continue'}
							</Button>
						</Link>
						<Link
							className='w-full sm:w-auto'
							to={isSignedIn ? '/dashboard' : '/login'}
						>
							<Button className='w-full sm:w-auto' size='lg' variant='outline'>
								{isSignedIn ? 'Open dashboard' : 'View the app shell'}
							</Button>
						</Link>
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Quick filters</CardTitle>
						<CardDescription>Refine the facility list.</CardDescription>
					</CardHeader>
					<CardContent className='space-y-4'>
						<div className='space-y-2'>
							<label
								className='text-sm font-medium text-slate-200'
								htmlFor='facilitySearch'
							>
								Search
							</label>
							<input
								className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
								id='facilitySearch'
								onChange={event =>
									setFilters(current => ({...current, q: event.target.value}))
								}
								placeholder='Padel, tennis, football...'
								value={filters.q}
							/>
						</div>
						<div className='grid gap-3 sm:grid-cols-2'>
							<div className='space-y-2'>
								<label
									className='text-sm font-medium text-slate-200'
									htmlFor='facilityType'
								>
									Type
								</label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityType'
									onChange={event =>
										setFilters(current => ({
											...current,
											type: event.target.value as FacilityType | ''
										}))
									}
									value={filters.type}
								>
									<option value=''>All types</option>
									{facilityTypes.map(type => (
										<option key={type} value={type}>
											{humanizeLabel(type)}
										</option>
									))}
								</select>
							</div>
							<div className='space-y-2'>
								<label
									className='text-sm font-medium text-slate-200'
									htmlFor='facilityStatus'
								>
									Status
								</label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityStatus'
									onChange={event =>
										setFilters(current => ({
											...current,
											status: event.target.value as FacilityStatus | ''
										}))
									}
									value={filters.status}
								>
									<option value=''>All statuses</option>
									{facilityStatuses.map(status => (
										<option key={status} value={status}>
											{humanizeLabel(status)}
										</option>
									))}
								</select>
							</div>
						</div>
					</CardContent>
				</Card>
			</section>

			<section className='space-y-4'>
				<div className='flex flex-col items-start gap-2 sm:flex-row sm:items-end sm:justify-between sm:gap-4'>
					<div>
						<h2 className='text-2xl font-semibold text-white'>
							Featured facilities
						</h2>
						<p className='text-sm text-slate-400'>
							{facilities.length} courts found
						</p>
					</div>
				</div>

				{facilitiesQuery.isPending ? (
					<LoadingOrError title='Loading facilities' />
				) : facilitiesQuery.isError ? (
					<LoadingOrError error={facilitiesQuery.error} />
				) : facilities.length === 0 ? (
					<Card>
						<CardContent className='pt-6 text-sm text-slate-400'>
							No facilities match the selected filters.
						</CardContent>
					</Card>
				) : (
					<div className='grid gap-4 md:grid-cols-2 xl:grid-cols-3'>
						{facilities.map(facility => (
							<Card key={facility.id}>
								<CardHeader>
									<div className='flex flex-wrap items-center gap-2'>
										<Badge>{humanizeLabel(facility.type)}</Badge>
										<Badge variant='muted'>{facility.status}</Badge>
									</div>
									<CardTitle>{facility.name}</CardTitle>
									<CardDescription>{facility.description}</CardDescription>
								</CardHeader>
								<CardContent className='space-y-3 text-sm text-slate-300'>
									<div className='flex flex-wrap items-center justify-between gap-2'>
										<span>Capacity</span>
										<span>{facility.capacity}</span>
									</div>
									<div className='flex flex-wrap items-center justify-between gap-2'>
										<span>Base price</span>
										<span>{formatCurrency(facility.basePricePerHour)}/h</span>
									</div>
									<div className='flex flex-wrap items-center justify-between gap-2'>
										<span>Hours</span>
										<span>
											{facility.workingHoursStart} - {facility.workingHoursEnd}
										</span>
									</div>
									<div className='flex flex-wrap gap-2 pt-2'>
										<Link to={`/facilities/${facility.id}`}>
											<Button size='sm' type='button' variant='outline'>
												Details
											</Button>
										</Link>
										<Link
											to={
												isSignedIn
													? `/bookings/new?facilityId=${facility.id}`
													: '/login'
											}
										>
											<Button size='sm' type='button'>
												Book
											</Button>
										</Link>
									</div>
								</CardContent>
							</Card>
						))}
					</div>
				)}
			</section>

			<section className='space-y-4'>
				<div>
					<h2 className='text-2xl font-semibold text-white'>
						Equipment snapshot
					</h2>
					<p className='text-sm text-slate-400'>Available rental inventory</p>
				</div>

				{equipmentQuery.isPending ? (
					<LoadingOrError title='Loading equipment' />
				) : equipmentQuery.isError ? (
					<LoadingOrError error={equipmentQuery.error} />
				) : (
					<div className='grid gap-4 md:grid-cols-2 xl:grid-cols-3'>
						{equipment.map(item => (
							<Card key={item.id}>
								<CardHeader>
									<div className='flex flex-wrap items-center gap-2'>
										<Badge variant='warning'>{humanizeLabel(item.type)}</Badge>
										<Badge variant='muted'>
											{item.quantityAvailable}/{item.quantityTotal} available
										</Badge>
									</div>
									<CardTitle>{item.name}</CardTitle>
									<CardDescription>
										{item.facilityName || 'Standalone inventory item'}
									</CardDescription>
								</CardHeader>
								<CardContent className='space-y-3 text-sm text-slate-300'>
									<div className='flex flex-wrap items-center justify-between gap-2'>
										<span>Category</span>
										<span>{item.category}</span>
									</div>
									<div className='flex flex-wrap items-center justify-between gap-2'>
										<span>Daily price</span>
										<span>{formatCurrency(item.pricePerDay)}</span>
									</div>
									<div className='flex flex-wrap items-center justify-between gap-2'>
										<span>Deposit</span>
										<span>{formatCurrency(item.depositRequired)}</span>
									</div>
								</CardContent>
							</Card>
						))}
					</div>
				)}
			</section>
		</div>
	)
}
