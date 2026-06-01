import {type FormEvent, useState} from 'react'
import {Link, useNavigate} from 'react-router'
import {useFeedback} from '@/components/feedback'
import {Alert} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {getErrorMessage} from '@/lib/format'
import {validateRegisterForm} from '@/lib/validation'

export function RegisterPage() {
	const {showFeedback} = useFeedback()
	const navigate = useNavigate()
	const [errorMessage, setErrorMessage] = useState<string | null>(null)
	const [isSubmitting, setIsSubmitting] = useState(false)
	const [form, setForm] = useState({
		username: '',
		email: '',
		password: '',
		role: 'USER'
	})

	async function handleSubmit(event: FormEvent<HTMLFormElement>) {
		event.preventDefault()
		setErrorMessage(null)

		const validationErrors = validateRegisterForm(form)
		if (validationErrors.length > 0) {
			setErrorMessage(validationErrors.join(' '))
			return
		}

		setIsSubmitting(true)

		try {
			const response = await fetch(`${import.meta.env.VITE_API_BASE_URL}/api/auth/register`, {
				method: 'POST',
				headers: {'Content-Type': 'application/json'},
				body: JSON.stringify(form)
			})

			if (!response.ok) {
				const errorData = await response.json().catch(() => ({}));
				throw new Error(errorData.message || 'Registration failed. Username or email might be taken.')
			}

			showFeedback({
				title: 'Account created',
				description: 'You can now sign in with your credentials.',
				variant: 'success'
			})
			navigate('/login')
		} catch (error) {
			setErrorMessage(getErrorMessage(error))
		} finally {
			setIsSubmitting(false)
		}
	}

	return (
		<div className='mx-auto w-full max-w-lg'>
			<Card>
				<CardHeader>
					<CardTitle>Create an account</CardTitle>
					<CardDescription>Join SportsCenter to start booking facilities.</CardDescription>
				</CardHeader>
				<CardContent>
					<form className='space-y-5' onSubmit={handleSubmit}>
						<div className='space-y-2'>
							<Label htmlFor='username'>Username</Label>
							<Input
								autoComplete='username'
								id='username'
								onChange={e => setForm({...form, username: e.target.value})}
								placeholder='johndoe'
								required={true}
								value={form.username}
							/>
						</div>
						<div className='space-y-2'>
							<Label htmlFor='email'>Email</Label>
							<Input
								autoComplete='email'
								id='email'
								onChange={e => setForm({...form, email: e.target.value})}
								placeholder='john@example.com'
								required={true}
								type='email'
								value={form.email}
							/>
						</div>
						<div className='space-y-2'>
							<Label htmlFor='password'>Password</Label>
							<Input
								autoComplete='new-password'
								id='password'
								onChange={e => setForm({...form, password: e.target.value})}
								placeholder='••••••••'
								required={true}
								type='password'
								value={form.password}
							/>
						</div>
						{errorMessage && <Alert variant='destructive'>{errorMessage}</Alert>}
						<Button className='w-full' disabled={isSubmitting} type='submit'>
							{isSubmitting ? 'Creating account...' : 'Register'}
						</Button>
						<div className='text-center text-sm text-slate-400'>
							Already have an account? <Link className='text-sky-400 hover:underline' to='/login'>Sign in</Link>
						</div>
					</form>
				</CardContent>
			</Card>
		</div>
	)
}