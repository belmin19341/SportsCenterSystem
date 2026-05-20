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
		'block rounded-md px-3 py-2 text-sm font-medium text-slate-300 transition-colors hover:bg-slate-900 hover:text-white',
		isActive && 'bg-slate-900 text-white'
	)

export function AppHeader() {
	const {isSignedIn, logout, session} = useAuth()
	const {showFeedback} = useFeedback()
	const [isMenuOpen, setIsMenuOpen] = useState(false)
	const [isLoggingOut, setIsLoggingOut] = useState(false)

	function closeMenu() {
		setIsMenuOpen(false)
	}

	async function handleLogout() {
		setIsLoggingOut(true)
		try {
			await logout()
			closeMenu()
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
						onClick={closeMenu}
						to='/'
					>
						SportsCenterSystem
					</Link>
					<Button
						aria-controls='app-navigation'
						aria-expanded={isMenuOpen}
						aria-label={isMenuOpen ? 'Close menu' : 'Open menu'}
						className='h-10 w-10 px-0 lg:hidden'
						onClick={() => setIsMenuOpen(current => !current)}
						type='button'
						variant='outline'
					>
						<span
							aria-hidden={true}
							className='flex h-4 w-5 flex-col justify-between'
						>
							<span className='h-0.5 rounded-full bg-current' />
							<span className='h-0.5 rounded-full bg-current' />
							<span className='h-0.5 rounded-full bg-current' />
						</span>
					</Button>
				</div>

				<div
					className={cn(
						'border-t border-slate-800/80 pt-3 lg:flex lg:flex-1 lg:items-center lg:justify-between lg:border-0 lg:pt-0',
						isMenuOpen ? 'block' : 'hidden lg:flex'
					)}
					id='app-navigation'
				>
					<nav className='grid gap-1 lg:flex lg:flex-wrap lg:items-center lg:justify-center lg:gap-2'>
						<NavLink className={navLinkClassName} onClick={closeMenu} to='/'>
							Explore
						</NavLink>
						{isSignedIn ? (
							<>
								<NavLink
									className={navLinkClassName}
									onClick={closeMenu}
									to='/dashboard'
								>
									Dashboard
								</NavLink>
								<NavLink
									className={navLinkClassName}
									onClick={closeMenu}
									to='/bookings/new'
								>
									New booking
								</NavLink>
								{session && canManageFacilities(session.role) ? (
									<NavLink
										className={navLinkClassName}
										onClick={closeMenu}
										to='/owner/facilities'
									>
										Facilities
									</NavLink>
								) : null}
							</>
						) : (
							<NavLink
								className={navLinkClassName}
								onClick={closeMenu}
								to='/login'
							>
								Sign in
							</NavLink>
						)}
					</nav>

					<div className='mt-3 grid min-w-0 gap-2 lg:mt-0 lg:flex lg:items-center lg:justify-end lg:gap-3'>
						{session ? (
							<>
								<div className='min-w-0'>
									<div className='truncate text-sm font-medium text-white'>
										{session.username}
									</div>
									<div className='truncate text-xs text-slate-400'>
										{session.email}
									</div>
								</div>
								<div className='flex items-center gap-2'>
									<Badge>{session.role}</Badge>
									<Button
										className='ml-auto lg:ml-0'
										onClick={() => {
											void handleLogout()
										}}
										variant='outline'
									>
										{isLoggingOut ? 'Signing out...' : 'Sign out'}
									</Button>
								</div>
							</>
						) : (
							<Link
								className='w-full lg:w-auto'
								onClick={closeMenu}
								to='/login'
							>
								<Button className='w-full lg:w-auto'>Open app</Button>
							</Link>
						)}
					</div>
				</div>
			</div>
		</header>
	)
}
