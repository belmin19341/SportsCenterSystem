import type {PropsWithChildren} from 'react'
import {AppHeader} from '@/components/appHeader'
import {FeedbackBanner} from '@/components/feedback'

export function AppShell({children}: PropsWithChildren) {
	return (
		<div className='min-h-screen overflow-x-hidden'>
			<AppHeader />
			<FeedbackBanner />
			<main className='mx-auto w-full max-w-7xl px-3 py-5 sm:px-6 sm:py-8 lg:px-8 lg:py-10'>
				{children}
			</main>
			<footer className='border-t border-slate-800/80 px-4 py-6 text-center text-sm text-slate-500'>
				SportsCenterSystem
			</footer>
		</div>
	)
}
