import {toLocalDateValue, toLocalTimeValue} from '@/lib/localDateTime'
import {
	hourOptions,
	minuteOptions,
	type TimeBounds
} from './dateTimePicker.shared'

export function createQuickDateOptions() {
	const today = new Date()
	const tomorrow = new Date(today)
	tomorrow.setDate(today.getDate() + 1)
	const nextWeek = new Date(today)
	nextWeek.setDate(today.getDate() + 7)

	return [
		{label: 'Today', value: toLocalDateValue(today)},
		{label: 'Tomorrow', value: toLocalDateValue(tomorrow)},
		{label: 'Next week', value: toLocalDateValue(nextWeek)}
	]
}

export function createQuickTimeSuggestions(
	mode: 'datetime' | 'time',
	quickStepMinutes: number,
	selectedDateValue: string,
	getTimeBounds: (dateValue?: string) => TimeBounds | null
) {
	const bounds =
		mode === 'datetime' ? getTimeBounds(selectedDateValue) : getTimeBounds()
	if (!bounds) {
		return []
	}

	const firstSuggestion =
		Math.ceil(bounds.lowerBound / quickStepMinutes) * quickStepMinutes
	const suggestions: string[] = []

	for (
		let minutes = firstSuggestion;
		minutes <= bounds.upperBound;
		minutes += quickStepMinutes
	) {
		suggestions.push(toLocalTimeValue(Math.floor(minutes / 60), minutes % 60))
	}

	if (suggestions.length === 0) {
		suggestions.push(
			toLocalTimeValue(
				Math.floor(bounds.lowerBound / 60),
				bounds.lowerBound % 60
			)
		)
	}

	return suggestions
}

export function getAvailableHours(currentTimeBounds: TimeBounds | null) {
	if (!currentTimeBounds) {
		return []
	}

	return hourOptions.filter(
		hour =>
			hour * 60 <= currentTimeBounds.upperBound &&
			hour * 60 + 59 >= currentTimeBounds.lowerBound
	)
}

export function getAvailableMinutes(
	currentTimeBounds: TimeBounds | null,
	selectedHour?: number
) {
	if (!currentTimeBounds) {
		return []
	}

	if (selectedHour === undefined) {
		return minuteOptions
	}

	return minuteOptions.filter(minute => {
		const totalMinutes = selectedHour * 60 + minute
		return (
			totalMinutes >= currentTimeBounds.lowerBound &&
			totalMinutes <= currentTimeBounds.upperBound
		)
	})
}
