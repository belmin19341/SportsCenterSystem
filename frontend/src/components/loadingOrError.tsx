import {Alert} from '@/components/ui/alert'
import {getErrorMessages} from '@/lib/format'

export function LoadingOrError(props: {error?: unknown; title?: string}) {
	if (props.error) {
		const messages = getErrorMessages(props.error)

		return (
			<Alert variant='destructive'>
				<div className='font-semibold'>Could not load data</div>
				<div className='mt-1 space-y-1 text-sm'>
					{messages.map(message => (
						<div key={message}>{message}</div>
					))}
				</div>
			</Alert>
		)
	}

	return (
		<div className='rounded-xl border border-dashed border-slate-800 bg-slate-950/40 px-4 py-6 text-sm text-slate-400'>
			{props.title || 'Loading...'}
		</div>
	)
}
