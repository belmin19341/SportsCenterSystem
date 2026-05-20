import type {Role} from '@/types/api'

export function canAccessRole(role: Role, allowedRoles?: Role[]) {
	return !allowedRoles || allowedRoles.includes(role)
}

export function canManageFacilities(role: Role) {
	return role === 'OWNER' || role === 'ADMIN'
}
