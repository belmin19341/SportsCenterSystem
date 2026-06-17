import {Link} from 'react-router'
import {useFacilityData} from '@/hooks/useFacilityData'
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
import {Label} from '@/components/ui/label'

const COMMENT_MAX = 1000
import {formatPriceAdjustment} from '@/features/bookings/pricingCopy'
import {formatCurrency, formatDateTime} from '@/lib/format'
import {formatTimeRange} from '@/lib/localDateTime'

function humanizeLabel(value: string) {
	return value.replaceAll('_', ' ').toLowerCase()
}

export function FacilityPage() {
	const {
		numericFacilityId,
		session,
		facilityQuery,
		equipmentQuery,
		pricingRulesQuery,
		reviewsQuery,
		canReview,
		averageRating,
		reviewForm,
		setReviewForm,
		formError,
		reviewMutation,
		handleReviewSubmit
	} = useFacilityData();

	if (!Number.isFinite(numericFacilityId)) {
		return <LoadingOrError error={new Error('Facility ID is not valid.')} />
	}

	if (facilityQuery.isPending) {
		return <LoadingOrError title='Loading facility' />
	}

	if (facilityQuery.isError) {
		return <LoadingOrError error={facilityQuery.error} />
	}

	const facility = facilityQuery.data

	return (
		<div className='space-y-8'>
			<section className='grid gap-6 lg:grid-cols-[1.4fr,0.6fr]'>
				<Card>
					<CardHeader>
						<div className='flex flex-wrap gap-2'>
							<Badge>{humanizeLabel(facility.type)}</Badge>
							<Badge
								variant={facility.status === 'ACTIVE' ? 'success' : 'warning'}
							>
								{facility.status}
							</Badge>
						</div>
						<CardTitle className='text-2xl sm:text-3xl'>
							{facility.name}
						</CardTitle>
						<CardDescription>{facility.description}</CardDescription>
					</CardHeader>
					<CardContent className='grid gap-4 text-sm text-slate-300 sm:grid-cols-3'>
						<div>
							<div className='text-slate-500'>Capacity</div>
							<div className='mt-1 font-medium text-white'>
								{facility.capacity}
							</div>
						</div>
						<div>
							<div className='text-slate-500'>Base price</div>
							<div className='mt-1 font-medium text-white'>
								{formatCurrency(facility.basePricePerHour)}/h
							</div>
						</div>
						<div>
							<div className='text-slate-500'>Hours</div>
							<div className='mt-1 font-medium text-white'>
								{formatTimeRange(
									facility.workingHoursStart,
									facility.workingHoursEnd
								)}
							</div>
						</div>
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Booking</CardTitle>
						<CardDescription>
							Reserve this court when it is active.
						</CardDescription>
					</CardHeader>
					<CardContent className='space-y-4'>
						{averageRating ? (
							<Badge variant='success'>
								{averageRating.toFixed(1)} average rating
							</Badge>
						) : (
							<Badge variant='muted'>No rating yet</Badge>
						)}
						<Link
							className='block w-full'
							to={
								session ? `/bookings/new?facilityId=${facility.id}` : '/login'
							}
						>
							<Button
								className='w-full'
								disabled={facility.status !== 'ACTIVE'}
								type='button'
							>
								{session ? 'Book this facility' : 'Sign in to book'}
							</Button>
						</Link>
					</CardContent>
				</Card>
			</section>

			<section className='grid gap-6 lg:grid-cols-2'>
				<Card>
					<CardHeader>
						<CardTitle>Equipment</CardTitle>
						<CardDescription>
							Inventory linked to this facility.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{equipmentQuery.isPending ? (
							<LoadingOrError title='Loading equipment' />
						) : equipmentQuery.isError ? (
							<LoadingOrError error={equipmentQuery.error} />
						) : equipmentQuery.data.length === 0 ? (
							<div className='text-sm text-slate-400'>No linked equipment.</div>
						) : (
							<div className='space-y-3'>
								{equipmentQuery.data.map(item => (
									<div
										className='rounded-lg border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'
										key={item.id}
									>
										<div className='flex flex-wrap items-center justify-between gap-3'>
											<span className='font-medium text-white'>
												{item.name}
											</span>
											<Badge variant='muted'>
												{item.quantityAvailable}/{item.quantityTotal}
											</Badge>
										</div>
										<div className='mt-2 flex flex-wrap justify-between gap-3'>
											<span>{humanizeLabel(item.type)}</span>
											<span>{formatCurrency(item.pricePerDay)}/day</span>
										</div>
									</div>
								))}
							</div>
						)}
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Pricing rules</CardTitle>
						<CardDescription>
							Time-based changes to the base hourly price.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{pricingRulesQuery.isPending ? (
							<LoadingOrError title='Loading pricing rules' />
						) : pricingRulesQuery.isError ? (
							<LoadingOrError error={pricingRulesQuery.error} />
						) : pricingRulesQuery.data.length === 0 ? (
							<div className='text-sm text-slate-400'>Base price applies.</div>
						) : (
							<div className='space-y-3'>
								{pricingRulesQuery.data.map(rule => (
									<div
										className='rounded-lg border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'
										key={rule.id}
									>
										<div className='flex flex-wrap items-center justify-between gap-3'>
											<span>{rule.dayOfWeek || 'Every day'}</span>
											<Badge>
												{formatPriceAdjustment(rule.priceMultiplier)}
											</Badge>
										</div>
										<div className='mt-2 text-slate-400'>
											{rule.timeSlotStart} - {rule.timeSlotEnd}
										</div>
									</div>
								))}
							</div>
						)}
					</CardContent>
				</Card>
			</section>

			<section className='grid gap-6 lg:grid-cols-[0.8fr,1.2fr]'>
				<Card>
					<CardHeader>
						<CardTitle>Leave a review</CardTitle>
						<CardDescription>
							Reviews are available after completed bookings.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{session ? (
							canReview ? (
								<form className='space-y-4' onSubmit={handleReviewSubmit}>
									<div className='space-y-2'>
										<Label htmlFor='reviewRating'>Rating</Label>
										<select
											className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
											id='reviewRating'
											onChange={event =>
												setReviewForm(current => ({
													...current,
													rating: Number(event.target.value)
												}))
											}
											value={reviewForm.rating}
										>
											{[5, 4, 3, 2, 1].map(rating => (
												<option key={rating} value={rating}>
													{rating}
												</option>
											))}
										</select>
									</div>
									<div className='space-y-2'>
										<div className='flex items-center justify-between'>
											<Label htmlFor='reviewComment'>Comment</Label>
											<span
												className={`text-xs ${reviewForm.comment.length > COMMENT_MAX ? 'text-rose-400' : 'text-slate-500'}`}
											>
												{reviewForm.comment.length}/{COMMENT_MAX}
											</span>
										</div>
										<textarea
											className='min-h-28 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
											id='reviewComment'
											onChange={event =>
												setReviewForm(current => ({
													...current,
													comment: event.target.value
												}))
											}
											value={reviewForm.comment}
										/>
									</div>
									{formError ? (
										<Alert variant='destructive'>{formError}</Alert>
									) : null}
									<Button
										className='w-full sm:w-auto'
										disabled={
											reviewMutation.isPending ||
											reviewForm.comment.length > COMMENT_MAX
										}
										type='submit'
									>
										{reviewMutation.isPending ? 'Saving...' : 'Submit review'}
									</Button>
								</form>
							) : (
								<div className='text-sm text-slate-400'>
									Complete a booking for this facility before reviewing it.
								</div>
							)
						) : (
							<Link className='block w-full sm:w-auto' to='/login'>
								<Button className='w-full sm:w-auto' type='button'>
									Sign in
								</Button>
							</Link>
						)}
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Reviews</CardTitle>
						<CardDescription>
							Recent feedback from completed visits.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{session ? (
							reviewsQuery.isPending ? (
								<LoadingOrError title='Loading reviews' />
							) : reviewsQuery.isError ? (
								<LoadingOrError error={reviewsQuery.error} />
							) : reviewsQuery.data.length === 0 ? (
								<div className='text-sm text-slate-400'>No reviews yet.</div>
							) : (
								<div className='space-y-3'>
									{reviewsQuery.data.map(review => (
										<div
											className='rounded-lg border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'
											key={review.id}
										>
											<div className='flex flex-wrap items-center justify-between gap-3'>
												<Badge variant='success'>{review.rating}/5</Badge>
												<span className='text-xs text-slate-500'>
													{formatDateTime(review.createdAt)}
												</span>
											</div>
											{review.comment ? (
												<div className='mt-3'>{review.comment}</div>
											) : null}
										</div>
									))}
								</div>
							)
						) : (
							<div className='text-sm text-slate-400'>
								Sign in to view reviews.
							</div>
						)}
					</CardContent>
				</Card>
			</section>
		</div>
	)
}
