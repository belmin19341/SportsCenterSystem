import {requestJson} from '@/lib/http'
import type {EquipmentResponse, FacilityResponse, PriceQuote} from '@/types/api'

export function listFacilities() {
	return requestJson<FacilityResponse[]>('/api/facilities')
}

export function listEquipment() {
	return requestJson<EquipmentResponse[]>('/api/equipment')
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

	return requestJson<PriceQuote>(`/api/pricing-rules/calculate?${searchParams.toString()}`)
}

