import {
	addLocalDateTimeMinutes,
	combineLocalDateAndTime,
	getDatePart,
	normalizeTimeValue,
	parseLocalDateTime
} from '@/lib/localDateTime'

export function isValidDateRange(startTime: string, endTime: string) {
	if (!(startTime && endTime)) {
		return false
	}

	const start = new Date(startTime)
	const end = new Date(endTime)

	return (
		!(Number.isNaN(start.valueOf()) || Number.isNaN(end.valueOf())) &&
		start.valueOf() > Date.now() &&
		end.valueOf() > start.valueOf()
	)
}

export function getSuggestedEndTime(
	startTime: string,
	facilityEndTime?: string | null
) {
	if (!startTime) {
		return ''
	}

	const suggestedEndTime = addLocalDateTimeMinutes(startTime, 60)
	const normalizedFacilityEndTime = normalizeTimeValue(facilityEndTime)

	if (!normalizedFacilityEndTime) {
		return suggestedEndTime
	}

	const closingDateTime = combineLocalDateAndTime(
		getDatePart(startTime),
		normalizedFacilityEndTime
	)
	const parsedStartTime = parseLocalDateTime(startTime)
	const parsedSuggestedEndTime = parseLocalDateTime(suggestedEndTime)
	const parsedClosingDateTime = parseLocalDateTime(closingDateTime)

	if (!(parsedStartTime && parsedSuggestedEndTime && parsedClosingDateTime)) {
		return suggestedEndTime
	}

	if (parsedClosingDateTime.valueOf() <= parsedStartTime.valueOf()) {
		return ''
	}

	return parsedSuggestedEndTime.valueOf() > parsedClosingDateTime.valueOf()
		? closingDateTime
		: suggestedEndTime
}
