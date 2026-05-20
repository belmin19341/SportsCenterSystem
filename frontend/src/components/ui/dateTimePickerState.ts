import {useEffect, useRef, useState} from 'react'
import {
	buildCalendarDays,
	combineLocalDateAndTime,
	formatPickerDateTime,
	formatPickerTime,
	formatTimeRange,
	normalizeTimeValue,
	parseLocalDateTime,
	parseTimeValue,
	startOfMonth,
	toLocalDateValue,
	toLocalTimeValue
} from '@/lib/localDateTime'
import type {DateTimePickerProps, TimeBounds} from './dateTimePicker.shared'
import {
	createQuickDateOptions,
	createQuickTimeSuggestions,
	getAvailableHours,
	getAvailableMinutes
} from './dateTimePickerHelpers'

export function useDateTimePickerState({
	maxTime,
	maxValue,
	minTime,
	minValue,
	mode = 'datetime',
	onChange,
	quickStepMinutes = 30,
	value
}: DateTimePickerProps) {
	const rootRef = useRef<HTMLDivElement>(null)
	const [isOpen, setIsOpen] = useState(false)
	const normalizedValue = mode === 'time' ? normalizeTimeValue(value) : value
	const selectedDateTime =
		mode === 'datetime' ? parseLocalDateTime(normalizedValue) : null
	const minDateTime = mode === 'datetime' ? parseLocalDateTime(minValue) : null
	const maxDateTime = mode === 'datetime' ? parseLocalDateTime(maxValue) : null
	const selectedDateValue = selectedDateTime
		? toLocalDateValue(selectedDateTime)
		: ''
	const selectedTimeValue = selectedDateTime
		? toLocalTimeValue(
				selectedDateTime.getHours(),
				selectedDateTime.getMinutes()
			)
		: normalizeTimeValue(normalizedValue)
	const selectedTimeParts = parseTimeValue(selectedTimeValue)
	const normalizedMinTime = normalizeTimeValue(minTime)
	const normalizedMaxTime = normalizeTimeValue(maxTime)
	const minTimeMinutes = parseTimeValue(normalizedMinTime)?.totalMinutes ?? 0
	const maxTimeMinutes = parseTimeValue(normalizedMaxTime)?.totalMinutes ?? 1439
	const [visibleMonth, setVisibleMonth] = useState(() =>
		startOfMonth(selectedDateTime ?? minDateTime ?? new Date())
	)

	useEffect(() => {
		if (!isOpen) {
			return
		}

		function handlePointerDown(event: MouseEvent) {
			if (
				rootRef.current &&
				event.target instanceof Node &&
				!rootRef.current.contains(event.target)
			) {
				setIsOpen(false)
			}
		}

		function handleEscape(event: KeyboardEvent) {
			if (event.key === 'Escape') {
				setIsOpen(false)
			}
		}

		document.addEventListener('mousedown', handlePointerDown)
		document.addEventListener('keydown', handleEscape)

		return () => {
			document.removeEventListener('mousedown', handlePointerDown)
			document.removeEventListener('keydown', handleEscape)
		}
	}, [isOpen])

	useEffect(() => {
		if (!(isOpen && mode === 'datetime')) {
			return
		}

		const focusDate = selectedDateTime ?? minDateTime ?? new Date()
		setVisibleMonth(currentMonth =>
			focusDate.getFullYear() === currentMonth.getFullYear() &&
			focusDate.getMonth() === currentMonth.getMonth()
				? currentMonth
				: startOfMonth(focusDate)
		)
	}, [isOpen, minDateTime, mode, selectedDateTime])

	function getTimeBounds(dateValue?: string): TimeBounds | null {
		let lowerBound = minTimeMinutes
		let upperBound = maxTimeMinutes

		if (mode === 'datetime') {
			if (!dateValue) {
				return null
			}

			const minDateValue = minDateTime ? toLocalDateValue(minDateTime) : ''
			const maxDateValue = maxDateTime ? toLocalDateValue(maxDateTime) : ''

			if (minDateValue && dateValue < minDateValue) {
				return null
			}

			if (maxDateValue && dateValue > maxDateValue) {
				return null
			}

			if (minDateTime && minDateValue && dateValue === minDateValue) {
				lowerBound = Math.max(
					lowerBound,
					minDateTime.getHours() * 60 + minDateTime.getMinutes()
				)
			}

			if (maxDateTime && maxDateValue && dateValue === maxDateValue) {
				upperBound = Math.min(
					upperBound,
					maxDateTime.getHours() * 60 + maxDateTime.getMinutes()
				)
			}
		}

		return lowerBound <= upperBound ? {lowerBound, upperBound} : null
	}

	function clampTimeValue(timeValue: string, dateValue?: string) {
		const parsedTime = parseTimeValue(timeValue)
		const bounds =
			mode === 'datetime' ? getTimeBounds(dateValue) : getTimeBounds()

		if (!(parsedTime && bounds)) {
			return ''
		}

		const boundedMinutes = Math.min(
			bounds.upperBound,
			Math.max(bounds.lowerBound, parsedTime.totalMinutes)
		)

		return toLocalTimeValue(
			Math.floor(boundedMinutes / 60),
			boundedMinutes % 60
		)
	}

	const currentTimeBounds =
		mode === 'datetime' ? getTimeBounds(selectedDateValue) : getTimeBounds()
	const quickTimeSuggestions = createQuickTimeSuggestions(
		mode,
		quickStepMinutes,
		selectedDateValue,
		getTimeBounds
	)
	const availableHours = getAvailableHours(currentTimeBounds)
	const availableMinutes = getAvailableMinutes(
		currentTimeBounds,
		selectedTimeParts?.hours
	)
	const calendarDays =
		mode === 'datetime' ? buildCalendarDays(visibleMonth) : []
	const quickDateOptions = mode === 'datetime' ? createQuickDateOptions() : []
	const displayValue =
		mode === 'datetime'
			? formatPickerDateTime(normalizedValue)
			: formatPickerTime(normalizedValue)
	const defaultCaption =
		normalizedMinTime || normalizedMaxTime
			? `Available ${formatTimeRange(
					normalizedMinTime || '00:00',
					normalizedMaxTime || '23:59'
				)}`
			: mode === 'datetime'
				? 'Pick a date, then fine-tune the exact time.'
				: 'Choose the opening or closing time.'

	function setDateValue(dateValue: string) {
		const bounds = getTimeBounds(dateValue)
		if (!bounds) {
			return
		}

		const nextTime = selectedTimeValue
			? clampTimeValue(selectedTimeValue, dateValue)
			: toLocalTimeValue(
					Math.floor(bounds.lowerBound / 60),
					bounds.lowerBound % 60
				)

		onChange(combineLocalDateAndTime(dateValue, nextTime))
	}

	function setTimeValue(timeValue: string, closeAfterSelect = false) {
		if (mode === 'time') {
			const nextTimeValue = clampTimeValue(timeValue)
			if (!nextTimeValue) {
				return
			}

			onChange(nextTimeValue)
			if (closeAfterSelect) {
				setIsOpen(false)
			}
			return
		}

		const nextDateValue =
			selectedDateValue ||
			(minDateTime
				? toLocalDateValue(minDateTime)
				: toLocalDateValue(new Date()))
		const nextTimeValue = clampTimeValue(timeValue, nextDateValue)
		if (!nextTimeValue) {
			return
		}

		onChange(combineLocalDateAndTime(nextDateValue, nextTimeValue))
		if (closeAfterSelect) {
			setIsOpen(false)
		}
	}

	return {
		availableHours,
		availableMinutes,
		calendarDays,
		currentTimeBounds,
		defaultCaption,
		displayValue,
		getTimeBounds,
		isOpen,
		quickDateOptions,
		quickTimeSuggestions,
		rootRef,
		selectedDateValue,
		selectedTimeParts,
		selectedTimeValue,
		setDateValue,
		setIsOpen,
		setTimeValue,
		setVisibleMonth,
		visibleMonth
	}
}
