import {type FormEvent, useState} from 'react'
import {Navigate, useLocation, useNavigate} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {Alert} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {getErrorMessage} from '@/lib/format'

export function LoginPage() {
	const {isSignedIn, login} = useAuth()
	const location = useLocation()
	const navigate = useNavigate()
	const [errorMessage, setErrorMessage] = useState<string | null>(null)
	const [isSubmitting, setIsSubmitting] = useState(false)
	const [password, setPassword] = useState('')
	const [username, setUsername] = useState('')

	const redirectPath =
		(location.state as {from?: {pathname?: string}} | null)?.from?.pathname ||
		'/dashboard'

	if (isSignedIn) {
		return <Navigate replace={true} to='/dashboard' />
	}

	async function handleSubmit(event: FormEvent<HTMLFormElement>) {
		event.preventDefault()
		setErrorMessage(null)
		setIsSubmitting(true)

		try {
			await login({password, username})
			navigate(redirectPath, {replace: true})
		} catch (error) {
			setErrorMessage(getErrorMessage(error))
		} finally {
			setIsSubmitting(false)
		}
	}

	return (
		<div className='mx-auto max-w-lg'>
			<Card>
				<CardHeader>
					<CardTitle>Sign in to continue</CardTitle>
					<CardDescription>
						Authentication goes through the existing gateway-backed JWT flow.
					</CardDescription>
				</CardHeader>
				<CardContent>
					<form className='space-y-5' onSubmit={handleSubmit}>
						<div className='space-y-2'>
							<Label htmlFor='username'>Username</Label>
							<Input
								autoComplete='username'
								id='username'
								onChange={event => setUsername(event.target.value)}
								placeholder='john_doe'
								required={true}
								value={username}
							/>
						</div>

						<div className='space-y-2'>
							<Label htmlFor='password'>Password</Label>
							<Input
								autoComplete='current-password'
								id='password'
								onChange={event => setPassword(event.target.value)}
								placeholder='••••••••'
								required={true}
								type='password'
								value={password}
							/>
						</div>

						{errorMessage ? <Alert variant='destructive'>{errorMessage}</Alert> : null}

						<Button className='w-full' disabled={isSubmitting} size='lg' type='submit'>
							{isSubmitting ? 'Signing in...' : 'Sign in'}
						</Button>

						<p className='text-sm text-slate-400'>
							Use a seeded account from the backend if the stack is already
							running locally.
						</p>
					</form>
				</CardContent>
			</Card>
		</div>
	)
}
