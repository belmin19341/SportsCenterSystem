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
		'rounded-md px-3 py-2 text-sm font-medium text-slate-300 transition-colors hover:bg-slate-900 hover:text-white',
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
			<div className='mx-auto flex w-full max-w-7xl items-center justify-between gap-4 px-4 py-4 sm:px-6 lg:px-8'>
				<div className='flex items-center gap-3'>
					<Link className='text-lg font-semibold text-white' to='/'>
						SportsCenterSystem
					</Link>
					<Badge variant='muted'>Gateway-first frontend</Badge>
				</div>

				<nav className='flex items-center gap-2'>
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

				<div className='flex items-center gap-3'>
					{session ? (
						<>
							<div className='hidden text-right sm:block'>
								<div className='text-sm font-medium text-white'>
									{session.username}
								</div>
								<div className='text-xs text-slate-400'>{session.email}</div>
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
						<Link to='/login'>
							<Button>Open app</Button>
						</Link>
					)}
				</div>
			</div>
		</header>
	)
}
