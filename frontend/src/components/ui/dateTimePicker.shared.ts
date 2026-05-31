import {formatMonthName, getWeekdayLabels} from '@/lib/localDateTime'

export type PickerMode = 'datetime' | 'time'

export interface DateTimePickerProps {
	caption?: string
	disabled?: boolean
	id: string
	maxTime?: string
	maxValue?: string
	minTime?: string
	minValue?: string
	mode?: PickerMode
	onChange: (value: string) => void
	placeholder?: string
	quickStepMinutes?: number
	value: string
}

export interface TimeBounds {
	lowerBound: number
	upperBound: number
}

export const hourOptions = Array.from({length: 24}, (_, index) => index)
export const minuteOptions = Array.from({length: 60}, (_, index) => index)
export const monthOptions = Array.from({length: 12}, (_, index) => ({
	label: formatMonthName(index),
	value: index
}))
export const weekdayLabels = getWeekdayLabels()
export const pickerSelectClassName =
	'flex h-11 w-full rounded-xl border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
