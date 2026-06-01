import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { useFeedback } from '@/components/feedback';
import { listUserBookings, listUserRentals, listPaymentsForBookings } from '@/features/bookings/api';
import { listFacilities } from '@/features/resources/api';
import { getUser, getUserLoyalty, listUserAchievements, listUserNotifications, markNotificationAsRead } from '@/features/user/api';
import { getErrorMessage } from '@/lib/format';
import type { BookingStatus } from '@/types/api';

export function useDashboardData() {
	const { session } = useAuth();
	const { showFeedback } = useFeedback();
	const queryClient = useQueryClient();
	const userId = session?.userId ?? 0;

	const [bookingFilter, setBookingFilter] = useState<BookingStatus | 'ALL'>('ALL');
	const [notificationFilter, setNotificationFilter] = useState<'ALL' | 'READ' | 'UNREAD'>('ALL');

	const facilitiesQuery = useQuery({
		queryFn: () => listFacilities(),
		queryKey: ['facilities']
	});

	const userQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => getUser(userId),
		queryKey: ['user', userId]
	});

	const loyaltyQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => getUserLoyalty(userId),
		queryKey: ['loyalty', userId]
	});

	const achievementsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserAchievements(userId),
		queryKey: ['achievements', userId]
	});

	const bookingsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserBookings(userId),
		queryKey: ['bookings', userId]
	});

	const rentalsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserRentals(userId),
		queryKey: ['rentals', userId]
	});

	const notificationsQuery = useQuery({
		enabled: Boolean(session),
		queryFn: () => listUserNotifications(userId),
		queryKey: ['notifications', userId]
	});

	const paymentsQuery = useQuery({
		enabled: Boolean(session) && (bookingsQuery.data?.length ?? 0) > 0,
		queryFn: () => listPaymentsForBookings((bookingsQuery.data || []).map((b: any) => b.id)),
		queryKey: ['payments', userId, JSON.stringify((bookingsQuery.data || []).map((b: any) => b.id))]
	});

	const markReadMutation = useMutation({
		mutationFn: (id: number) => markNotificationAsRead(id),
		onError: (error: any) => showFeedback({ description: getErrorMessage(error), title: 'Notification update failed', variant: 'destructive' }),
		onSuccess: () => {
			showFeedback({ description: 'The notification was marked as read.', title: 'Notification updated', variant: 'success' });
			return queryClient.invalidateQueries({ queryKey: ['notifications', userId] });
		}
	});

	const facilityNameById = useMemo(() => 
		new Map((facilitiesQuery.data || []).map((f: any) => [f.id, f.name])), 
	[facilitiesQuery.data]);

	const visibleBookings = useMemo(() => 
		(bookingsQuery.data || []).filter((b: any) => bookingFilter === 'ALL' || b.status === bookingFilter),
	[bookingFilter, bookingsQuery.data]);

	const visibleNotifications = useMemo(() => 
		(notificationsQuery.data || []).filter((n: any) => {
			if (notificationFilter === 'READ') return n.isRead;
			if (notificationFilter === 'UNREAD') return !n.isRead;
			return true;
		}),
	[notificationFilter, notificationsQuery.data]);

	return {
		session,
		userQuery,
		loyaltyQuery,
		achievementsQuery,
		bookingsQuery,
		rentalsQuery,
		notificationsQuery,
		paymentsQuery,
		markReadMutation,
		filters: { bookingFilter, setBookingFilter, notificationFilter, setNotificationFilter },
		derived: { facilityNameById, visibleBookings, visibleNotifications }
	};
}