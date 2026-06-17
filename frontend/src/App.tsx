import {useQueryClient} from '@tanstack/react-query'
import {Suspense, useEffect, useRef} from 'react'
import {ErrorBoundary, type FallbackProps} from 'react-error-boundary'
import {Route, Routes} from 'react-router'
import {useAuth, AuthProvider} from '@/auth/authContext'
import {ProtectedRoute} from '@/auth/protectedRoute'
import {AppShell} from '@/components/appShell'
import {FeedbackProvider} from '@/components/feedback'
import {LoadingOrError} from '@/components/loadingOrError'
import {BookingPage} from '@/pages/bookingPage'
import {DashboardPage} from '@/pages/dashboardPage'
import {FacilityPage} from '@/pages/facilityPage'
import {HomePage} from '@/pages/homePage'
import {LoginPage} from '@/pages/loginPage'
import {NotFoundPage} from '@/pages/notFoundPage'
import {RegisterPage} from '@/pages/registerPage'
import {OwnerFacilitiesPage} from '@/pages/ownerFacilitiesPage'

function renderError({error}: FallbackProps) {
	return <LoadingOrError error={error} />
}

function CacheCleaner() {
	const {session} = useAuth()
	const queryClient = useQueryClient()
	const prevUserId = useRef<number | undefined>(undefined)

	useEffect(() => {
		const id = session?.userId
		if (prevUserId.current !== undefined && prevUserId.current !== id) {
			queryClient.clear()
		}
		prevUserId.current = id
	}, [session?.userId, queryClient])

	return null
}

export function App() {
	// Issue #25: Validation to ensure VITE_API_BASE_URL is defined at runtime.
	// This prevents the app from silently failing or using a wrong hardcoded fallback.
	if (!import.meta.env.VITE_API_BASE_URL) {
		throw new Error(
			'VITE_API_BASE_URL is not defined. Please check your .env file and ensure it points to the API Gateway (typically http://localhost:8080).'
		)
	}

	return (
		<ErrorBoundary fallbackRender={renderError}>
			<FeedbackProvider>
				<AuthProvider>
					<CacheCleaner />
					<AppShell>
						<Suspense fallback={<LoadingOrError title='Loading route' />}>
							<Routes>
								<Route element={<HomePage />} index={true} />
								<Route
									element={<FacilityPage />}
									path='/facilities/:facilityId'
								/>
								<Route element={<LoginPage />} path='/login' />
								<Route element={<RegisterPage />} path='/register' />
								<Route
									element={
										<ProtectedRoute>
											<DashboardPage />
										</ProtectedRoute>
									}
									path='/dashboard'
								/>
								<Route
									element={
										<ProtectedRoute>
											<BookingPage />
										</ProtectedRoute>
									}
									path='/bookings/new'
								/>
								<Route
									element={
										<ProtectedRoute allowedRoles={['OWNER', 'ADMIN']}>
											<OwnerFacilitiesPage />
										</ProtectedRoute>
									}
									path='/owner/facilities'
								/>
								<Route element={<NotFoundPage />} path='*' />
							</Routes>
						</Suspense>
					</AppShell>
				</AuthProvider>
			</FeedbackProvider>
		</ErrorBoundary>
	)
}
