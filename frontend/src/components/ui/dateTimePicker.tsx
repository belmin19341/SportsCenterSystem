import {cn} from '@/lib/utils'
import type {DateTimePickerProps} from './dateTimePicker.shared'
import {DateTimePickerCalendar} from './dateTimePickerCalendar'
import {useDateTimePickerState} from './dateTimePickerState'
import {DateTimePickerTimePanel} from './dateTimePickerTimePanel'

export function DateTimePicker({
	caption,
	disabled,
	id,
	mode = 'datetime',
	placeholder,
	...props
}: DateTimePickerProps) {
	const {
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
	} = useDateTimePickerState({...props, id, mode})
	const panelId = `${id}-panel`

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
					className='absolute left-0 z-20 mt-3 w-full rounded-3xl border border-slate-800 bg-slate-950/98 p-4 shadow-2xl shadow-slate-950/50 backdrop-blur sm:p-5'
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
						onClear={() => props.onChange('')}
						onClose={() => setIsOpen(false)}
						onSelectTime={setTimeValue}
						quickTimeSuggestions={quickTimeSuggestions}
						selectedDateValue={selectedDateValue}
						selectedHour={selectedTimeParts?.hours}
						selectedMinute={selectedTimeParts?.minutes}
						selectedTimeValue={selectedTimeValue}
						value={props.value}
					/>
				</div>
			) : null}
		</div>
	)
}
