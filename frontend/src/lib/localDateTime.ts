const monthFormatter = new Intl.DateTimeFormat('en-GB', {month: 'long'})
const monthLabelFormatter = new Intl.DateTimeFormat('en-GB', {
	month: 'long',
	year: 'numeric'
})
const pickerDateFormatter = new Intl.DateTimeFormat('en-GB', {
	day: 'numeric',
	month: 'short',
	weekday: 'short'
})
const pickerDateTimeFormatter = new Intl.DateTimeFormat('en-GB', {
	day: 'numeric',
	hour: '2-digit',
	minute: '2-digit',
	month: 'short',
	weekday: 'short'
})
const pickerTimeFormatter = new Intl.DateTimeFormat('en-GB', {
	hour: '2-digit',
	minute: '2-digit'
})
const dayAriaFormatter = new Intl.DateTimeFormat('en-GB', {
	day: 'numeric',
	month: 'long',
	weekday: 'long',
	year: 'numeric'
})
const weekdayFormatter = new Intl.DateTimeFormat('en-GB', {weekday: 'short'})

export function padTimeUnit(value: number) {
	return String(value).padStart(2, '0')
}

export function normalizeTimeValue(value: string | null | undefined) {
	if (!value) {
		return ''
	}

	const [hours = '', minutes = ''] = value.trim().split(':')
	if (!(hours && minutes)) {
		return value.trim()
	}

	return `${hours.padStart(2, '0').slice(-2)}:${minutes.padStart(2, '0').slice(-2)}`
}

export function parseTimeValue(value: string | null | undefined) {
	const normalizedValue = normalizeTimeValue(value)
	if (!normalizedValue) {
		return null
	}

	const [hours = '', minutes = ''] = normalizedValue.split(':')
	const parsedHours = Number(hours)
	const parsedMinutes = Number(minutes)

	if (
		Number.isNaN(parsedHours) ||
		Number.isNaN(parsedMinutes) ||
		parsedHours < 0 ||
		parsedHours > 23 ||
		parsedMinutes < 0 ||
		parsedMinutes > 59
	) {
		return null
	}

	return {
		hours: parsedHours,
		minutes: parsedMinutes,
		totalMinutes: parsedHours * 60 + parsedMinutes
	}
}

export function toLocalTimeValue(hours: number, minutes: number) {
	return `${padTimeUnit(hours)}:${padTimeUnit(minutes)}`
}

export function parseLocalDate(value: string | null | undefined) {
	if (!value) {
		return null
	}

	const [year = '', month = '', day = ''] = value.split('-')
	const parsedYear = Number(year)
	const parsedMonth = Number(month)
	const parsedDay = Number(day)

	if (
		Number.isNaN(parsedYear) ||
		Number.isNaN(parsedMonth) ||
		Number.isNaN(parsedDay) ||
		parsedMonth < 1 ||
		parsedMonth > 12 ||
		parsedDay < 1 ||
		parsedDay > 31
	) {
		return null
	}

	const date = new Date(parsedYear, parsedMonth - 1, parsedDay)
	return date.getFullYear() === parsedYear &&
		date.getMonth() === parsedMonth - 1 &&
		date.getDate() === parsedDay
		? date
		: null
}

export function toLocalDateValue(date: Date) {
	return [
		date.getFullYear(),
		padTimeUnit(date.getMonth() + 1),
		padTimeUnit(date.getDate())
	].join('-')
}

export function getDatePart(value: string | null | undefined) {
	if (!value) {
		return ''
	}

	return value.split('T')[0] || ''
}

export function getTimePart(value: string | null | undefined) {
	if (!value) {
		return ''
	}

	return normalizeTimeValue(value.split('T')[1] || '')
}

export function parseLocalDateTime(value: string | null | undefined) {
	if (!value) {
		return null
	}

	const dateValue = getDatePart(value)
	const timeValue = getTimePart(value)
	const date = parseLocalDate(dateValue)
	const time = parseTimeValue(timeValue)

	if (!(date && time)) {
		return null
	}

	return new Date(
		date.getFullYear(),
		date.getMonth(),
		date.getDate(),
		time.hours,
		time.minutes,
		0,
		0
	)
}

export function toLocalDateTimeValue(date: Date) {
	return `${toLocalDateValue(date)}T${toLocalTimeValue(
		date.getHours(),
		date.getMinutes()
	)}`
}

export function combineLocalDateAndTime(
	dateValue: string | null | undefined,
	timeValue: string | null | undefined
) {
	const normalizedTimeValue = normalizeTimeValue(timeValue)
	return dateValue && normalizedTimeValue
		? `${dateValue}T${normalizedTimeValue}`
		: ''
}

export function addMinutes(date: Date, minutes: number) {
	return new Date(date.getTime() + minutes * 60_000)
}

export function addLocalDateTimeMinutes(
	value: string | null | undefined,
	minutes: number
) {
	const date = parseLocalDateTime(value)
	return date ? toLocalDateTimeValue(addMinutes(date, minutes)) : ''
}

export function roundDateUp(date: Date, stepMinutes: number) {
	const roundedDate = new Date(date)
	roundedDate.setSeconds(0, 0)

	const remainder = roundedDate.getMinutes() % stepMinutes
	if (remainder === 0) {
		return roundedDate
	}

	roundedDate.setMinutes(roundedDate.getMinutes() + stepMinutes - remainder)
	return roundedDate
}

export function sameCalendarDay(left: Date, right: Date) {
	return (
		left.getFullYear() === right.getFullYear() &&
		left.getMonth() === right.getMonth() &&
		left.getDate() === right.getDate()
	)
}

export function startOfMonth(date: Date) {
	return new Date(date.getFullYear(), date.getMonth(), 1)
}

export function addCalendarMonths(date: Date, months: number) {
	return new Date(date.getFullYear(), date.getMonth() + months, 1)
}

export function buildCalendarDays(monthDate: Date) {
	const firstDayOfMonth = startOfMonth(monthDate)
	const firstWeekdayIndex = (firstDayOfMonth.getDay() + 6) % 7
	const firstVisibleDay = new Date(
		monthDate.getFullYear(),
		monthDate.getMonth(),
		1 - firstWeekdayIndex
	)

	return Array.from({length: 42}, (_, index) => {
		const date = new Date(firstVisibleDay)
		date.setDate(firstVisibleDay.getDate() + index)

		return {
			date,
			inCurrentMonth: date.getMonth() === monthDate.getMonth(),
			key: toLocalDateValue(date)
		}
	})
}

export function formatMonthName(index: number) {
	return monthFormatter.format(new Date(2026, index, 1))
}

export function formatMonthLabel(date: Date) {
	return monthLabelFormatter.format(date)
}

export function formatPickerDateTime(value: string | null | undefined) {
	const date = parseLocalDateTime(value)
	return date ? pickerDateTimeFormatter.format(date) : ''
}

export function formatPickerTime(value: string | null | undefined) {
	const parsedTime = parseTimeValue(value)
	if (!parsedTime) {
		return ''
	}

	return pickerTimeFormatter.format(
		new Date(2026, 0, 1, parsedTime.hours, parsedTime.minutes)
	)
}

export function formatPickerDate(value: string | null | undefined) {
	const date = parseLocalDate(value)
	return date ? pickerDateFormatter.format(date) : ''
}

export function formatDayAriaLabel(date: Date) {
	return dayAriaFormatter.format(date)
}

export function getWeekdayLabels() {
	return Array.from({length: 7}, (_, index) => {
		const date = new Date(2026, 0, 5 + index)
		return weekdayFormatter.format(date)
	})
}

export function formatTimeRange(
	start: string | null | undefined,
	end: string | null | undefined
) {
	const normalizedStart = normalizeTimeValue(start)
	const normalizedEnd = normalizeTimeValue(end)

	if (!(normalizedStart && normalizedEnd)) {
		return [normalizedStart, normalizedEnd].filter(Boolean).join(' - ')
	}

	return `${normalizedStart} - ${normalizedEnd}`
}