import type {AuthResponse, StoredSession} from '@/types/api'

const SESSION_STORAGE_KEY = 'sportscenter-session'
export const SESSION_EVENT_NAME = 'sportscenter:session-updated'

function emitSessionUpdate(session: StoredSession | null) {
	if (typeof window === 'undefined') {
		return
	}

	window.dispatchEvent(
		new CustomEvent<StoredSession | null>(SESSION_EVENT_NAME, {
			detail: session
		})
	)
}

export function getStoredSession() {
	if (typeof window === 'undefined') {
		return null
	}

	const rawValue = window.localStorage.getItem(SESSION_STORAGE_KEY)
	if (!rawValue) {
		return null
	}

	try {
		return JSON.parse(rawValue) as StoredSession
	} catch {
		window.localStorage.removeItem(SESSION_STORAGE_KEY)
		return null
	}
}

export function isTimestampExpired(timestamp: number | null) {
	if (!timestamp) {
		return false
	}

	return Date.now() >= timestamp - 30_000
}

export function createStoredSession(
	response: AuthResponse,
	previousSession?: StoredSession | null
) {
	const now = Date.now()
	const refreshToken =
		response.refresh_token || previousSession?.refreshToken || ''
	const refreshTokenExpiresAt = response.refresh_expires_in
		? now + response.refresh_expires_in * 1000
		: (previousSession?.refreshTokenExpiresAt ?? null)

	return {
		accessToken: response.access_token,
		accessTokenExpiresAt: now + response.expires_in * 1000,
		email: response.email,
		refreshToken,
		refreshTokenExpiresAt,
		role: response.role,
		tokenType: response.token_type,
		userId: response.userId,
		username: response.username
	} satisfies StoredSession
}

export function setStoredSession(session: StoredSession) {
	if (typeof window === 'undefined') {
		return
	}

	window.localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session))
	emitSessionUpdate(session)
}

export function clearStoredSession() {
	if (typeof window === 'undefined') {
		return
	}

	window.localStorage.removeItem(SESSION_STORAGE_KEY)
	emitSessionUpdate(null)
}
