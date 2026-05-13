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

export const handlers = [
	http.get('*/api/facilities', () => HttpResponse.json(facilities)),
	http.get('*/api/equipment', () => HttpResponse.json(equipment))
]

