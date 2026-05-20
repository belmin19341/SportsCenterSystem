import {requestJson} from '@/lib/http'
import type {
	BookingRequest,
	BookingResponse,
	EquipmentRentalResponse,
	PaymentResponse,
	PaymentMethod
} from '@/types/api'

export function listUserBookings(userId: number) {
	return requestJson<BookingResponse[]>(`/api/bookings/user/${userId}`)
}

export function createBooking(
	input: BookingRequest & {
		paymentMethod: PaymentMethod
	}
) {
	const {paymentMethod, ...payload} = input
	const searchParams = new URLSearchParams({paymentMethod})

	return requestJson<BookingResponse>(
		`/api/bookings/orchestrated?${searchParams.toString()}`,
		{
			body: payload,
			method: 'POST'
		}
	)
}

export function listUserRentals(userId: number) {
	return requestJson<EquipmentRentalResponse[]>(`/api/rentals/user/${userId}`)
}

export async function listPaymentsForBookings(bookingIds: number[]) {
	const uniqueBookingIds = Array.from(new Set(bookingIds))
	const paymentGroups = await Promise.all(
		uniqueBookingIds.map(bookingId =>
			requestJson<PaymentResponse[]>(`/api/payments/booking/${bookingId}`)
		)
	)

	return paymentGroups.flat()
}

