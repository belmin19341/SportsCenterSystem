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
import {FieldError} from '@/components/ui/fieldError'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {formatCurrency} from '@/lib/format'
import {formatTimeRange} from '@/lib/localDateTime'
import type {FacilityStatus, FacilityType} from '@/types/api'

const DESCRIPTION_MAX = 500

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
		activeForm,
		setActiveForm,
		activeFieldErrors,
		activeApiError,
		isSubmitting,
		isEditing,
		editingFacility,
		handleBlur,
		handleSubmit,
		handleStartEdit,
		handleCancelEdit
	} = useOwnerFacilitiesData()

	if (!session) {
		return null
	}

	const descriptionLength = (activeForm.description ?? '').length

	return (
		<div className='grid gap-6 xl:grid-cols-[0.9fr,1.1fr]'>
			<Card>
				<CardHeader>
					<div className='flex items-center justify-between gap-2'>
						<div>
							<CardTitle>
								{isEditing ? `Editing: ${editingFacility?.name}` : 'Create facility'}
							</CardTitle>
							<CardDescription>
								{isEditing
									? 'Update the facility details below.'
									: 'Owner-managed courts use the signed-in user ID.'}
							</CardDescription>
						</div>
						{isEditing && (
							<Button
								onClick={handleCancelEdit}
								size='sm'
								type='button'
								variant='outline'
							>
								Cancel
							</Button>
						)}
					</div>
				</CardHeader>
				<CardContent>
					<form className='space-y-5' onSubmit={handleSubmit}>
						<div className='space-y-1'>
							<Label htmlFor='facilityName'>Name</Label>
							<Input
								id='facilityName'
								isInvalid={activeFieldErrors.name !== null}
								onBlur={() => handleBlur('name')}
								onChange={event =>
									setActiveForm(current => ({...current, name: event.target.value}))
								}
								value={activeForm.name}
							/>
							<FieldError message={activeFieldErrors.name} />
						</div>

						<div className='grid gap-4 sm:grid-cols-2'>
							<div className='space-y-1'>
								<Label htmlFor='facilityType'>Type</Label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityType'
									onChange={event =>
										setActiveForm(current => ({
											...current,
											type: event.target.value as FacilityType
										}))
									}
									value={activeForm.type}
								>
									{facilityTypes.map(type => (
										<option key={type} value={type}>
											{humanizeLabel(type)}
										</option>
									))}
								</select>
							</div>
							<div className='space-y-1'>
								<Label htmlFor='facilityStatus'>Status</Label>
								<select
									className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
									id='facilityStatus'
									onChange={event =>
										setActiveForm(current => ({
											...current,
											status: event.target.value as FacilityStatus
										}))
									}
									value={activeForm.status}
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
							<div className='space-y-1'>
								<Label htmlFor='capacity'>Capacity</Label>
								<Input
									id='capacity'
									isInvalid={activeFieldErrors.capacity !== null}
									min={1}
									onBlur={() => handleBlur('capacity')}
									onChange={event =>
										setActiveForm(current => ({
											...current,
											capacity: Number(event.target.value)
										}))
									}
									type='number'
									value={activeForm.capacity}
								/>
								<FieldError message={activeFieldErrors.capacity} />
							</div>
							<div className='space-y-1'>
								<Label htmlFor='basePrice'>Base price per hour</Label>
								<Input
									id='basePrice'
									isInvalid={activeFieldErrors.basePricePerHour !== null}
									min={0.01}
									onBlur={() => handleBlur('basePricePerHour')}
									onChange={event =>
										setActiveForm(current => ({
											...current,
											basePricePerHour: Number(event.target.value)
										}))
									}
									step={0.01}
									type='number'
									value={activeForm.basePricePerHour}
								/>
								<FieldError message={activeFieldErrors.basePricePerHour} />
							</div>
						</div>

						<div className='grid gap-4 sm:grid-cols-2'>
							<div className='space-y-1'>
								<Label htmlFor='hoursStart'>Opens</Label>
								<DateTimePicker
									caption='Shown as the first available booking time.'
									id='hoursStart'
									mode='time'
									onChange={nextValue =>
										setActiveForm(current => ({
											...current,
											workingHoursStart: nextValue
										}))
									}
									placeholder='Choose the opening time'
									quickStepMinutes={60}
									value={activeForm.workingHoursStart}
								/>
							</div>
							<div className='space-y-1'>
								<Label htmlFor='hoursEnd'>Closes</Label>
								<DateTimePicker
									caption='Shown as the last available booking time.'
									id='hoursEnd'
									mode='time'
									onChange={nextValue =>
										setActiveForm(current => ({
											...current,
											workingHoursEnd: nextValue
										}))
									}
									placeholder='Choose the closing time'
									quickStepMinutes={60}
									value={activeForm.workingHoursEnd}
								/>
							</div>
						</div>
						{activeFieldErrors.workingHours ? (
							<FieldError message={activeFieldErrors.workingHours} />
						) : null}

						<div className='space-y-1'>
							<div className='flex items-center justify-between'>
								<Label htmlFor='description'>Description</Label>
								<span
									className={`text-xs ${descriptionLength > DESCRIPTION_MAX ? 'text-rose-400' : 'text-slate-500'}`}
								>
									{descriptionLength}/{DESCRIPTION_MAX}
								</span>
							</div>
							<textarea
								className='min-h-24 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
								id='description'
								onChange={event =>
									setActiveForm(current => ({
										...current,
										description: event.target.value
									}))
								}
								value={activeForm.description ?? ''}
							/>
							{descriptionLength > DESCRIPTION_MAX ? (
								<FieldError
									message={`Description must be at most ${DESCRIPTION_MAX} characters.`}
								/>
							) : null}
						</div>

						{activeApiError ? (
							<Alert variant='destructive'>{activeApiError}</Alert>
						) : null}

						<Button
							className='w-full'
							disabled={isSubmitting || descriptionLength > DESCRIPTION_MAX}
							type='submit'
						>
							{isSubmitting
								? isEditing
									? 'Saving...'
									: 'Creating...'
								: isEditing
									? 'Save changes'
									: 'Create facility'}
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
									className={`rounded-lg border bg-slate-900/50 p-4 text-sm text-slate-300 transition-colors ${editingFacility?.id === facility.id ? 'border-sky-500/40' : 'border-slate-800'}`}
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
										<div className='flex flex-wrap items-center gap-2'>
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
											{(session.role === 'ADMIN' ||
												facility.ownerId === session.userId) && (
												<Button
													disabled={isSubmitting}
													onClick={() => handleStartEdit(facility)}
													size='sm'
													type='button'
													variant='outline'
												>
													Edit
												</Button>
											)}
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
