import {
	addLocalDateTimeMinutes,
	parseLocalDateTime,
	roundDateUp,
	toLocalDateTimeValue
} from '@/lib/localDateTime'
import type {BookingDraft} from './bookingDraft'
import {getSuggestedEndTime} from './timeRange'

export function createMinimumBookingDateTime() {
	return toLocalDateTimeValue(
		roundDateUp(new Date(Date.now() + 5 * 60_000), 15)
	)
}

export function getMinimumEndDateTime(
	startTime: string,
	minimumBookingDateTime: string
) {
	return addLocalDateTimeMinutes(startTime, 1) || minimumBookingDateTime
}

export function applyStartTimeToDraft(
	currentForm: BookingDraft,
	nextStartTime: string,
	facilityEndTime?: string | null
) {
	if (!nextStartTime) {
		return {...currentForm, endTime: '', startTime: ''}
	}

	const parsedCurrentEndTime = parseLocalDateTime(currentForm.endTime)
	const parsedNextStartTime = parseLocalDateTime(nextStartTime)
	const shouldSuggestEndTime =
		!(parsedCurrentEndTime && parsedNextStartTime) ||
		parsedCurrentEndTime.valueOf() <= parsedNextStartTime.valueOf()

	return {
		...currentForm,
		endTime: shouldSuggestEndTime
			? getSuggestedEndTime(nextStartTime, facilityEndTime)
			: currentForm.endTime,
		startTime: nextStartTime
	}
}
