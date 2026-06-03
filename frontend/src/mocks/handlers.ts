import {HttpResponse, http} from 'msw'

const facilities = [
	{
		basePricePerHour: 45,
		capacity: 10,
		createdAt: '2026-05-01T10:00:00',
		description: 'Covered padel court with premium lighting.',
		id: 1,
		imageUrl: null,
		name: 'Padel Arena Alpha',
		ownerId: 2,
		status: 'ACTIVE',
		type: 'PADEL',
		workingHoursEnd: '23:00:00',
		workingHoursStart: '08:00:00'
	},
	{
		basePricePerHour: 60,
		capacity: 10,
		createdAt: '2026-05-01T10:00:00',
		description: 'Outdoor football pitch for evening sessions.',
		id: 2,
		imageUrl: null,
		name: 'Small Field A',
		ownerId: 2,
		status: 'ACTIVE',
		type: 'FOOTBALL_5V5',
		workingHoursEnd: '22:00:00',
		workingHoursStart: '09:00:00'
	}
]

const equipment = [
	{
		category: 'Rackets',
		createdAt: '2026-05-01T10:00:00',
		depositRequired: 20,
		equipmentCondition: 'GOOD',
		facilityId: 1,
		facilityName: 'Padel Arena Alpha',
		id: 10,
		lastMaintenance: '2026-05-08',
		name: 'Carbon Racket Set',
		pricePerDay: 12,
		quantityAvailable: 4,
		quantityTotal: 6,
		rentalCount: 18,
		type: 'RACKET'
	}
]

const user = {
	createdAt: '2026-05-01T10:00:00',
	email: 'john@example.com',
	id: 1,
	phone: '+38761111222',
	role: 'USER',
	username: 'john_doe'
}

const authResponse = {
	access_token: 'access-token',
	email: user.email,
	expires_in: 900,
	refresh_expires_in: 604_800,
	refresh_token: 'refresh-token',
	role: user.role,
	token_type: 'Bearer',
	userId: user.id,
	username: user.username
}

const bookings = [
	{
		createdAt: '2026-05-10T10:00:00',
		endTime: '2026-05-25T11:00:00',
		facilityId: 1,
		id: 101,
		isRecurring: false,
		recurringPattern: null,
		startTime: '2026-05-25T10:00:00',
		status: 'COMPLETED',
		totalPrice: 45,
		userId: 1
	}
]

const notifications = [
	{
		id: 1,
		isRead: false,
		message: 'Your booking was confirmed.',
		sentAt: '2026-05-10T11:00:00',
		subject: 'Booking confirmed',
		type: 'BOOKING_CONFIRMATION',
		userId: 1
	}
]

const reviews = [
	{
		comment: 'Great lighting and clean surface.',
		createdAt: '2026-05-11T11:00:00',
		id: 30,
		rating: 5,
		reviewedEntityId: 1,
		reviewedEntityType: 'FACILITY',
		reviewerId: 1
	}
]

export const handlers = [
	http.post('*/api/auth/login', async ({request}) => {
		const body = (await request.json()) as {
			password?: string
			username?: string
		}
		if (body.username === 'john_doe' && body.password === 'password123') {
			return HttpResponse.json(authResponse)
		}

		return HttpResponse.json(
			{error: 'Unauthorized', message: 'Bad credentials', status: 401},
			{status: 401}
		)
	}),
	http.post('*/api/auth/logout', () =>
		HttpResponse.json({logout: true, message: 'Tokens revoked.'})
	),
	http.get('*/api/facilities', ({request}) => {
		const url = new URL(request.url)
		const type = url.searchParams.get('type')
		const status = url.searchParams.get('status')
		const q = url.searchParams.get('q')?.toLowerCase()
		const filteredFacilities = facilities.filter(facility => {
			const matchesType = !type || facility.type === type
			const matchesStatus = !status || facility.status === status
			const matchesQuery =
				!q ||
				facility.name.toLowerCase().includes(q) ||
				facility.description.toLowerCase().includes(q)

			return matchesType && matchesStatus && matchesQuery
		})

		return HttpResponse.json(filteredFacilities)
	}),
	http.get('*/api/facilities/:facilityId', ({params}) => {
		const facility = facilities.find(
			item => item.id === Number(params.facilityId)
		)

		return facility
			? HttpResponse.json(facility)
			: HttpResponse.json({message: 'Facility not found'}, {status: 404})
	}),
	http.get('*/api/facilities/owner/:ownerId', () =>
		HttpResponse.json(facilities)
	),
	http.post('*/api/facilities', async ({request}) => {
		const body = (await request.json()) as object

		return HttpResponse.json(
			{...body, createdAt: '2026-05-20T10:00:00', id: 20},
			{status: 201}
		)
	}),
	http.get('*/api/equipment', () => HttpResponse.json(equipment)),
	http.get('*/api/equipment/facility/:facilityId', ({params}) =>
		HttpResponse.json(
			equipment.filter(item => item.facilityId === Number(params.facilityId))
		)
	),
	http.get('*/api/pricing-rules/facility/:facilityId', ({params}) =>
		HttpResponse.json([
			{
				dayOfWeek: 'SATURDAY',
				description: 'Weekend premium',
				facilityId: Number(params.facilityId),
				facilityName: 'Padel Arena Alpha',
				id: 1,
				priceMultiplier: 1.2,
				timeSlotEnd: '22:00:00',
				timeSlotStart: '18:00:00'
			}
		])
	),
	http.get('*/api/pricing-rules/calculate', () =>
		HttpResponse.json({facilityId: 1, hours: 1, multiplier: 1, totalPrice: 45})
	),
	http.get('*/api/users/:userId', () => HttpResponse.json(user)),
	http.get('*/api/loyalty/user/:userId', () =>
		HttpResponse.json({
			id: 1,
			tier: 'SILVER',
			tierAchievedAt: '2026-05-01T10:00:00',
			totalPoints: 750,
			updatedAt: '2026-05-10T10:00:00',
			userId: 1,
			username: 'john_doe'
		})
	),
	http.get('*/api/user-achievements/user/:userId', () =>
		HttpResponse.json([
			{
				achievementId: 1,
				achievementName: 'Prva rezervacija',
				badgeIcon: null,
				id: 1,
				unlockedAt: '2026-05-10T10:00:00',
				userId: 1,
				username: 'john_doe'
			}
		])
	),
	http.get('*/api/bookings/user/:userId', () => HttpResponse.json(bookings)),
	http.get('*/api/bookings/facility/:facilityId/conflicting', () =>
		HttpResponse.json([])
	),
	http.post('*/api/bookings/orchestrated', async ({request}) => {
		const body = (await request.json()) as object

		return HttpResponse.json(
			{...body, createdAt: '2026-05-20T10:00:00', id: 200},
			{status: 201}
		)
	}),
	http.get('*/api/rentals/user/:userId', () => HttpResponse.json([])),
	http.get('*/api/payments/booking/:bookingId', ({params}) =>
		HttpResponse.json([
			{
				amount: 45,
				bookingId: Number(params.bookingId),
				createdAt: '2026-05-10T10:00:00',
				depositAmount: 0,
				id: 50,
				paidAt: '2026-05-10T10:00:00',
				paymentMethod: 'CREDIT_CARD',
				rentalId: null,
				status: 'PAID',
				transactionId: 'txn_1'
			}
		])
	),
	http.get('*/api/notifications/user/:userId', () =>
		HttpResponse.json(notifications)
	),
	http.patch('*/api/notifications/:notificationId/read', ({params}) =>
		HttpResponse.json({
			...notifications[0],
			id: Number(params.notificationId),
			isRead: true
		})
	),
	http.get('*/api/reviews/entity/:type/:entityId', () =>
		HttpResponse.json(reviews)
	),
	http.post('*/api/reviews', async ({request}) => {
		const body = (await request.json()) as object

		return HttpResponse.json(
			{...body, createdAt: '2026-05-20T10:00:00', id: 31},
			{status: 201}
		)
	})
]
