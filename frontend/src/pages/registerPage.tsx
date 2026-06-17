import {Link} from 'react-router'
import {Alert} from '@/components/ui/alert'
import {Button} from '@/components/ui/button'
import {Card, CardContent, CardDescription, CardHeader, CardTitle} from '@/components/ui/card'
import {FieldError} from '@/components/ui/fieldError'
import {Input} from '@/components/ui/input'
import {Label} from '@/components/ui/label'
import {useRegisterForm} from '@/hooks/useRegisterForm'

export function RegisterPage() {
	const {apiError, fieldErrors, form, handleBlur, handleSubmit, isSubmitting, setForm} =
		useRegisterForm()

	return (
		<div className='mx-auto w-full max-w-lg'>
			<Card>
				<CardHeader>
					<CardTitle>Create an account</CardTitle>
					<CardDescription>Join SportsCenter to start booking facilities.</CardDescription>
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
								onChange={e => setForm({...form, username: e.target.value})}
								placeholder='johndoe'
								value={form.username}
							/>
							<FieldError message={fieldErrors.username} />
						</div>

						<div className='space-y-1'>
							<Label htmlFor='email'>Email</Label>
							<Input
								autoComplete='email'
								id='email'
								isInvalid={fieldErrors.email !== null}
								onBlur={() => handleBlur('email')}
								onChange={e => setForm({...form, email: e.target.value})}
								placeholder='john@example.com'
								type='email'
								value={form.email}
							/>
							<FieldError message={fieldErrors.email} />
						</div>

						<div className='space-y-1'>
							<Label htmlFor='password'>Password</Label>
							<Input
								autoComplete='new-password'
								id='password'
								isInvalid={fieldErrors.password !== null}
								onBlur={() => handleBlur('password')}
								onChange={e => setForm({...form, password: e.target.value})}
								placeholder='••••••••'
								type='password'
								value={form.password}
							/>
							<FieldError message={fieldErrors.password} />
						</div>

						<div className='space-y-1'>
							<Label htmlFor='confirmPassword'>Confirm password</Label>
							<Input
								autoComplete='new-password'
								id='confirmPassword'
								isInvalid={fieldErrors.confirmPassword !== null}
								onBlur={() => handleBlur('confirmPassword')}
								onChange={e => setForm({...form, confirmPassword: e.target.value})}
								placeholder='••••••••'
								type='password'
								value={form.confirmPassword}
							/>
							<FieldError message={fieldErrors.confirmPassword} />
						</div>

						{apiError ? <Alert variant='destructive'>{apiError}</Alert> : null}

						<Button className='w-full' disabled={isSubmitting} type='submit'>
							{isSubmitting ? 'Creating account...' : 'Register'}
						</Button>

						<div className='text-center text-sm text-slate-400'>
							Already have an account?{' '}
							<Link className='text-sky-400 hover:underline' to='/login'>
								Sign in
							</Link>
						</div>
					</form>
				</CardContent>
			</Card>
		</div>
	)
}
