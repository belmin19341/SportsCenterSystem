import type {HTMLAttributes} from 'react'
import {cn} from '@/lib/utils'

const variants = {
	default: 'border-slate-800 bg-slate-950/70 text-slate-200',
	destructive: 'border-rose-500/30 bg-rose-500/10 text-rose-100',
	success: 'border-emerald-500/30 bg-emerald-500/10 text-emerald-100'
} as const

export function Alert({
	className,
	variant = 'default',
	...props
}: HTMLAttributes<HTMLDivElement> & {
	variant?: keyof typeof variants
}) {
	return (
		<div
			className={cn(
				'rounded-xl border px-4 py-3 text-sm',
				variants[variant],
				className
			)}
			role='alert'
			{...props}
		/>
	)
}

