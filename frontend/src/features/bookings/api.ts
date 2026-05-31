import {requestJson} from '@/lib/http'
import type {
	BookingRequest,
	BookingResponse,
	EquipmentRentalRequest,
	EquipmentRentalResponse,
	PaymentMethod,
	PaymentResponse,
	ReviewedEntityType,
	ReviewRequest,
	ReviewResponse
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

export function listConflictingBookings(input: {
	end: string
	facilityId: number
	start: string
}) {
	const searchParams = new URLSearchParams({
		end: input.end,
		start: input.start
	})

	return requestJson<BookingResponse[]>(
		`/api/bookings/facility/${input.facilityId}/conflicting?${searchParams.toString()}`
	)
}

export function listUserRentals(userId: number) {
	return requestJson<EquipmentRentalResponse[]>(`/api/rentals/user/${userId}`)
}

export function createRental(
	input: EquipmentRentalRequest & {
		paymentMethod: PaymentMethod
		simulateFailure?: boolean
	}
) {
	const {paymentMethod, simulateFailure = false, ...payload} = input
	const searchParams = new URLSearchParams({
		paymentMethod,
		simulateFailure: String(simulateFailure)
	})

	return requestJson<EquipmentRentalResponse>(
		`/api/rentals/saga?${searchParams.toString()}`,
		{
			body: payload,
			method: 'POST'
		}
	)
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

export function listReviewsForEntity(
	reviewedEntityType: ReviewedEntityType,
	reviewedEntityId: number
) {
	return requestJson<ReviewResponse[]>(
		`/api/reviews/entity/${reviewedEntityType}/${reviewedEntityId}`
	)
}

export function createReview(input: ReviewRequest) {
	return requestJson<ReviewResponse>('/api/reviews', {
		body: {...input},
		method: 'POST'
	})
}
