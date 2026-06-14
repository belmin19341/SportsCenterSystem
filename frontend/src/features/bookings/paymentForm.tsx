import {CardElement, useElements, useStripe} from '@stripe/react-stripe-js'
import {forwardRef, useEffect, useImperativeHandle, useState} from 'react'
import {Label} from '@/components/ui/label'
import type {PaymentMethod, SavedCardResponse} from '@/types/api'

export interface PaymentFormHandle {
	getPaymentData(): Promise<PaymentData>
}

export interface PaymentData {
	paymentMethod: PaymentMethod
	stripeToken?: string
	savedCardId?: number
	saveCard?: boolean
	cardLast4?: string
	cardBrand?: string
}

interface Props {
	savedCards: SavedCardResponse[]
	stripeReady: boolean
}

const cardElementOptions = {
	disableLink: true,
	style: {
		base: {
			color: '#f8fafc',
			fontFamily: 'inherit',
			fontSize: '14px',
			'::placeholder': {color: '#64748b'}
		},
		invalid: {color: '#f87171'}
	}
}

export const PaymentForm = forwardRef<PaymentFormHandle, Props>(
	({savedCards, stripeReady}, ref) => {
		const stripe = useStripe()
		const elements = useElements()

		const [mode, setMode] = useState<'SAVED' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'CASH'>(
			savedCards.length > 0 ? 'SAVED' : 'CREDIT_CARD'
		)
		const [selectedCardId, setSelectedCardId] = useState<number | null>(
			savedCards.length > 0 ? savedCards[0].id : null
		)
		const [saveCard, setSaveCard] = useState(false)

		// savedCards arrives async — sync mode and selectedCardId once data loads
		useEffect(() => {
			if (savedCards.length > 0 && selectedCardId === null) {
				setSelectedCardId(savedCards[0].id)
				setMode('SAVED')
			}
		}, [savedCards.length]) // eslint-disable-line react-hooks/exhaustive-deps

		useImperativeHandle(ref, () => ({
			async getPaymentData(): Promise<PaymentData> {
				if (mode === 'CASH') {
					return {paymentMethod: 'CASH'}
				}

				if (mode === 'SAVED' && selectedCardId) {
					return {paymentMethod: 'CREDIT_CARD', savedCardId: selectedCardId}
				}

				// Card mode — tokenise via Stripe.js
				if (!stripe || !elements) {
					throw new Error('Stripe not loaded yet.')
				}
				const cardElement = elements.getElement(CardElement)
				if (!cardElement) {
					throw new Error('Card form not mounted.')
				}
				const {token, error} = await stripe.createToken(cardElement)
				if (error || !token) {
					throw new Error(error?.message ?? 'Could not tokenise card.')
				}
				return {
					paymentMethod: mode as PaymentMethod,
					stripeToken: token.id,
					saveCard,
					cardLast4: token.card?.last4,
					cardBrand: token.card?.brand
				}
			}
		}))

		return (
			<div className='space-y-4'>
				<Label>Payment method</Label>

				{/* Mode selection */}
				<div className='grid gap-2 sm:grid-cols-3'>
					{savedCards.length > 0 && (
						<ModeButton active={mode === 'SAVED'} onClick={() => setMode('SAVED')}>
							Saved card
						</ModeButton>
					)}
					<ModeButton
						active={mode === 'CREDIT_CARD'}
						onClick={() => setMode('CREDIT_CARD')}
					>
						Credit card
					</ModeButton>
					<ModeButton
						active={mode === 'DEBIT_CARD'}
						onClick={() => setMode('DEBIT_CARD')}
					>
						Debit card
					</ModeButton>
					<ModeButton active={mode === 'CASH'} onClick={() => setMode('CASH')}>
						Cash
					</ModeButton>
				</div>

				{/* Saved card picker */}
				{mode === 'SAVED' && (
					<div className='space-y-2'>
						<Label htmlFor='savedCard'>Your saved cards</Label>
						<select
							className='flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400'
							id='savedCard'
							onChange={e => setSelectedCardId(Number(e.target.value))}
							value={selectedCardId ?? ''}
						>
							{savedCards.map(card => (
								<option key={card.id} value={card.id}>
									{card.brand.toUpperCase()} •••• {card.last4}
								</option>
							))}
						</select>
					</div>
				)}

				{/* Stripe card element */}
				{(mode === 'CREDIT_CARD' || mode === 'DEBIT_CARD') && (
					<div className='space-y-3'>
						{stripeReady ? (
							<>
								<div className='rounded-md border border-slate-800 bg-slate-950 px-3 py-3'>
									<CardElement options={cardElementOptions} />
								</div>
								<p className='text-xs text-slate-500'>
									Test card: <span className='font-mono'>4242 4242 4242 4242</span> · any
									future date · any 3-digit CVV
								</p>
								<label className='flex cursor-pointer items-center gap-2 text-sm text-slate-300'>
									<input
										checked={saveCard}
										className='h-4 w-4 accent-sky-400'
										onChange={e => setSaveCard(e.target.checked)}
										type='checkbox'
									/>
									Save card for future payments
								</label>
							</>
						) : (
							<div className='text-sm text-slate-400'>
								Stripe is not configured — payment will be auto-approved.
							</div>
						)}
					</div>
				)}

				{/* Cash info */}
				{mode === 'CASH' && (
					<p className='text-sm text-slate-400'>
						Pay in cash at the facility. Your booking will be confirmed immediately.
					</p>
				)}
			</div>
		)
	}
)

PaymentForm.displayName = 'PaymentForm'

function ModeButton({
	active,
	children,
	onClick
}: {
	active: boolean
	children: React.ReactNode
	onClick: () => void
}) {
	return (
		<button
			className={`rounded-md border px-4 py-2 text-sm transition-colors ${
				active
					? 'border-sky-500 bg-sky-500/10 text-sky-400'
					: 'border-slate-700 bg-slate-900 text-slate-400 hover:border-slate-600'
			}`}
			onClick={onClick}
			type='button'
		>
			{children}
		</button>
	)
}
