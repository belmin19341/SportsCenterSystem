import type {InputHTMLAttributes} from 'react'
import {cn} from '@/lib/utils'

export function Input({
	className,
	isInvalid,
	...props
}: InputHTMLAttributes<HTMLInputElement> & {isInvalid?: boolean}) {
	return (
		<input
			aria-invalid={isInvalid || undefined}
			className={cn(
				'flex h-11 w-full rounded-md border border-slate-800 bg-slate-950 px-3 py-2 text-sm text-slate-50 placeholder:text-slate-500 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400',
				isInvalid && 'border-rose-500/70 focus-visible:ring-rose-400',
				className
			)}
			{...props}
		/>
	)
}
