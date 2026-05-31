import {requestJson} from '@/lib/http'
import type {
	EquipmentResponse,
	FacilityRequest,
	FacilityResponse,
	FacilityStatus,
	FacilityType,
	PageResponse,
	PriceQuote,
	PricingRuleResponse
} from '@/types/api'

interface FacilityFilters {
	q?: string
	status?: FacilityStatus | ''
	type?: FacilityType | ''
}

function normalizeList<T>(response: PageResponse<T> | T[]) {
	return Array.isArray(response) ? response : response.content
}

function buildSearchParams(values: Record<string, string | undefined>) {
	const searchParams = new URLSearchParams()

	for (const [key, value] of Object.entries(values)) {
		if (value) {
			searchParams.set(key, value)
		}
	}

	return searchParams
}

export async function listFacilities(filters: FacilityFilters = {}) {
	const searchParams = buildSearchParams({
		q: filters.q?.trim(),
		status: filters.status || undefined,
		type: filters.type || undefined
	})
	const path =
		searchParams.size > 0
			? `/api/facilities?${searchParams.toString()}`
			: '/api/facilities'
	const response = await requestJson<
		FacilityResponse[] | PageResponse<FacilityResponse>
	>(path)

	return normalizeList(response)
}

export function getFacility(facilityId: number) {
	return requestJson<FacilityResponse>(`/api/facilities/${facilityId}`)
}

export function listOwnerFacilities(ownerId: number) {
	return requestJson<FacilityResponse[]>(`/api/facilities/owner/${ownerId}`)
}

export function createFacility(input: FacilityRequest) {
	return requestJson<FacilityResponse>('/api/facilities', {
		body: {...input},
		method: 'POST'
	})
}

export function updateFacility(facilityId: number, input: FacilityRequest) {
	return requestJson<FacilityResponse>(`/api/facilities/${facilityId}`, {
		body: {...input},
		method: 'PUT'
	})
}

export function listEquipment() {
	return requestJson<EquipmentResponse[]>('/api/equipment')
}

export function listEquipmentByFacility(facilityId: number) {
	return requestJson<EquipmentResponse[]>(
		`/api/equipment/facility/${facilityId}`
	)
}

export function listPricingRulesForFacility(facilityId: number) {
	return requestJson<PricingRuleResponse[]>(
		`/api/pricing-rules/facility/${facilityId}`
	)
}

export function getFacilityPriceQuote(input: {
	end: string
	facilityId: number
	start: string
}) {
	const searchParams = new URLSearchParams({
		end: input.end,
		facilityId: String(input.facilityId),
		start: input.start
	})

	return requestJson<PriceQuote>(
		`/api/pricing-rules/calculate?${searchParams.toString()}`
	)
}
