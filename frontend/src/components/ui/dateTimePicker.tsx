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
import {cn} from '@/lib/utils'
import {DateTimePickerCalendar} from './dateTimePickerCalendar'
import {DateTimePickerTimePanel} from './dateTimePickerTimePanel'
import {
	type DateTimePickerProps,
	hourOptions,
	minuteOptions,
	type TimeBounds
} from './dateTimePicker.shared'

export function DateTimePicker({
	caption,
	disabled,
	id,
	maxTime,
	maxValue,
	minTime,
	minValue,
	mode = 'datetime',
	onChange,
	placeholder,
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
	const selectedTimeParts = parseTimeValue(selectedTimeValue)
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
	const panelId = `${id}-panel`

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

	return (
		<div className='relative' ref={rootRef}>
			<button
				aria-controls={panelId}
				aria-expanded={isOpen}
				aria-haspopup='dialog'
				className={cn(
					'group flex min-h-15 w-full items-center justify-between rounded-2xl border border-slate-800 bg-slate-950/70 px-4 py-3 text-left shadow-lg shadow-slate-950/20 transition-colors hover:border-slate-700 hover:bg-slate-900/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400',
					isOpen && 'border-sky-400/70 bg-slate-900',
					disabled && 'cursor-not-allowed opacity-60'
				)}
				disabled={disabled}
				id={id}
				onClick={() => setIsOpen(current => !current)}
				type='button'
			>
				<div className='min-w-0'>
					<div className='text-xs uppercase tracking-[0.22em] text-slate-500'>
						{mode === 'datetime' ? 'Date and time' : 'Time'}
					</div>
					<div
						className={cn(
							'mt-1 truncate text-base font-medium',
							displayValue ? 'text-white' : 'text-slate-400'
						)}
					>
						{displayValue ||
							placeholder ||
							(mode === 'datetime'
								? 'Select a reservation slot'
								: 'Select a time')}
					</div>
					<div className='mt-1 truncate text-xs text-slate-500'>
						{caption || defaultCaption}
					</div>
				</div>
				<div className='ml-3 flex shrink-0 items-center gap-2'>
					{selectedTimeValue ? (
						<span className='rounded-full border border-slate-700 bg-slate-900 px-2.5 py-1 text-xs font-medium text-slate-200'>
							{selectedTimeValue}
						</span>
					) : null}
					<span className='text-xl leading-none text-slate-500'>
						{isOpen ? '−' : '+'}
					</span>
				</div>
			</button>

			{isOpen ? (
				<div
					aria-label={
						mode === 'datetime' ? 'Choose booking date and time' : 'Choose time'
					}
					className='absolute left-0 z-20 mt-3 w-full rounded-[1.5rem] border border-slate-800 bg-slate-950/98 p-4 shadow-2xl shadow-slate-950/50 backdrop-blur sm:p-5'
					id={panelId}
					role='dialog'
				>
					{mode === 'datetime' ? (
						<DateTimePickerCalendar
							calendarDays={calendarDays}
							getTimeBounds={getTimeBounds}
							onSelectDate={setDateValue}
							quickDateOptions={quickDateOptions}
							selectedDateValue={selectedDateValue}
							setVisibleMonth={setVisibleMonth}
							visibleMonth={visibleMonth}
						/>
					) : null}

					<DateTimePickerTimePanel
						availableHours={availableHours}
						availableMinutes={availableMinutes}
						canSelectTime={mode === 'time' || Boolean(selectedDateValue)}
						currentTimeBounds={currentTimeBounds}
						displayValue={displayValue}
						mode={mode}
						onClear={() => onChange('')}
						onClose={() => setIsOpen(false)}
						onSelectTime={setTimeValue}
						quickTimeSuggestions={quickTimeSuggestions}
						selectedDateValue={selectedDateValue}
						selectedHour={selectedTimeParts?.hours}
						selectedMinute={selectedTimeParts?.minutes}
						selectedTimeValue={selectedTimeValue}
						value={value}
					/>
				</div>
			) : null}
		</div>
	)
}

function createQuickDateOptions() {
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

function createQuickTimeSuggestions(
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

function getAvailableHours(currentTimeBounds: TimeBounds | null) {
	if (!currentTimeBounds) {
		return []
	}

	return hourOptions.filter(
		hour =>
			hour * 60 <= currentTimeBounds.upperBound &&
			hour * 60 + 59 >= currentTimeBounds.lowerBound
	)
}

function getAvailableMinutes(
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
