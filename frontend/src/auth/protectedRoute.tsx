import type {PropsWithChildren} from 'react'
import {Navigate, useLocation} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {canAccessRole} from '@/auth/roles'
import {LoadingOrError} from '@/components/loadingOrError'
import {Alert} from '@/components/ui/alert'
import type {Role} from '@/types/api'

export function ProtectedRoute({
	allowedRoles,
	children
}: PropsWithChildren<{allowedRoles?: Role[]}>) {
	const location = useLocation()
	const {isBootstrapping, session} = useAuth()

	if (isBootstrapping) {
		return <LoadingOrError title='Restoring session' />
	}

	if (!session) {
		return <Navigate replace={true} state={{from: location}} to='/login' />
	}

	if (!canAccessRole(session.role, allowedRoles)) {
		return (
			<Alert variant='destructive'>
				<div className='font-semibold'>Access restricted</div>
				<div className='mt-1 text-sm'>
					This area is available only to users with the required role.
				</div>
			</Alert>
		)
	}

	return <>{children}</>
}
