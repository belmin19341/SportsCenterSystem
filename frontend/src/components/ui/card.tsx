import type {HTMLAttributes} from 'react'
import {cn} from '@/lib/utils'

export function Card({className, ...props}: HTMLAttributes<HTMLDivElement>) {
	return (
		<div
			className={cn(
				'rounded-2xl border border-slate-800 bg-slate-950/70 shadow-lg shadow-slate-950/30',
				className
			)}
			{...props}
		/>
	)
}

export function CardHeader({
	className,
	...props
}: HTMLAttributes<HTMLDivElement>) {
	return <div className={cn('space-y-2 p-6', className)} {...props} />
}

export function CardTitle({
	className,
	...props
}: HTMLAttributes<HTMLHeadingElement>) {
	return (
		<h2
			className={cn('text-lg font-semibold text-slate-50', className)}
			{...props}
		/>
	)
}

export function CardDescription({
	className,
	...props
}: HTMLAttributes<HTMLParagraphElement>) {
	return <p className={cn('text-sm text-slate-400', className)} {...props} />
}

export function CardContent({
	className,
	...props
}: HTMLAttributes<HTMLDivElement>) {
	return <div className={cn('p-6 pt-0', className)} {...props} />
}

export function CardFooter({
	className,
	...props
}: HTMLAttributes<HTMLDivElement>) {
	return (
		<div
			className={cn('flex items-center gap-3 p-6 pt-0', className)}
			{...props}
		/>
	)
}
