import {
	createContext,
	type PropsWithChildren,
	useContext,
	useEffect,
	useMemo,
	useState
} from 'react'
import {
	SESSION_EVENT_NAME,
	clearStoredSession,
	createStoredSession,
	getStoredSession,
	isTimestampExpired,
	setStoredSession
} from '@/auth/authStorage'
import {requestJson, type ApiError, refreshSessionWithToken} from '@/lib/http'
import type {AuthResponse, StoredSession} from '@/types/api'

interface AuthContextValue {
	isBootstrapping: boolean
	isSignedIn: boolean
	login: (credentials: {password: string; username: string}) => Promise<void>
	logout: () => Promise<void>
	session: StoredSession | null
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({children}: PropsWithChildren) {
	const [session, setSession] = useState<StoredSession | null>(() => getStoredSession())
	const [isBootstrapping, setIsBootstrapping] = useState(true)

	useEffect(() => {
		const syncSession = (event: Event) => {
			setSession((event as CustomEvent<StoredSession | null>).detail)
		}

		window.addEventListener(SESSION_EVENT_NAME, syncSession)
		return () => window.removeEventListener(SESSION_EVENT_NAME, syncSession)
	}, [])

	useEffect(() => {
		let isMounted = true

		async function bootstrapSession() {
			const currentSession = getStoredSession()
			if (!currentSession) {
				if (isMounted) {
					setIsBootstrapping(false)
				}
				return
			}

			if (
				isTimestampExpired(currentSession.accessTokenExpiresAt) &&
				currentSession.refreshToken &&
				!isTimestampExpired(currentSession.refreshTokenExpiresAt)
			) {
				try {
					await refreshSessionWithToken(
						currentSession.refreshToken,
						currentSession
					)
				} catch {
					clearStoredSession()
				}
			}

			if (isMounted) {
				setSession(getStoredSession())
				setIsBootstrapping(false)
			}
		}

		void bootstrapSession()
		return () => {
			isMounted = false
		}
	}, [])

	const value = useMemo<AuthContextValue>(
		() => ({
			isBootstrapping,
			isSignedIn: session !== null,
			async login(credentials: {password: string; username: string}) {
				const response = await requestJson<AuthResponse>('/api/auth/login', {
					auth: false,
					body: credentials,
					method: 'POST'
				})
				setStoredSession(createStoredSession(response))
			},
			async logout() {
				const currentSession = getStoredSession()
				try {
					await requestJson<{logout: boolean; message: string}>(
						'/api/auth/logout',
						{
							auth: false,
							body: currentSession?.refreshToken
								? {refresh_token: currentSession.refreshToken}
								: undefined,
							headers: currentSession?.accessToken
								? {
										Authorization: `${currentSession.tokenType} ${currentSession.accessToken}`
									}
								: undefined,
							method: 'POST',
							retryOnUnauthorized: false
						}
					)
				} finally {
					clearStoredSession()
				}
			},
			session
		}),
		[isBootstrapping, session]
	)

	return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
	const context = useContext(AuthContext)
	if (!context) {
		throw new Error('useAuth must be used within AuthProvider')
	}

	return context
}

export function isApiError(error: unknown): error is ApiError {
	return error instanceof Error && error.name === 'ApiError'
}
