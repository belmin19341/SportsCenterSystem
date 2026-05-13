import type {PropsWithChildren} from 'react'
import {Navigate, useLocation} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {LoadingOrError} from '@/components/loadingOrError'

export function ProtectedRoute({children}: PropsWithChildren) {
	const location = useLocation()
	const {isBootstrapping, session} = useAuth()

	if (isBootstrapping) {
		return <LoadingOrError title='Restoring session' />
	}

	if (!session) {
		return <Navigate replace={true} state={{from: location}} to='/login' />
	}

	return <>{children}</>
}

