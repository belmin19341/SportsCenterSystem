import {useOwnerFacilitiesData} from '@/hooks/useOwnerFacilitiesData'
import {LoadingOrError} from '@/components/loadingOrError'
import {Alert} from '@/components/ui/alert'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'
import {DateTimePicker} from '@/components/ui/dateTimePicker'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {formatCurrency} from '@/lib/format'
import {formatTimeRange} from '@/lib/localDateTime'
import type {FacilityRequest, FacilityStatus, FacilityType} from '@/types/api'

const facilityTypes = [
	'FOOTBALL_5V5',
	'FOOTBALL_7V7',
	'PADEL',
	'TENNIS',
	'TABLE_TENNIS'
] satisfies FacilityType[]

const facilityStatuses = [
	'ACTIVE',
	'INACTIVE',
	'MAINTENANCE'
] satisfies FacilityStatus[]

function humanizeLabel(value: string) {
	return value.replaceAll('_', ' ').toLowerCase()
}

export function OwnerFacilitiesPage() {
	const {
		session,
		facilitiesQuery,
		createMutation,
		form,
		setForm,
		errorMessage,
		handleSubmit
	} = useOwnerFacilitiesData();

	if (!session) {
		return null
	}

	return (
		<div className='grid gap-6 xl:grid-cols-[0.9fr,1.1fr]'>
			<Card>
				<CardHeader>
					<CardTitle>Create facility</CardTitle>
					<CardDescription>
						Owner-managed courts use the signed-in user ID.
					</CardDescription>
				</CardHeader>
				<CardContent>
					<form className='space-y-5' onSubmit={handleSubmit}>
						<div className='space-y-2'>
							<Label htmlFor='facilityName'>Name</Label>
							<Input
								id='facilityName'
								onChange={event =>
									setForm(current => ({...current, name: event.target.value}))
								}
								required={true}
								value={form.name}
							/>
						</div>

						<div className='grid gap-4 sm:grid-cols-2'>
							<div className='space-y-2'>
								<Label htmlFor='facilityType'>Type</Label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityType'
									onChange={event =>
										setForm(current => ({
											...current,
											type: event.target.value as FacilityType
										}))
									}
									value={form.type}
								>
									{facilityTypes.map(type => (
										<option key={type} value={type}>
											{humanizeLabel(type)}
										</option>
									))}
								</select>
							</div>
							<div className='space-y-2'>
								<Label htmlFor='facilityStatus'>Status</Label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityStatus'
									onChange={event =>
										setForm(current => ({
											...current,
											status: event.target.value as FacilityStatus
										}))
									}
									value={form.status}
								>
									{facilityStatuses.map(status => (
										<option key={status} value={status}>
											{humanizeLabel(status)}
										</option>
									))}
								</select>
							</div>
						</div>

						<div className='grid gap-4 sm:grid-cols-2'>
							<div className='space-y-2'>
								<Label htmlFor='capacity'>Capacity</Label>
								<Input
									id='capacity'
									min={1}
									onChange={event =>
										setForm(current => ({
											...current,
											capacity: Number(event.target.value)
										}))
									}
									required={true}
									type='number'
									value={form.capacity}
								/>
							</div>
							<div className='space-y-2'>
								<Label htmlFor='basePrice'>Base price per hour</Label>
								<Input
									id='basePrice'
									min={0.01}
									onChange={event =>
										setForm(current => ({
											...current,
											basePricePerHour: Number(event.target.value)
										}))
									}
									required={true}
									step={0.01}
									type='number'
									value={form.basePricePerHour}
								/>
							</div>
						</div>

						<div className='grid gap-4 sm:grid-cols-2'>
							<div className='space-y-2'>
								<Label htmlFor='hoursStart'>Opens</Label>
								<DateTimePicker
									caption='Shown as the first available booking time.'
									id='hoursStart'
									mode='time'
									onChange={nextValue =>
										setForm(current => ({
											...current,
											workingHoursStart: nextValue
										}))
									}
									placeholder='Choose the opening time'
									quickStepMinutes={60}
									value={form.workingHoursStart}
								/>
							</div>
							<div className='space-y-2'>
								<Label htmlFor='hoursEnd'>Closes</Label>
								<DateTimePicker
									caption='Shown as the last available booking time.'
									id='hoursEnd'
									mode='time'
									onChange={nextValue =>
										setForm(current => ({
											...current,
											workingHoursEnd: nextValue
										}))
									}
									placeholder='Choose the closing time'
									quickStepMinutes={60}
									value={form.workingHoursEnd}
								/>
							</div>
						</div>

						<div className='space-y-2'>
							<Label htmlFor='description'>Description</Label>
							<textarea
								className='min-h-24 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
								id='description'
								onChange={event =>
									setForm(current => ({
										...current,
										description: event.target.value
									}))
								}
								value={form.description || ''}
							/>
						</div>

						{errorMessage ? (
							<Alert variant='destructive'>{errorMessage}</Alert>
						) : null}

						<Button
							className='w-full'
							disabled={createMutation.isPending}
							type='submit'
						>
							{createMutation.isPending ? 'Creating...' : 'Create facility'}
						</Button>
					</form>
				</CardContent>
			</Card>

			<Card>
				<CardHeader>
					<CardTitle>Facilities</CardTitle>
					<CardDescription>Courts visible to this role.</CardDescription>
				</CardHeader>
				<CardContent>
					{facilitiesQuery.isPending ? (
						<LoadingOrError title='Loading facilities' />
					) : facilitiesQuery.isError ? (
						<LoadingOrError error={facilitiesQuery.error} />
					) : facilitiesQuery.data.length === 0 ? (
						<div className='text-sm text-slate-400'>No facilities found.</div>
					) : (
						<div className='space-y-3'>
							{facilitiesQuery.data.map(facility => (
								<div
									className='rounded-lg border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'
									key={facility.id}
								>
									<div className='flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
										<div className='min-w-0'>
											<div className='font-medium text-white'>
												{facility.name}
											</div>
											<div className='mt-1 text-slate-400'>
												{formatTimeRange(
													facility.workingHoursStart,
													facility.workingHoursEnd
												)}
											</div>
										</div>
										<div className='flex flex-wrap gap-2 sm:justify-end'>
											<Badge>{humanizeLabel(facility.type)}</Badge>
											<Badge
												variant={
													facility.status === 'ACTIVE' ? 'success' : 'warning'
												}
											>
												{facility.status}
											</Badge>
											<Badge variant='muted'>
												{formatCurrency(facility.basePricePerHour)}/h
											</Badge>
										</div>
									</div>
								</div>
							))}
						</div>
					)}
				</CardContent>
			</Card>
		</div>
	)
}
