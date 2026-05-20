import {DateTimePicker} from '@/components/ui/dateTimePicker'
import {Label} from '@/components/ui/label'
import {formatTimeRange, normalizeTimeValue} from '@/lib/localDateTime'

interface BookingScheduleFieldsProps {
	endTime: string
	minimumBookingDateTime: string
	minimumEndDateTime: string
	onChangeEndTime: (value: string) => void
	onChangeStartTime: (value: string) => void
	startTime: string
	workingHoursEnd?: string | null
	workingHoursStart?: string | null
}

export function BookingScheduleFields({
	endTime,
	minimumBookingDateTime,
	minimumEndDateTime,
	onChangeEndTime,
	onChangeStartTime,
	startTime,
	workingHoursEnd,
	workingHoursStart
}: BookingScheduleFieldsProps) {
	const facilityHoursLabel =
		workingHoursStart && workingHoursEnd
			? formatTimeRange(workingHoursStart, workingHoursEnd)
			: ''
	const normalizedWorkingHoursStart = normalizeTimeValue(workingHoursStart)
	const normalizedWorkingHoursEnd = normalizeTimeValue(workingHoursEnd)

	return (
		<div className='grid gap-5 md:grid-cols-2'>
			<div className='space-y-2'>
				<Label htmlFor='startTime'>Start time</Label>
				<DateTimePicker
					caption={facilityHoursLabel ? `Available ${facilityHoursLabel}` : undefined}
					id='startTime'
					maxTime={normalizedWorkingHoursEnd}
					minTime={normalizedWorkingHoursStart}
					minValue={minimumBookingDateTime}
					onChange={onChangeStartTime}
					placeholder='Choose when the booking should begin'
					quickStepMinutes={60}
					value={startTime}
				/>
			</div>

			<div className='space-y-2'>
				<Label htmlFor='endTime'>End time</Label>
				<DateTimePicker
					caption={facilityHoursLabel ? `Finish inside ${facilityHoursLabel}` : undefined}
					disabled={!startTime}
					id='endTime'
					maxTime={normalizedWorkingHoursEnd}
					minTime={normalizedWorkingHoursStart}
					minValue={minimumEndDateTime}
					onChange={onChangeEndTime}
					placeholder={
						startTime
							? 'Choose when the booking should end'
							: 'Select the start time first'
					}
					quickStepMinutes={60}
					value={endTime}
				/>
			</div>
		</div>
	)
}