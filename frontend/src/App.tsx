import {Suspense} from 'react'
import {ErrorBoundary, type FallbackProps} from 'react-error-boundary'
import {Route, Routes} from 'react-router'
import {AuthProvider} from '@/auth/authContext'
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

export function App() {
	return (
		<ErrorBoundary fallbackRender={renderError}>
			<FeedbackProvider>
				<AuthProvider>
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
