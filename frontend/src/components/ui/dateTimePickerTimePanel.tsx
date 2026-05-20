import {Button} from '@/components/ui/button'
import {toLocalTimeValue} from '@/lib/localDateTime'
import {
	pickerSelectClassName,
	type PickerMode,
	type TimeBounds
} from './dateTimePicker.shared'

interface DateTimePickerTimePanelProps {
	availableHours: number[]
	availableMinutes: number[]
	canSelectTime: boolean
	currentTimeBounds: TimeBounds | null
	displayValue: string
	mode: PickerMode
	onClear: () => void
	onClose: () => void
	onSelectTime: (timeValue: string, closeAfterSelect?: boolean) => void
	quickTimeSuggestions: string[]
	selectedDateValue: string
	selectedHour?: number
	selectedMinute?: number
	selectedTimeValue: string
	value: string
}

export function DateTimePickerTimePanel({
	availableHours,
	availableMinutes,
	canSelectTime,
	currentTimeBounds,
	displayValue,
	mode,
	onClear,
	onClose,
	onSelectTime,
	quickTimeSuggestions,
	selectedDateValue,
	selectedHour,
	selectedMinute,
	selectedTimeValue,
	value
}: DateTimePickerTimePanelProps) {
	return (
		<div className={mode === 'datetime' ? 'mt-4 border-t border-slate-800 pt-4' : ''}>
			<div className='flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between'>
				<div>
					<div className='text-xs uppercase tracking-[0.22em] text-slate-500'>
						Time
					</div>
					<div className='mt-1 text-sm text-slate-300'>
						{mode === 'datetime' && !selectedDateValue
							? 'Pick a date first to unlock available times.'
							: selectedTimeValue || 'Choose an exact time below.'}
					</div>
				</div>
				{value ? (
					<button
						className='text-sm text-slate-400 transition-colors hover:text-white'
						onClick={onClear}
						type='button'
					>
						Clear
					</button>
				) : null}
			</div>

			<div className='mt-3 grid gap-3 sm:grid-cols-2'>
				<div>
					<div className='mb-2 text-xs font-medium uppercase tracking-[0.18em] text-slate-500'>
						Hour
					</div>
					<select
						aria-label='Hour'
						className={pickerSelectClassName}
						disabled={!canSelectTime || currentTimeBounds === null}
						onChange={event => {
							const nextHour = Number(event.target.value)
							const nextMinute = selectedMinute ?? 0
							onSelectTime(toLocalTimeValue(nextHour, nextMinute))
						}}
						value={selectedHour ?? ''}
					>
						{!canSelectTime ? (
							<option value=''>Select a date first</option>
						) : null}
						{availableHours.map(hour => (
							<option key={hour} value={hour}>
								{toLocalTimeValue(hour, 0).slice(0, 2)}
							</option>
						))}
					</select>
				</div>

				<div>
					<div className='mb-2 text-xs font-medium uppercase tracking-[0.18em] text-slate-500'>
						Minute
					</div>
					<select
						aria-label='Minute'
						className={pickerSelectClassName}
						disabled={!canSelectTime || currentTimeBounds === null}
						onChange={event => {
							const nextMinute = Number(event.target.value)
							const nextHour = selectedHour ?? availableHours[0] ?? 0
							onSelectTime(toLocalTimeValue(nextHour, nextMinute))
						}}
						value={selectedMinute ?? ''}
					>
						{!canSelectTime ? (
							<option value=''>Select a date first</option>
						) : null}
						{availableMinutes.map(minute => (
							<option key={minute} value={minute}>
								{toLocalTimeValue(0, minute).slice(3, 5)}
							</option>
						))}
					</select>
				</div>
			</div>

			<div className='mt-4'>
				<div className='mb-2 text-xs font-medium uppercase tracking-[0.18em] text-slate-500'>
					Quick times
				</div>
				<div className='flex flex-wrap gap-2'>
					{quickTimeSuggestions.map(timeOption => (
						<Button
							className='rounded-full'
							key={timeOption}
							onClick={() => onSelectTime(timeOption, true)}
							size='sm'
							type='button'
							variant={selectedTimeValue === timeOption ? 'default' : 'outline'}
						>
							{timeOption}
						</Button>
					))}
				</div>
			</div>

			<div className='mt-4 flex items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-900/60 px-4 py-3'>
				<div className='min-w-0'>
					<div className='text-xs uppercase tracking-[0.18em] text-slate-500'>
						Selected
					</div>
					<div className='mt-1 truncate text-sm font-medium text-white'>
						{displayValue || 'Nothing selected yet'}
					</div>
				</div>
				<Button onClick={onClose} size='sm' type='button'>
					Done
				</Button>
			</div>
		</div>
	)
}