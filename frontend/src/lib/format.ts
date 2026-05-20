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
	if (error instanceof Error) {
		return error.message
	}

	return 'Something went wrong.'
}

