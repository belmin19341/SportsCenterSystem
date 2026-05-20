export function formatCurrency(value: number | string | null | undefined) {
	if (value === null || value === undefined) {
		return '—'
	}

	const numericValue = typeof value === 'string' ? Number(value) : value
	if (Number.isNaN(numericValue)) {
		return '—'
	}

	return new Intl.NumberFormat('bs-BA', {
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

	return new Intl.DateTimeFormat('bs-BA', {
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

	return new Intl.DateTimeFormat('bs-BA', {
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
			return ['Your session is no longer valid. Sign in again to continue.']
		}

		if ('status' in error && error.status === 403) {
			return ['You do not have permission to perform this action.']
		}

		if ('status' in error && error.status === 429) {
			return ['Too many attempts. Wait a moment and try again.']
		}

		return [error.message]
	}

	return ['Something went wrong.']
}
