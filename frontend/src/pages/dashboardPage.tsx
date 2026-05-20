import {useMutation, useQuery, useQueryClient} from '@tanstack/react-query'
import {useMemo, useState} from 'react'
import {useAuth} from '@/auth/authContext'
import {useFeedback} from '@/components/feedback'
import {LoadingOrError} from '@/components/loadingOrError'
import {Badge} from '@/components/ui/badge'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'
import {
	listPaymentsForBookings,
	listUserBookings,
	listUserRentals
} from '@/features/bookings/api'
import {listFacilities} from '@/features/resources/api'
import {
	getUser,
	getUserLoyalty,
	listUserAchievements,
	listUserNotifications,
	markNotificationAsRead
} from '@/features/user/api'
import {
	formatCurrency,
	formatDate,
	formatDateTime,
	getErrorMessage
} from '@/lib/format'
import type {BookingStatus} from '@/types/api'

function getFacilityName(
	facilityNameById: Map<number, string>,
	facilityId: number
) {
	return facilityNameById.get(facilityId) || `Facility #${facilityId}`
}

export function DashboardPage() {
	const {session} = useAuth()
	const {showFeedback} = useFeedback()
	const queryClient = useQueryClient()
	const userId = session?.userId ?? 0
	const [bookingFilter, setBookingFilter] = useState<BookingStatus | 'ALL'>(
		'ALL'
	)
	const [notificationFilter, setNotificationFilter] = useState<
		'ALL' | 'READ' | 'UNREAD'
	>('ALL')

	const facilitiesQuery = useQuery({
		queryFn: () => listFacilities(),
		queryKey: ['facilities']
	})
	const userQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => getUser(userId),
		queryKey: ['user', userId]
	})
	const loyaltyQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => getUserLoyalty(userId),
		queryKey: ['loyalty', userId]
	})
	const achievementsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserAchievements(userId),
		queryKey: ['achievements', userId]
	})
	const bookingsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserBookings(userId),
		queryKey: ['bookings', userId]
	})
	const rentalsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserRentals(userId),
		queryKey: ['rentals', userId]
	})
	const notificationsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserNotifications(userId),
		queryKey: ['notifications', userId]
	})
	const paymentsQuery = useQuery({
		enabled: Boolean(session) && (bookingsQuery.data?.length ?? 0) > 0,
		queryFn: () =>
			listPaymentsForBookings(
				(bookingsQuery.data || []).map(booking => booking.id)
			),
		queryKey: [
			'payments',
			userId,
			(bookingsQuery.data || []).map(booking => booking.id).join(',')
		]
	})

	const markReadMutation = useMutation({
		mutationFn: (notificationId: number) =>
			markNotificationAsRead(notificationId),
		onError(error) {
			showFeedback({
				description: getErrorMessage(error),
				title: 'Notification update failed',
				variant: 'destructive'
			})
		},
		onSuccess() {
			showFeedback({
				description: 'The notification was marked as read.',
				title: 'Notification updated',
				variant: 'success'
			})
			return queryClient.invalidateQueries({
				queryKey: ['notifications', userId]
			})
		}
	})

	const facilityNameById = useMemo(
		() =>
			new Map(
				(facilitiesQuery.data || []).map(facility => [
					facility.id,
					facility.name
				])
			),
		[facilitiesQuery.data]
	)
	const visibleBookings = useMemo(
		() =>
			(bookingsQuery.data || []).filter(
				booking => bookingFilter === 'ALL' || booking.status === bookingFilter
			),
		[bookingFilter, bookingsQuery.data]
	)
	const visibleNotifications = useMemo(
		() =>
			(notificationsQuery.data || []).filter(notification => {
				if (notificationFilter === 'READ') {
					return notification.isRead
				}

				if (notificationFilter === 'UNREAD') {
					return !notification.isRead
				}

				return true
			}),
		[notificationFilter, notificationsQuery.data]
	)

	if (!session) {
		return null
	}

	return (
		<div className='space-y-6 sm:space-y-8'>
			<section className='grid gap-6 lg:grid-cols-2'>
				<Card>
					<CardHeader>
						<CardTitle>Profile</CardTitle>
						<CardDescription>Your account details.</CardDescription>
					</CardHeader>
					<CardContent>
						{userQuery.isPending ? (
							<LoadingOrError title='Loading profile' />
						) : userQuery.isError ? (
							<LoadingOrError error={userQuery.error} />
						) : (
							<div className='space-y-3 text-sm text-slate-300'>
								<div className='flex flex-wrap items-center justify-between gap-2'>
									<span>Username</span>
									<span className='break-all text-right'>
										{userQuery.data.username}
									</span>
								</div>
								<div className='flex flex-wrap items-center justify-between gap-2'>
									<span>Email</span>
									<span className='break-all text-right'>
										{userQuery.data.email}
									</span>
								</div>
								<div className='flex flex-wrap items-center justify-between gap-2'>
									<span>Role</span>
									<Badge>{userQuery.data.role}</Badge>
								</div>
								<div className='flex flex-wrap items-center justify-between gap-2'>
									<span>Phone</span>
									<span className='break-all text-right'>
										{userQuery.data.phone || '—'}
									</span>
								</div>
							</div>
						)}
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Loyalty</CardTitle>
						<CardDescription>
							Current points and tier status for the signed-in user.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{loyaltyQuery.isPending ? (
							<LoadingOrError title='Loading loyalty data' />
						) : loyaltyQuery.isError ? (
							<LoadingOrError error={loyaltyQuery.error} />
						) : (
							<div className='space-y-4'>
								<div className='flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
									<div>
										<div className='text-sm text-slate-400'>Tier</div>
										<div className='mt-1 text-2xl font-semibold text-white'>
											{loyaltyQuery.data.tier}
										</div>
									</div>
									<Badge variant='success'>
										{loyaltyQuery.data.totalPoints} pts
									</Badge>
								</div>
								<div className='text-sm text-slate-400'>
									Last updated {formatDateTime(loyaltyQuery.data.updatedAt)}
								</div>
							</div>
						)}
					</CardContent>
				</Card>
			</section>

			<section className='grid gap-6 xl:grid-cols-[1.3fr,0.7fr]'>
				<Card>
					<CardHeader>
						<div className='flex flex-col items-start gap-4 sm:flex-row sm:justify-between'>
							<div>
								<CardTitle>Bookings</CardTitle>
								<CardDescription>Your booking history.</CardDescription>
							</div>
							<select
								aria-label='Filter bookings by status'
								className='h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 sm:w-auto'
								onChange={event =>
									setBookingFilter(event.target.value as BookingStatus | 'ALL')
								}
								value={bookingFilter}
							>
								<option value='ALL'>All statuses</option>
								<option value='PENDING'>Pending</option>
								<option value='CONFIRMED'>Confirmed</option>
								<option value='COMPLETED'>Completed</option>
								<option value='CANCELLED'>Cancelled</option>
							</select>
						</div>
					</CardHeader>
					<CardContent>
						{bookingsQuery.isPending ? (
							<LoadingOrError title='Loading bookings' />
						) : bookingsQuery.isError ? (
							<LoadingOrError error={bookingsQuery.error} />
						) : visibleBookings.length === 0 ? (
							<div className='text-sm text-slate-400'>
								No bookings match the selected filter.
							</div>
						) : (
							<div className='space-y-4'>
								{visibleBookings.map(booking => (
									<div
										className='rounded-xl border border-slate-800 bg-slate-900/50 p-4'
										key={booking.id}
									>
										<div className='flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
											<div className='min-w-0'>
												<div className='font-medium text-white'>
													{getFacilityName(
														facilityNameById,
														booking.facilityId
													)}
												</div>
												<div className='text-sm text-slate-400'>
													{formatDateTime(booking.startTime)} -{' '}
													{formatDateTime(booking.endTime)}
												</div>
											</div>
											<div className='flex flex-wrap items-center gap-2'>
												<Badge variant='muted'>{booking.status}</Badge>
												<Badge>{formatCurrency(booking.totalPrice)}</Badge>
											</div>
										</div>
									</div>
								))}
							</div>
						)}
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Achievements</CardTitle>
						<CardDescription>
							Badges currently assigned to the user.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{achievementsQuery.isPending ? (
							<LoadingOrError title='Loading achievements' />
						) : achievementsQuery.isError ? (
							<LoadingOrError error={achievementsQuery.error} />
						) : achievementsQuery.data.length === 0 ? (
							<div className='text-sm text-slate-400'>
								No achievements unlocked yet.
							</div>
						) : (
							<div className='flex flex-wrap gap-2'>
								{achievementsQuery.data.map(achievement => (
									<Badge key={achievement.id} variant='success'>
										{achievement.achievementName}
									</Badge>
								))}
							</div>
						)}
					</CardContent>
				</Card>
			</section>

			<section className='grid gap-6 lg:grid-cols-2'>
				<Card>
					<CardHeader>
						<CardTitle>Rentals</CardTitle>
						<CardDescription>Your equipment rental history.</CardDescription>
					</CardHeader>
					<CardContent>
						{rentalsQuery.isPending ? (
							<LoadingOrError title='Loading rentals' />
						) : rentalsQuery.isError ? (
							<LoadingOrError error={rentalsQuery.error} />
						) : rentalsQuery.data.length === 0 ? (
							<div className='text-sm text-slate-400'>
								No equipment rentals found.
							</div>
						) : (
							<div className='space-y-3'>
								{rentalsQuery.data.map(rental => (
									<div
										className='rounded-xl border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'
										key={rental.id}
									>
										<div className='flex flex-wrap items-center justify-between gap-2'>
											<span>Equipment #{rental.equipmentId}</span>
											<Badge variant='muted'>{rental.status}</Badge>
										</div>
										<div className='mt-2 flex flex-wrap items-center justify-between gap-2'>
											<span>
												{formatDate(rental.startDate)} -{' '}
												{formatDate(rental.endDate)}
											</span>
											<span>{formatCurrency(rental.totalPrice)}</span>
										</div>
									</div>
								))}
							</div>
						)}
					</CardContent>
				</Card>

				<Card>
					<CardHeader>
						<CardTitle>Payments</CardTitle>
						<CardDescription>
							Payment status for your recent bookings.
						</CardDescription>
					</CardHeader>
					<CardContent>
						{!bookingsQuery.data || bookingsQuery.data.length === 0 ? (
							<div className='text-sm text-slate-400'>
								No payment records yet.
							</div>
						) : paymentsQuery.isPending ? (
							<LoadingOrError title='Loading payments' />
						) : paymentsQuery.isError ? (
							<LoadingOrError error={paymentsQuery.error} />
						) : !paymentsQuery.data || paymentsQuery.data.length === 0 ? (
							<div className='text-sm text-slate-400'>
								No payment records yet.
							</div>
						) : (
							<div className='space-y-3'>
								{paymentsQuery.data.map(payment => (
									<div
										className='rounded-xl border border-slate-800 bg-slate-900/50 p-4 text-sm text-slate-300'
										key={payment.id}
									>
										<div className='flex flex-wrap items-center justify-between gap-2'>
											<span>Booking #{payment.bookingId}</span>
											<Badge variant='success'>{payment.status}</Badge>
										</div>
										<div className='mt-2 flex flex-wrap items-center justify-between gap-2'>
											<span>{payment.paymentMethod}</span>
											<span>{formatCurrency(payment.amount)}</span>
										</div>
									</div>
								))}
							</div>
						)}
					</CardContent>
				</Card>
			</section>

			<section>
				<Card>
					<CardHeader>
						<div className='flex flex-col items-start gap-4 sm:flex-row sm:justify-between'>
							<div>
								<CardTitle>Notifications</CardTitle>
								<CardDescription>Recent account updates.</CardDescription>
							</div>
							<select
								aria-label='Filter notifications'
								className='h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 sm:w-auto'
								onChange={event =>
									setNotificationFilter(
										event.target.value as 'ALL' | 'READ' | 'UNREAD'
									)
								}
								value={notificationFilter}
							>
								<option value='ALL'>All</option>
								<option value='UNREAD'>Unread</option>
								<option value='READ'>Read</option>
							</select>
						</div>
					</CardHeader>
					<CardContent>
						{notificationsQuery.isPending ? (
							<LoadingOrError title='Loading notifications' />
						) : notificationsQuery.isError ? (
							<LoadingOrError error={notificationsQuery.error} />
						) : visibleNotifications.length === 0 ? (
							<div className='text-sm text-slate-400'>
								No notifications match the selected filter.
							</div>
						) : (
							<div className='space-y-3'>
								{visibleNotifications.slice(0, 6).map(notification => (
									<div
										className='rounded-xl border border-slate-800 bg-slate-900/50 p-4'
										key={notification.id}
									>
										<div className='flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between'>
											<div className='min-w-0'>
												<div className='font-medium text-white'>
													{notification.subject}
												</div>
												<div className='ssc-text-wrap mt-1 text-sm text-slate-400'>
													{notification.message}
												</div>
												<div className='mt-2 text-xs text-slate-500'>
													{formatDateTime(notification.sentAt)}
												</div>
											</div>
											<div className='flex flex-wrap items-center gap-2'>
												<Badge
													variant={notification.isRead ? 'muted' : 'warning'}
												>
													{notification.isRead ? 'Read' : 'Unread'}
												</Badge>
												{notification.isRead ? null : (
													<Button
														disabled={markReadMutation.isPending}
														onClick={() => {
															markReadMutation.mutate(notification.id)
														}}
														size='sm'
														variant='outline'
													>
														Mark read
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
			</section>
		</div>
	)
}
