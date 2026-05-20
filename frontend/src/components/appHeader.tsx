import {useState} from 'react'
import {Link, NavLink} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {canManageFacilities} from '@/auth/roles'
import {useFeedback} from '@/components/feedback'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'
import {cn} from '@/lib/utils'

const navLinkClassName = ({isActive}: {isActive: boolean}) =>
	cn(
		'shrink-0 rounded-md px-3 py-2 text-sm font-medium text-slate-300 transition-colors hover:bg-slate-900 hover:text-white',
		isActive && 'bg-slate-900 text-white'
	)

export function AppHeader() {
	const {isSignedIn, logout, session} = useAuth()
	const {showFeedback} = useFeedback()
	const [isLoggingOut, setIsLoggingOut] = useState(false)

	async function handleLogout() {
		setIsLoggingOut(true)
		try {
			await logout()
			showFeedback({
				description: 'Your local session was cleared.',
				title: 'Signed out',
				variant: 'success'
			})
		} finally {
			setIsLoggingOut(false)
		}
	}

	return (
		<header className='sticky top-0 z-10 border-b border-slate-800/80 bg-slate-950/80 backdrop-blur'>
			<div className='mx-auto flex w-full max-w-7xl flex-col gap-3 px-3 py-3 sm:px-6 lg:flex-row lg:items-center lg:justify-between lg:px-8'>
				<div className='flex min-w-0 items-center justify-between gap-3 lg:justify-start'>
					<Link
						className='truncate text-base font-semibold text-white sm:text-lg'
						to='/'
					>
						SportsCenterSystem
					</Link>
					<Badge className='hidden sm:inline-flex' variant='muted'>
						Gateway-first frontend
					</Badge>
				</div>

				<nav className='-mx-1 flex max-w-full gap-1 overflow-x-auto px-1 pb-1 sm:mx-0 sm:flex-wrap sm:gap-2 sm:px-0 sm:pb-0 lg:justify-center'>
					<NavLink className={navLinkClassName} to='/'>
						Explore
					</NavLink>
					{isSignedIn ? (
						<>
							<NavLink className={navLinkClassName} to='/dashboard'>
								Dashboard
							</NavLink>
							<NavLink className={navLinkClassName} to='/bookings/new'>
								New booking
							</NavLink>
							{session && canManageFacilities(session.role) ? (
								<NavLink className={navLinkClassName} to='/owner/facilities'>
									Facilities
								</NavLink>
							) : null}
						</>
					) : (
						<NavLink className={navLinkClassName} to='/login'>
							Sign in
						</NavLink>
					)}
				</nav>

				<div className='flex min-w-0 flex-wrap items-center justify-between gap-2 sm:justify-end lg:flex-nowrap'>
					{session ? (
						<>
							<div className='min-w-0 text-left sm:text-right'>
								<div className='truncate text-sm font-medium text-white'>
									{session.username}
								</div>
								<div className='hidden truncate text-xs text-slate-400 sm:block'>
									{session.email}
								</div>
							</div>
							<Badge>{session.role}</Badge>
							<Button
								onClick={() => {
									void handleLogout()
								}}
								variant='outline'
							>
								{isLoggingOut ? 'Signing out...' : 'Sign out'}
							</Button>
						</>
					) : (
						<Link className='w-full sm:w-auto' to='/login'>
							<Button className='w-full sm:w-auto'>Open app</Button>
						</Link>
					)}
				</div>
			</div>
		</header>
	)
}
