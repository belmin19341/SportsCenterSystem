import {
	createContext,
	type PropsWithChildren,
	useContext,
	useEffect,
	useMemo,
	useState
} from 'react'
import {Alert} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'

type FeedbackVariant = 'default' | 'destructive' | 'success'

interface FeedbackMessage {
	description?: string
	id: number
	title: string
	variant: FeedbackVariant
}

interface FeedbackInput {
	description?: string
	title: string
	variant?: FeedbackVariant
}

interface FeedbackContextValue {
	clearFeedback: () => void
	message: FeedbackMessage | null
	showFeedback: (input: FeedbackInput) => void
}

const FeedbackContext = createContext<FeedbackContextValue | null>(null)
const AUTO_DISMISS_DELAY_MS = 4500 // 4.5 seconds

export function FeedbackProvider({children}: PropsWithChildren) {
	const [message, setMessage] = useState<FeedbackMessage | null>(null)

	const value = useMemo<FeedbackContextValue>(
		() => ({
			clearFeedback() {
				setMessage(null)
			},
			message,
			showFeedback(input: FeedbackInput) {
				setMessage({
					description: input.description,
					id: Date.now(),
					title: input.title,
					variant: input.variant || 'default'
				})
			}
		}),
		[message]
	)

	return (
		<FeedbackContext.Provider value={value}>
			{children}
		</FeedbackContext.Provider>
	)
}

export function FeedbackBanner() {
	const {clearFeedback, message} = useFeedback()

	useEffect(() => {
		if (!message) return

		const timeoutId = setTimeout(clearFeedback, AUTO_DISMISS_DELAY_MS)
		return () => clearTimeout(timeoutId)
	}, [message, clearFeedback])

	if (!message) {
		return null
	}

	return (
		<div className='mx-auto w-full max-w-7xl px-4 pt-5 sm:px-6 lg:px-8'>
			<Alert
				className='flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between'
				variant={message.variant}
			>
				<div>
					<div className='font-semibold'>{message.title}</div>
					{message.description ? (
						<div className='mt-1 text-sm opacity-90'>{message.description}</div>
					) : null}
				</div>
				<Button
					aria-label='Dismiss feedback'
					onClick={clearFeedback}
					size='sm'
					type='button'
					variant='ghost'
				>
					Dismiss
				</Button>
			</Alert>
		</div>
	)
}

export function useFeedback() {
	const context = useContext(FeedbackContext)
	if (!context) {
		throw new Error('useFeedback must be used within FeedbackProvider')
	}

	return context
}
