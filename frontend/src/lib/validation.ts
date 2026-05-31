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
