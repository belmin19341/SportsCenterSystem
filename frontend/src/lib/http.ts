import {
	clearStoredSession,
	createStoredSession,
	getStoredSession,
	isTimestampExpired,
	setStoredSession
} from '@/auth/authStorage'
import type {ApiErrorPayload, AuthResponse, StoredSession} from '@/types/api'

const DEFAULT_API_BASE_URL = 'http://localhost:8080'
const apiBaseUrl = (
	import.meta.env.VITE_API_BASE_URL || DEFAULT_API_BASE_URL
).replace(/\/$/u, '')

export class ApiError extends Error {
	details?: string[]
	label?: string
	status: number

	constructor(
		message: string,
		status: number,
		options?: {
			details?: string[]
			label?: string
		}
	) {
		super(message)
		this.name = 'ApiError'
		this.details = options?.details
		this.label = options?.label
		this.status = status
	}
}

type JsonValue =
	| boolean
	| null
	| number
	| string
	| JsonValue[]
	| {[key: string]: JsonValue}

interface RequestJsonOptions {
	auth?: boolean
	body?: BodyInit | JsonValue
	headers?: HeadersInit
	method?: 'DELETE' | 'GET' | 'PATCH' | 'POST' | 'PUT'
	retryOnUnauthorized?: boolean
}

function isBodyInit(body: unknown): body is BodyInit {
	return (
		body instanceof ArrayBuffer ||
		body instanceof Blob ||
		body instanceof FormData ||
		body instanceof URLSearchParams ||
		typeof body === 'string'
	)
}

function createHeaders(headers?: HeadersInit) {
	return new Headers(headers)
}

function getBody(body: RequestJsonOptions['body'], headers: Headers) {
	if (body === undefined) {
		return
	}

	if (isBodyInit(body)) {
		return body
	}

	if (!headers.has('Content-Type')) {
		headers.set('Content-Type', 'application/json')
	}

	return JSON.stringify(body)
}

export function buildApiUrl(path: string) {
	if (path.startsWith('http://') || path.startsWith('https://')) {
		return path
	}

	return `${apiBaseUrl}${path.startsWith('/') ? path : `/${path}`}`
}

async function parseResponse<T>(response: Response) {
	if (response.status === 204) {
		return undefined as T
	}

	const contentType = response.headers.get('Content-Type') || ''
	const isJson = contentType.includes('application/json')
	const payload = isJson
		? ((await response.json()) as ApiErrorPayload | T)
		: await response.text()

	if (!response.ok) {
		if (typeof payload === 'string') {
			throw new ApiError(payload || response.statusText, response.status)
		}

		const errorPayload = payload as ApiErrorPayload
		throw new ApiError(
			errorPayload.message || errorPayload.error || response.statusText,
			errorPayload.status ?? response.status,
			{
				details: errorPayload.details,
				label: errorPayload.error
			}
		)
	}

	return payload as T
}

async function rawRequest<T>(
	path: string,
	options: Omit<RequestJsonOptions, 'auth' | 'retryOnUnauthorized'>
) {
	const headers = createHeaders(options.headers)
	const response = await fetch(buildApiUrl(path), {
		body: getBody(options.body, headers),
		headers,
		method: options.method
	})

	return parseResponse<T>(response)
}

export async function refreshSessionWithToken(
	refreshToken: string,
	previousSession?: StoredSession | null
) {
	const response = await rawRequest<AuthResponse>('/api/auth/refresh', {
		body: {refresh_token: refreshToken},
		method: 'POST'
	})

	const nextSession = createStoredSession(response, previousSession)
	setStoredSession(nextSession)
	return nextSession
}

async function getAuthorizedSession() {
	const session = getStoredSession()
	if (!session) {
		return null
	}

	if (!isTimestampExpired(session.accessTokenExpiresAt)) {
		return session
	}

	if (
		!session.refreshToken ||
		isTimestampExpired(session.refreshTokenExpiresAt)
	) {
		clearStoredSession()
		return null
	}

	try {
		return await refreshSessionWithToken(session.refreshToken, session)
	} catch {
		clearStoredSession()
		return null
	}
}

export async function requestJson<T>(
	path: string,
	options: RequestJsonOptions = {}
) {
	const headers = createHeaders(options.headers)
	let session = options.auth === false ? null : await getAuthorizedSession()

	if (session?.accessToken) {
		headers.set('Authorization', `${session.tokenType} ${session.accessToken}`)
	}

	let response = await fetch(buildApiUrl(path), {
		body: getBody(options.body, headers),
		headers,
		method: options.method
	})

	if (
		options.auth !== false &&
		options.retryOnUnauthorized !== false &&
		response.status === 401 &&
		session?.refreshToken &&
		!isTimestampExpired(session.refreshTokenExpiresAt)
	) {
		try {
			session = await refreshSessionWithToken(session.refreshToken, session)
			headers.set(
				'Authorization',
				`${session.tokenType} ${session.accessToken}`
			)
			response = await fetch(buildApiUrl(path), {
				body: getBody(options.body, headers),
				headers,
				method: options.method
			})
		} catch {
			clearStoredSession()
		}
	}

	return parseResponse<T>(response)
}
