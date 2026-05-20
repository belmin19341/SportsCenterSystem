export function isValidDateRange(startTime: string, endTime: string) {
	if (!(startTime && endTime)) {
		return false
	}

	const start = new Date(startTime)
	const end = new Date(endTime)

	return (
		!(Number.isNaN(start.valueOf()) || Number.isNaN(end.valueOf())) &&
		start.valueOf() > Date.now() &&
		end.valueOf() > start.valueOf()
	)
}
