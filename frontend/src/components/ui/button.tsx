import {cva, type VariantProps} from 'class-variance-authority'
import type {ButtonHTMLAttributes} from 'react'
import {cn} from '@/lib/utils'

const buttonVariants = cva(
	'inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-400 focus-visible:ring-offset-2 focus-visible:ring-offset-slate-950 disabled:pointer-events-none disabled:opacity-50',
	{
		defaultVariants: {
			size: 'default',
			variant: 'default'
		},
		variants: {
			size: {
				default: 'h-10 px-4 py-2',
				lg: 'h-11 px-6 py-2.5',
				sm: 'h-9 px-3'
			},
			variant: {
				default: 'bg-sky-500 text-slate-950 hover:bg-sky-400',
				ghost: 'bg-transparent text-slate-200 hover:bg-slate-900',
				outline:
					'border border-slate-800 bg-slate-950 text-slate-100 hover:border-slate-700 hover:bg-slate-900/60',
				secondary: 'bg-emerald-400 text-slate-950 hover:bg-emerald-300'
			}
		}
	}
)

export interface ButtonProps
	extends ButtonHTMLAttributes<HTMLButtonElement>,
		VariantProps<typeof buttonVariants> {}

export function Button({className, size, variant, ...props}: ButtonProps) {
	return (
		<button
			className={cn(buttonVariants({className, size, variant}))}
			{...props}
		/>
	)
}
