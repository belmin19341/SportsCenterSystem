export type Role = 'USER' | 'OWNER' | 'ADMIN'
export type BookingStatus =
	| 'PENDING'
	| 'CONFIRMED'
	| 'COMPLETED'
	| 'CANCELLED'
	| 'TEMPORARY_HOLD'

export type FacilityStatus = 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE'
export type FacilityType =
	| 'FOOTBALL_5V5'
	| 'FOOTBALL_7V7'
	| 'PADEL'
	| 'TENNIS'
	| 'TABLE_TENNIS'

export type PaymentMethod = 'CREDIT_CARD' | 'DEBIT_CARD' | 'PAYPAL' | 'CASH'
export type PaymentStatus = 'FAILED' | 'PAID' | 'PENDING' | 'REFUNDED'
export type EquipmentType =
	| 'RACKET'
	| 'BALL'
	| 'FITNESS'
	| 'SKI'
	| 'BICYCLE'
	| 'OTHER'
export type EquipmentCondition = 'NEW' | 'GOOD' | 'FAIR' | 'POOR'
export type ReviewedEntityType = 'FACILITY' | 'EQUIPMENT'
export type DayOfWeekName =
	| 'FRIDAY'
	| 'HOLIDAY'
	| 'MONDAY'
	| 'SATURDAY'
	| 'SUNDAY'
	| 'THURSDAY'
	| 'TUESDAY'
	| 'WEDNESDAY'

export interface PageResponse<T> {
	content: T[]
	empty: boolean
	first: boolean
	last: boolean
	number: number
	numberOfElements: number
	size: number
	totalElements: number
	totalPages: number
}

export interface AuthResponse {
	access_token: string
	refresh_token: string
	token_type: string
	userId: number
	username: string
	email: string
	role: Role
	expires_in: number
	refresh_expires_in?: number
}

export interface StoredSession {
	accessToken: string
	refreshToken: string
	tokenType: string
	userId: number
	username: string
	email: string
	role: Role
	accessTokenExpiresAt: number
	refreshTokenExpiresAt: number | null
}

export interface ApiErrorPayload {
	details?: string[]
	error?: string
	message?: string
	status?: number
}

export interface FacilityResponse {
	basePricePerHour: number
	capacity: number
	createdAt: string
	description: string
	id: number
	imageUrl: string | null
	name: string
	ownerId: number
	status: FacilityStatus
	type: FacilityType
	workingHoursEnd: string
	workingHoursStart: string
}

export interface FacilityRequest {
	basePricePerHour: number
	capacity: number
	description: string | null
	imageUrl: string | null
	name: string
	ownerId: number
	status: FacilityStatus
	type: FacilityType
	workingHoursEnd: string
	workingHoursStart: string
}

export interface EquipmentResponse {
	category: string
	createdAt: string
	depositRequired: number
	equipmentCondition: 'NEW' | 'GOOD' | 'FAIR' | 'POOR'
	facilityId: number | null
	facilityName: string | null
	id: number
	lastMaintenance: string | null
	name: string
	pricePerDay: number
	quantityAvailable: number
	quantityTotal: number
	rentalCount: number
	type: EquipmentType
}

export interface PricingRuleResponse {
	dayOfWeek: DayOfWeekName | null
	description: string | null
	facilityId: number
	facilityName: string
	id: number
	priceMultiplier: number
	timeSlotEnd: string
	timeSlotStart: string
}

export interface PriceQuote {
	basePricePerHour?: number
	end?: string
	facilityId: number
	hours: number
	multiplier: number
	start?: string
	totalPrice: number
}

export interface UserResponse {
	createdAt: string
	email: string
	id: number
	phone: string | null
	role: Role
	username: string
}

export interface UserLoyaltyResponse {
	id: number
	tier: 'BRONZE' | 'SILVER' | 'GOLD'
	tierAchievedAt: string
	totalPoints: number
	updatedAt: string
	userId: number
	username: string
}

export interface UserAchievementResponse {
	achievementId: number
	achievementName: string
	badgeIcon: string | null
	id: number
	unlockedAt: string
	userId: number
	username: string
}

export interface SavedCardResponse {
	id: number
	last4: string
	brand: string
	createdAt: string
}

export interface BookingRequest {
	endTime: string
	facilityId: number
	isRecurring: boolean
	recurringPattern: string | null
	startTime: string
	status: BookingStatus
	totalPrice: number
	userId: number
	// Payment fields
	paymentMethod?: PaymentMethod
	stripeToken?: string
	savedCardId?: number
	saveCard?: boolean
	cardLast4?: string
	cardBrand?: string
}

export interface BookingResponse {
	createdAt: string
	endTime: string
	facilityId: number
	id: number
	isRecurring: boolean
	recurringPattern: string | null
	startTime: string
	status: BookingStatus
	totalPrice: number
	userId: number
}

export interface EquipmentRentalResponse {
	bookingId: number | null
	conditionOnReturn: string | null
	createdAt: string
	depositPaid: number
	endDate: string
	equipmentId: number
	id: number
	quantity: number
	startDate: string
	status: string
	totalPrice: number
	userId: number
}

export interface EquipmentRentalRequest {
	bookingId: number | null
	depositPaid: number
	endDate: string
	equipmentId: number
	quantity: number
	startDate: string
	status: string
	totalPrice: number
	userId: number
}

export interface PaymentResponse {
	amount: number
	bookingId: number | null
	createdAt: string
	depositAmount: number
	id: number
	paidAt: string | null
	paymentMethod: PaymentMethod
	rentalId: number | null
	status: PaymentStatus | string
	transactionId: string
}

export interface NotificationResponse {
	id: number
	isRead: boolean
	message: string
	sentAt: string
	subject: string
	type: string
	userId: number
}

export interface ReviewRequest {
	comment: string | null
	rating: number
	reviewedEntityId: number
	reviewedEntityType: ReviewedEntityType
	reviewerId: number
}

export interface ReviewResponse {
	comment: string | null
	createdAt: string
	id: number
	rating: number
	reviewedEntityId: number
	reviewedEntityType: ReviewedEntityType
	reviewerId: number
}
