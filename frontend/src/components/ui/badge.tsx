import type {HTMLAttributes} from 'react'
import {cn} from '@/lib/utils'

const variants = {
	default: 'bg-sky-500/15 text-sky-300 ring-1 ring-inset ring-sky-400/25',
	muted: 'bg-slate-900 text-slate-300 ring-1 ring-inset ring-slate-700',
	success:
		'bg-emerald-500/15 text-emerald-300 ring-1 ring-inset ring-emerald-400/25',
	warning: 'bg-amber-500/15 text-amber-300 ring-1 ring-inset ring-amber-400/25'
} as const

export function Badge({
	children,
	className,
	variant = 'default',
	...props
}: HTMLAttributes<HTMLSpanElement> & {
	variant?: keyof typeof variants
}) {
	return (
		<span
			className={cn(
				'inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium',
				variants[variant],
				className
			)}
			{...props}
		>
			{children}
		</span>
	)
}
