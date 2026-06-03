export function FieldError({message}: {message?: string | null}) {
	if (!message) return null
	return (
		<p className='mt-1 text-xs text-rose-400' role='alert'>
			{message}
		</p>
	)
}
