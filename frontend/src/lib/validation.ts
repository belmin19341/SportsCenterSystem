import {formatTimeRange} from '@/lib/localDateTime'
import type {FacilityRequest, FacilityResponse, PriceQuote} from '@/types/api'

const MAX_USERNAME_LENGTH = 100
const MIN_PASSWORD_LENGTH = 6
const MIN_USERNAME_LENGTH = 3

export interface BookingValidationInput {
	conflictCount: number
	endTime: string
	facility: FacilityResponse | null | undefined
	quote: PriceQuote | null | undefined
	startTime: string
}

export interface ReviewValidationInput {
	comment: string
	rating: number
}

function getLocalDate(value: string) {
	if (!value) {
		return null
	}

	const date = new Date(value)
	return Number.isNaN(date.valueOf()) ? null : date
}

function getMinutes(value: string) {
	const [hours = '', minutes = ''] = value.slice(0, 5).split(':')
	const parsedHours = Number(hours)
	const parsedMinutes = Number(minutes)

	if (Number.isNaN(parsedHours) || Number.isNaN(parsedMinutes)) {
		return null
	}

	return parsedHours * 60 + parsedMinutes
}

function getDateTimeMinutes(value: string) {
	const time = value.split('T')[1]
	return time ? getMinutes(time) : null
}

export function validateLoginForm(input: {password: string; username: string}) {
	const errors: string[] = []
	const username = input.username.trim()

	if (
		username.length < MIN_USERNAME_LENGTH ||
		username.length > MAX_USERNAME_LENGTH
	) {
		errors.push('Username must be between 3 and 100 characters.')
	}

	if (input.password.length < MIN_PASSWORD_LENGTH) {
		errors.push('Password must be at least 6 characters.')
	}

	return errors
}

export function validateRegisterForm(form: {
	username?: string
	email?: string
	password?: string
}) {
	const errors: string[] = []

	if (!form.username || form.username.trim().length < MIN_USERNAME_LENGTH) {
		errors.push(`Username must be at least ${MIN_USERNAME_LENGTH} characters long.`)
	} else if (form.username.length > 50) {
		errors.push('Username cannot exceed 50 characters.')
	}

	const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
	if (!form.email || !emailRegex.test(form.email)) {
		errors.push('Please enter a valid email address.')
	}

	if (!form.password || form.password.length < 8) {
		errors.push('Password must be at least 8 characters long.')
	}

	return errors
}

export function validateBookingForm(input: BookingValidationInput) {
	const errors: string[] = []
	const start = getLocalDate(input.startTime)
	const end = getLocalDate(input.endTime)

	if (!input.facility) {
		errors.push('Choose a facility before continuing.')
	} else if (input.facility.status !== 'ACTIVE') {
		errors.push('Only active facilities can be booked.')
	}

	if (!start) {
		errors.push('Choose a valid start time.')
	}

	if (!end) {
		errors.push('Choose a valid end time.')
	}

	if (start && start.valueOf() <= Date.now()) {
		errors.push('Start time must be in the future.')
	}

	if (start && end && end.valueOf() <= start.valueOf()) {
		errors.push('End time must be after start time.')
	}

	if (input.facility && start && end) {
		const open = getMinutes(input.facility.workingHoursStart)
		const close = getMinutes(input.facility.workingHoursEnd)
		const requestedStart = getDateTimeMinutes(input.startTime)
		const requestedEnd = getDateTimeMinutes(input.endTime)

		if (
			open !== null &&
			close !== null &&
			requestedStart !== null &&
			requestedEnd !== null &&
			(requestedStart < open || requestedEnd > close)
		) {
			errors.push(
				`Selected time must fit facility hours (${formatTimeRange(
					input.facility.workingHoursStart,
					input.facility.workingHoursEnd
				)}).`
			)
		}
	}

	if (input.conflictCount > 0) {
		errors.push('Selected time overlaps with an existing booking.')
	}

	if (!input.quote || Number(input.quote.totalPrice) <= 0) {
		errors.push('A valid price quote is required before booking.')
	}

	return errors
}

/**
 * Validate only time-related constraints (before fetching price quote)
 * Returns errors that prevent the quote API call
 */
export function validateBookingTimeOnly(
	input: Omit<BookingValidationInput, 'quote'> & {facility: FacilityResponse | null | undefined}
) {
	const errors: string[] = []
	const start = getLocalDate(input.startTime)
	const end = getLocalDate(input.endTime)

	if (!input.facility) {
		return errors // Silent - not needed for quote validation
	}

	if (!start || !end) {
		return errors // Silent - inputs not ready yet
	}

	if (start.valueOf() <= Date.now()) {
		errors.push('Start time must be in the future.')
	}

	if (end.valueOf() <= start.valueOf()) {
		errors.push('End time must be after start time.')
	}

	const open = getMinutes(input.facility.workingHoursStart)
	const close = getMinutes(input.facility.workingHoursEnd)
	const requestedStart = getDateTimeMinutes(input.startTime)
	const requestedEnd = getDateTimeMinutes(input.endTime)

	if (
		open !== null &&
		close !== null &&
		requestedStart !== null &&
		requestedEnd !== null &&
		(requestedStart < open || requestedEnd > close)
	) {
		errors.push(
			`Selected time must fit facility hours (${formatTimeRange(
				input.facility.workingHoursStart,
				input.facility.workingHoursEnd
			)}).`
		)
	}

	return errors
}

export function validateFacilityForm(input: FacilityRequest) {
	const errors: string[] = []

	if (!input.name.trim() || input.name.length > 200) {
		errors.push('Facility name is required and must be at most 200 characters.')
	}

	if (input.capacity < 1) {
		errors.push('Capacity must be at least 1.')
	}

	if (Number(input.basePricePerHour) <= 0) {
		errors.push('Base price must be greater than 0.')
	}

	const open = getMinutes(input.workingHoursStart)
	const close = getMinutes(input.workingHoursEnd)
	if (open === null || close === null || close <= open) {
		errors.push('Working hours end must be after start.')
	}

	if (input.imageUrl && input.imageUrl.length > 500) {
		errors.push('Image URL must be at most 500 characters.')
	}

	return errors
}

export function validateReviewForm(input: ReviewValidationInput) {
	const errors: string[] = []

	if (input.rating < 1 || input.rating > 5) {
		errors.push('Rating must be between 1 and 5.')
	}

	if (input.comment.length > 1000) {
		errors.push('Comment is too long.')
	}

	return errors
}

// ── Per-field validators ──────────────────────────────────────────────────────

export function validateUsernameField(value: string): string | null {
	const trimmed = value.trim()
	if (!trimmed) return 'Username is required.'
	if (trimmed.length < 3) return 'Username must be at least 3 characters.'
	if (trimmed.length > 50) return 'Username cannot exceed 50 characters.'
	return null
}

export function validateEmailField(value: string): string | null {
	if (!value.trim()) return 'Email is required.'
	if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim()))
		return 'Enter a valid email address.'
	return null
}

export function validatePasswordField(
	value: string,
	minLength = 8
): string | null {
	if (!value) return 'Password is required.'
	if (value.length < minLength)
		return `Password must be at least ${minLength} characters.`
	return null
}

export function validateConfirmPasswordField(
	password: string,
	confirm: string
): string | null {
	if (!confirm) return 'Please confirm your password.'
	if (password !== confirm) return 'Passwords do not match.'
	return null
}

export function validateFacilityNameField(value: string): string | null {
	const trimmed = value.trim()
	if (!trimmed) return 'Facility name is required.'
	if (trimmed.length > 200) return 'Name must be at most 200 characters.'
	return null
}

export function validateCapacityField(value: number): string | null {
	if (!value || value < 1) return 'Capacity must be at least 1.'
	return null
}

export function validateBasePriceField(value: number): string | null {
	if (!value || Number(value) <= 0) return 'Base price must be greater than 0.'
	return null
}

export function validateWorkingHoursField(
	start: string,
	end: string
): string | null {
	if (!start) return 'Opening time is required.'
	if (!end) return 'Closing time is required.'
	const openMin = getMinutes(start)
	const closeMin = getMinutes(end)
	if (openMin === null || closeMin === null) return 'Enter valid times.'
	if (closeMin <= openMin) return 'Closing time must be after opening time.'
	return null
}
