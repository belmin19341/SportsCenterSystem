import {Alert} from '@/components/ui/alert'
import {getErrorMessage} from '@/lib/format'

export function LoadingOrError(props: {
	error?: unknown
	title?: string
}) {
	if (props.error) {
		return (
			<Alert variant='destructive'>
				<div className='font-semibold'>Could not load data</div>
				<div className='mt-1 text-sm'>{getErrorMessage(props.error)}</div>
			</Alert>
		)
	}

	return (
		<div className='rounded-xl border border-dashed border-slate-800 bg-slate-950/40 px-4 py-6 text-sm text-slate-400'>
			{props.title || 'Loading...'}
		</div>
	)
}

