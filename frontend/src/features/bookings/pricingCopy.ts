export function formatDuration(hours: number) {
	const formattedHours = new Intl.NumberFormat('en', {
		maximumFractionDigits: 2
	}).format(hours)

	return `${formattedHours} ${hours === 1 ? 'hour' : 'hours'}`
}

export function formatRateSummary(multiplier: number) {
	if (Math.abs(multiplier - 1) < 0.001) {
		return 'Standard hourly rate applied.'
	}

	const percentage = new Intl.NumberFormat('en', {
		maximumFractionDigits: 0
	}).format(Math.abs(multiplier - 1) * 100)

	return multiplier > 1
		? `${percentage}% peak-time price increase applied.`
		: `${percentage}% price discount applied.`
}

export function formatPriceAdjustment(multiplier: number) {
	if (Math.abs(multiplier - 1) < 0.001) {
		return 'Standard rate'
	}

	const percentage = new Intl.NumberFormat('en', {
		maximumFractionDigits: 0
	}).format(Math.abs(multiplier - 1) * 100)

	return multiplier > 1
		? `${percentage}% higher rate`
		: `${percentage}% lower rate`
}
