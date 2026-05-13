import {Suspense} from 'react'
import {ErrorBoundary, type FallbackProps} from 'react-error-boundary'
import {Route, Routes} from 'react-router'
import {AuthProvider} from '@/auth/authContext'
import {ProtectedRoute} from '@/auth/protectedRoute'
import {AppShell} from '@/components/appShell'
import {LoadingOrError} from '@/components/loadingOrError'
import {BookingPage} from '@/pages/bookingPage'
import {DashboardPage} from '@/pages/dashboardPage'
import {HomePage} from '@/pages/homePage'
import {LoginPage} from '@/pages/loginPage'
import {NotFoundPage} from '@/pages/notFoundPage'

function renderError({error}: FallbackProps) {
	return <LoadingOrError error={error} />
}

export function App() {
	return (
		<ErrorBoundary fallbackRender={renderError}>
			<AuthProvider>
				<AppShell>
					<Suspense fallback={<LoadingOrError title='Loading route' />}>
						<Routes>
							<Route element={<HomePage />} index={true} />
							<Route element={<LoginPage />} path='/login' />
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
							<Route element={<NotFoundPage />} path='*' />
						</Routes>
					</Suspense>
				</AppShell>
			</AuthProvider>
		</ErrorBoundary>
	)
}

