import {useQuery} from '@tanstack/react-query'
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

function humanizeLabel(value: string) {
	return value.replaceAll('_', ' ').toLowerCase()
}

export function HomePage() {
	const {isSignedIn} = useAuth()
	const facilitiesQuery = useQuery({
		queryFn: listFacilities,
		queryKey: ['facilities']
	})
	const equipmentQuery = useQuery({
		queryFn: listEquipment,
		queryKey: ['equipment']
	})

	const facilities =
		facilitiesQuery.data?.filter(facility => facility.status === 'ACTIVE').slice(0, 6) ||
		[]
	const equipment = equipmentQuery.data?.slice(0, 6) || []

	return (
		<div className='space-y-10'>
			<section className='grid gap-6 lg:grid-cols-[1.4fr,0.6fr]'>
				<Card className='overflow-hidden border-sky-500/20 bg-gradient-to-br from-sky-500/10 via-slate-950 to-slate-950'>
					<CardHeader className='space-y-4'>
						<Badge>Public browsing + USER MVP</Badge>
						<CardTitle className='max-w-2xl text-4xl leading-tight sm:text-5xl'>
							Find courts, understand pricing, and move into authenticated
							booking flows through the API Gateway.
						</CardTitle>
						<CardDescription className='max-w-2xl text-base text-slate-300'>
							This frontend is now anchored in the repository and talks to the
							existing Spring microservices through the gateway instead of
							coding around them.
						</CardDescription>
					</CardHeader>
					<CardContent className='flex flex-wrap gap-3'>
						<Link to={isSignedIn ? '/bookings/new' : '/login'}>
							<Button size='lg'>
								{isSignedIn ? 'Create a booking' : 'Sign in to continue'}
							</Button>
						</Link>
						<Link to={isSignedIn ? '/dashboard' : '/login'}>
							<Button size='lg' variant='outline'>
								{isSignedIn ? 'Open dashboard' : 'View the app shell'}
							</Button>
						</Link>
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>What is already wired</CardTitle>
						<CardDescription>
							This first implementation slice is aligned to the real backend.
						</CardDescription>
					</CardHeader>
					<CardContent>
						<ul className='space-y-3 text-sm text-slate-300'>
							<li>Gateway-based auth and browser entrypoint</li>
							<li>Public facility and equipment discovery</li>
							<li>User session handling with access + refresh tokens</li>
							<li>Booking creation through the orchestrated booking endpoint</li>
							<li>User dashboard with loyalty, bookings, rentals, and alerts</li>
						</ul>
					</CardContent>
				</Card>
			</section>

			<section className='space-y-4'>
				<div className='flex items-end justify-between gap-4'>
					<div>
						<h2 className='text-2xl font-semibold text-white'>
							Featured facilities
						</h2>
						<p className='text-sm text-slate-400'>
							Live data from Resource Service through the gateway.
						</p>
					</div>
				</div>

				{facilitiesQuery.isPending ? (
					<LoadingOrError title='Loading facilities' />
				) : facilitiesQuery.isError ? (
					<LoadingOrError error={facilitiesQuery.error} />
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
									<div className='flex items-center justify-between'>
										<span>Capacity</span>
										<span>{facility.capacity}</span>
									</div>
									<div className='flex items-center justify-between'>
										<span>Base price</span>
										<span>{formatCurrency(facility.basePricePerHour)}/h</span>
									</div>
									<div className='flex items-center justify-between'>
										<span>Hours</span>
										<span>
											{facility.workingHoursStart} - {facility.workingHoursEnd}
										</span>
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
					<p className='text-sm text-slate-400'>
						A lightweight public slice from Resource Service inventory.
					</p>
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
									<div className='flex items-center justify-between'>
										<span>Category</span>
										<span>{item.category}</span>
									</div>
									<div className='flex items-center justify-between'>
										<span>Daily price</span>
										<span>{formatCurrency(item.pricePerDay)}</span>
									</div>
									<div className='flex items-center justify-between'>
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

