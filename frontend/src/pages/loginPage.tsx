import {useState} from 'react'
import {Link, Navigate, useLocation, useNavigate} from 'react-router'
import {useAuth} from '@/auth/authContext'
import {useFeedback} from '@/components/feedback'
import {Alert} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle
} from '@/components/ui/card'
import {FieldError} from '@/components/ui/fieldError'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {getErrorMessage} from '@/lib/format'
import {validateLoginForm, validatePasswordField, validateUsernameField} from '@/lib/validation'

interface FieldErrors {
	username: string | null
	password: string | null
}

export function LoginPage() {
	const {isSignedIn, login} = useAuth()
	const {showFeedback} = useFeedback()
	const location = useLocation()
	const navigate = useNavigate()
	const [apiError, setApiError] = useState<string | null>(null)
	const [isSubmitting, setIsSubmitting] = useState(false)
	const [password, setPassword] = useState('')
	const [username, setUsername] = useState('')
	const [fieldErrors, setFieldErrors] = useState<FieldErrors>({
		password: null,
		username: null
	})

	const redirectPath =
		(location.state as {from?: {pathname?: string}} | null)?.from?.pathname ||
		'/dashboard'

	if (isSignedIn) {
		return <Navigate replace={true} to='/dashboard' />
	}

	function handleBlur(name: keyof FieldErrors) {
		const newErrors = {...fieldErrors}
		if (name === 'username') {
			newErrors.username = validateUsernameField(username)
		} else {
			newErrors.password = validatePasswordField(password, 6)
		}
		setFieldErrors(newErrors)
	}

	async function handleSubmit(event: {preventDefault(): void}) {
		event.preventDefault()
		setApiError(null)

		const errors: FieldErrors = {
			password: validatePasswordField(password, 6),
			username: validateUsernameField(username)
		}
		setFieldErrors(errors)

		const validationErrors = validateLoginForm({password, username})
		if (validationErrors.length > 0 || Object.values(errors).some(e => e !== null)) {
			return
		}

		setIsSubmitting(true)

		try {
			await login({password, username})
			showFeedback({
				description: 'Your dashboard data is loading from the secured API.',
				title: 'Signed in',
				variant: 'success'
			})
			navigate(redirectPath, {replace: true})
		} catch (error) {
			setApiError(getErrorMessage(error))
		} finally {
			setIsSubmitting(false)
		}
	}

	return (
		<div className='mx-auto w-full max-w-lg'>
			<Card>
				<CardHeader>
					<CardTitle>Sign in to continue</CardTitle>
					<CardDescription>Use your SportsCenter account.</CardDescription>
				</CardHeader>
				<CardContent>
					<form className='space-y-5' onSubmit={handleSubmit}>
						<div className='space-y-1'>
							<Label htmlFor='username'>Username</Label>
							<Input
								autoComplete='username'
								id='username'
								isInvalid={fieldErrors.username !== null}
								onBlur={() => handleBlur('username')}
								onChange={event => setUsername(event.target.value)}
								placeholder='john_doe'
								value={username}
							/>
							<FieldError message={fieldErrors.username} />
						</div>

						<div className='space-y-1'>
							<Label htmlFor='password'>Password</Label>
							<Input
								autoComplete='current-password'
								id='password'
								isInvalid={fieldErrors.password !== null}
								onBlur={() => handleBlur('password')}
								onChange={event => setPassword(event.target.value)}
								placeholder='••••••••'
								type='password'
								value={password}
							/>
							<FieldError message={fieldErrors.password} />
						</div>

						{apiError ? (
							<Alert variant='destructive'>{apiError}</Alert>
						) : null}

						<Button
							className='w-full'
							disabled={isSubmitting}
							size='lg'
							type='submit'
						>
							{isSubmitting ? 'Signing in...' : 'Sign in'}
						</Button>

						<div className='text-center text-sm'>
							<span className='text-slate-400'>Don't have an account? </span>
							<Link className='text-sky-400 hover:underline' to='/register'>
								Register here
							</Link>
						</div>

						<p className='text-sm text-slate-400'>
							Seed account: john_doe / password123
						</p>
					</form>
				</CardContent>
			</Card>
		</div>
	)
}
