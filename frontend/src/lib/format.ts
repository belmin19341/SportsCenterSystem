export function formatCurrency(value: number | string | null | undefined) {
	if (value === null || value === undefined) {
		return '—'
	}

	const numericValue = typeof value === 'string' ? Number(value) : value
	if (Number.isNaN(numericValue)) {
		return '—'
	}

	return new Intl.NumberFormat('en-US', {
		currency: 'BAM',
		maximumFractionDigits: 2,
		style: 'currency'
	}).format(numericValue)
}

export function formatDateTime(value: string | null | undefined) {
	if (!value) {
		return '—'
	}

	const date = new Date(value)
	if (Number.isNaN(date.valueOf())) {
		return value
	}

	return new Intl.DateTimeFormat('en-US', {
		dateStyle: 'medium',
		timeStyle: 'short'
	}).format(date)
}

export function formatDate(value: string | null | undefined) {
	if (!value) {
		return '—'
	}

	const date = new Date(value)
	if (Number.isNaN(date.valueOf())) {
		return value
	}

	return new Intl.DateTimeFormat('en-US', {
		dateStyle: 'medium'
	}).format(date)
}

export function getErrorMessage(error: unknown) {
	return getErrorMessages(error)[0] || 'Something went wrong.'
}

export function getErrorMessages(error: unknown) {
	if (error instanceof Error) {
		const details = 'details' in error ? error.details : undefined
		if (Array.isArray(details) && details.length > 0) {
			return [error.message, ...details]
		}

		if ('status' in error && error.status === 401) {
			// Distinguish between invalid credentials and expired session
			const label = 'label' in error ? error.label : undefined
			const message = error.message
			
			if (label === 'BadCredentials' || message?.includes('username') || message?.includes('password')) {
				return ['Invalid username or password. Please check your credentials.']
			}
			
			return ['Your session is no longer valid. Sign in again to continue.']
		}

		if ('status' in error && error.status === 403) {
			return ['You do not have permission to perform this action.']
		}

		if ('status' in error && error.status === 429) {
			return ['Too many attempts. Wait a moment and try again.']
		}

		if ('status' in error && error.status === 404) {
			const message = error.message
			if (message?.includes('Loyalty record not found')) {
				return ['You do not currently belong to any loyalty category.']
			}
			return ['Resource not found.']
		}

		if ('status' in error && error.status === 503) {
			return ['A required service is currently unavailable. Please try again in a few moments.']
		}

		return [error.message]
	}

	return ['Something went wrong.']
}
