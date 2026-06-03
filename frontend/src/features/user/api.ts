import {requestJson} from '@/lib/http'
import type {
	NotificationResponse,
	UserAchievementResponse,
	UserLoyaltyResponse,
	UserResponse
} from '@/types/api'

export function register(payload: {
	email: string
	password: string
	role: string
	username: string
}) {
	return requestJson<void>('/api/auth/register', {
		auth: false,
		body: payload,
		method: 'POST'
	})
}

export function getUser(userId: number) {
	return requestJson<UserResponse>(`/api/users/${userId}`)
}

export function getUserLoyalty(userId: number) {
	return requestJson<UserLoyaltyResponse>(`/api/loyalty/user/${userId}`)
}

export function listUserAchievements(userId: number) {
	return requestJson<UserAchievementResponse[]>(
		`/api/user-achievements/user/${userId}`
	)
}

export function listUserNotifications(userId: number) {
	return requestJson<NotificationResponse[]>(
		`/api/notifications/user/${userId}`
	)
}

export function listUnreadUserNotifications(userId: number) {
	return requestJson<NotificationResponse[]>(
		`/api/notifications/user/${userId}/unread`
	)
}

export function markNotificationAsRead(notificationId: number) {
	return requestJson<NotificationResponse>(
		`/api/notifications/${notificationId}/read`,
		{
			method: 'PATCH'
		}
	)
}
