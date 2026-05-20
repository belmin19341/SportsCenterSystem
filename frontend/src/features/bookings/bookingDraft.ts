import type {PaymentMethod} from '@/types/api'

export interface BookingDraft {
	endTime: string
	facilityId: string
	paymentMethod: PaymentMethod
	startTime: string
}

const bookingDraftStorageKey = 'sportscenter-booking-draft'
const defaultPaymentMethod: PaymentMethod = 'CREDIT_CARD'
const paymentMethods = [
	'CREDIT_CARD',
	'DEBIT_CARD',
	'PAYPAL'
] satisfies PaymentMethod[]

function isPaymentMethod(value: unknown): value is PaymentMethod {
	return (
		typeof value === 'string' && paymentMethods.includes(value as PaymentMethod)
	)
}

export function readBookingDraft() {
	try {
		const storedDraft = sessionStorage.getItem(bookingDraftStorageKey)
		if (!storedDraft) {
			return null
		}

		const parsedDraft = JSON.parse(storedDraft) as Partial<BookingDraft>
		return {
			endTime:
				typeof parsedDraft.endTime === 'string' ? parsedDraft.endTime : '',
			facilityId:
				typeof parsedDraft.facilityId === 'string'
					? parsedDraft.facilityId
					: '',
			paymentMethod: isPaymentMethod(parsedDraft.paymentMethod)
				? parsedDraft.paymentMethod
				: defaultPaymentMethod,
			startTime:
				typeof parsedDraft.startTime === 'string' ? parsedDraft.startTime : ''
		} satisfies BookingDraft
	} catch {
		return null
	}
}

export function saveBookingDraft(draft: BookingDraft) {
	try {
		const hasDraftContent = Boolean(
			draft.facilityId ||
				draft.startTime ||
				draft.endTime ||
				draft.paymentMethod !== defaultPaymentMethod
		)

		if (!hasDraftContent) {
			sessionStorage.removeItem(bookingDraftStorageKey)
			return
		}

		sessionStorage.setItem(bookingDraftStorageKey, JSON.stringify(draft))
	} catch (error) {
		void error
	}
}

export function clearBookingDraft() {
	try {
		sessionStorage.removeItem(bookingDraftStorageKey)
	} catch (error) {
		void error
	}
}

export function createInitialBookingDraft(
	requestedFacilityId: string
): BookingDraft {
	const storedDraft = readBookingDraft()
	return {
		endTime: storedDraft?.endTime ?? '',
		facilityId: requestedFacilityId || storedDraft?.facilityId || '',
		paymentMethod: storedDraft?.paymentMethod ?? defaultPaymentMethod,
		startTime: storedDraft?.startTime ?? ''
	}
}
