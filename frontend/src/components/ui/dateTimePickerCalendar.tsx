import type {Dispatch, SetStateAction} from 'react'
import {Button} from '@/components/ui/button'
import {Input} from '@/components/ui/input'
import {
	addCalendarMonths,
	formatDayAriaLabel,
	formatMonthLabel,
	formatPickerDate,
	parseLocalDate,
	sameCalendarDay
} from '@/lib/localDateTime'
import {cn} from '@/lib/utils'
import {
	monthOptions,
	pickerSelectClassName,
	type TimeBounds,
	weekdayLabels
} from './dateTimePicker.shared'

interface DateTimePickerCalendarProps {
	calendarDays: Array<{date: Date; inCurrentMonth: boolean; key: string}>
	getTimeBounds: (dateValue?: string) => TimeBounds | null
	onSelectDate: (dateValue: string) => void
	quickDateOptions: Array<{label: string; value: string}>
	selectedDateValue: string
	setVisibleMonth: Dispatch<SetStateAction<Date>>
	visibleMonth: Date
}

export function DateTimePickerCalendar({
	calendarDays,
	getTimeBounds,
	onSelectDate,
	quickDateOptions,
	selectedDateValue,
	setVisibleMonth,
	visibleMonth
}: DateTimePickerCalendarProps) {
	return (
		<>
			<div className='flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between'>
				<div>
					<div className='text-xs uppercase tracking-[0.22em] text-slate-500'>
						Date
					</div>
					<div className='mt-1 text-sm text-slate-300'>
						{selectedDateValue
							? formatPickerDate(selectedDateValue)
							: 'Choose a day from the calendar.'}
					</div>
				</div>
				<div className='flex items-center gap-2'>
					<Button
						aria-label='Previous month'
						onClick={() =>
							setVisibleMonth(currentMonth =>
								addCalendarMonths(currentMonth, -1)
							)
						}
						size='sm'
						type='button'
						variant='outline'
					>
						‹
					</Button>
					<div className='grid flex-1 gap-2 sm:grid-cols-[1fr,92px]'>
						<select
							aria-label='Visible month'
							className={pickerSelectClassName}
							onChange={event =>
								setVisibleMonth(currentMonth =>
									new Date(
										currentMonth.getFullYear(),
										Number(event.target.value),
										1
									)
								)
							}
							value={visibleMonth.getMonth()}
						>
							{monthOptions.map(month => (
								<option key={month.value} value={month.value}>
									{month.label}
								</option>
							))}
						</select>
						<Input
							aria-label='Visible year'
							className='text-center'
							inputMode='numeric'
							onChange={event => {
								const nextYear = Number(event.target.value)
								if (Number.isNaN(nextYear)) {
									return
								}

								setVisibleMonth(
									currentMonth =>
										new Date(nextYear, currentMonth.getMonth(), 1)
								)
							}}
							type='number'
							value={visibleMonth.getFullYear()}
						/>
					</div>
					<Button
						aria-label='Next month'
						onClick={() =>
							setVisibleMonth(currentMonth =>
								addCalendarMonths(currentMonth, 1)
							)
						}
						size='sm'
						type='button'
						variant='outline'
					>
						›
					</Button>
				</div>
			</div>

			<div className='mt-4 flex flex-wrap gap-2'>
				{quickDateOptions.map(option => {
					const optionDate = parseLocalDate(option.value)
					const isDisabled = !getTimeBounds(option.value)

					return (
						<Button
							className='rounded-full'
							disabled={isDisabled}
							key={option.value}
							onClick={() => onSelectDate(option.value)}
							size='sm'
							type='button'
							variant={
								selectedDateValue === option.value ? 'default' : 'outline'
							}
						>
							{option.label}
							{optionDate ? (
								<span className='ml-2 text-xs text-inherit/70'>
									{formatMonthLabel(optionDate)}
								</span>
							) : null}
						</Button>
					)
				})}
			</div>

			<div className='mt-4 grid grid-cols-7 gap-2'>
				{weekdayLabels.map(label => (
					<div
						className='px-1 text-center text-[11px] font-medium uppercase tracking-[0.2em] text-slate-500'
						key={label}
					>
						{label}
					</div>
				))}
				{calendarDays.map(({date, inCurrentMonth, key}) => {
					const isDisabled = !getTimeBounds(key)
					const isSelected = selectedDateValue === key
					const isToday = sameCalendarDay(date, new Date())

					return (
						<button
							aria-label={formatDayAriaLabel(date)}
							className={cn(
								'flex aspect-square items-center justify-center rounded-2xl border text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400',
								isSelected
									? 'border-sky-400 bg-sky-500 text-slate-950'
									: 'border-slate-800 bg-slate-900/70 text-slate-200 hover:border-slate-700 hover:bg-slate-900',
								!(inCurrentMonth || isSelected) && 'text-slate-500',
								isToday && !isSelected && 'border-emerald-500/50',
								isDisabled &&
									'cursor-not-allowed border-slate-900 bg-slate-950 text-slate-700 hover:border-slate-900 hover:bg-slate-950'
							)}
							disabled={isDisabled}
							key={key}
							onClick={() => onSelectDate(key)}
							type='button'
						>
							{date.getDate()}
						</button>
					)
				})}
			</div>
		</>
	)
}