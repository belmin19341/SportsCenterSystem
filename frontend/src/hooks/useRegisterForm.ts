import {useState} from 'react'
import {useNavigate} from 'react-router'
import {useFeedback} from '@/components/feedback'
import {register} from '@/features/user/api'
import {getErrorMessage} from '@/lib/format'
import {
	validateConfirmPasswordField,
	validateEmailField,
	validatePasswordField,
	validateUsernameField
} from '@/lib/validation'

interface FieldErrors {
	username: string | null
	email: string | null
	password: string | null
	confirmPassword: string | null
}

function emptyFieldErrors(): FieldErrors {
	return {confirmPassword: null, email: null, password: null, username: null}
}

export function useRegisterForm() {
	const {showFeedback} = useFeedback()
	const navigate = useNavigate()
	const [apiError, setApiError] = useState<string | null>(null)
	const [isSubmitting, setIsSubmitting] = useState(false)
	const [form, setForm] = useState({
		confirmPassword: '',
		email: '',
		password: '',
		role: 'USER',
		username: ''
	})
	const [fieldErrors, setFieldErrors] = useState<FieldErrors>(emptyFieldErrors)

	function handleBlur(name: keyof FieldErrors) {
		const newErrors = {...fieldErrors}
		switch (name) {
			case 'username':
				newErrors.username = validateUsernameField(form.username)
				break
			case 'email':
				newErrors.email = validateEmailField(form.email)
				break
			case 'password':
				newErrors.password = validatePasswordField(form.password)
				if (form.confirmPassword) {
					newErrors.confirmPassword = validateConfirmPasswordField(
						form.password,
						form.confirmPassword
					)
				}
				break
			case 'confirmPassword':
				newErrors.confirmPassword = validateConfirmPasswordField(
					form.password,
					form.confirmPassword
				)
				break
		}
		setFieldErrors(newErrors)
	}

	async function handleSubmit(event: {preventDefault(): void}) {
		event.preventDefault()
		setApiError(null)

		const errors: FieldErrors = {
			confirmPassword: validateConfirmPasswordField(
				form.password,
				form.confirmPassword
			),
			email: validateEmailField(form.email),
			password: validatePasswordField(form.password),
			username: validateUsernameField(form.username)
		}
		setFieldErrors(errors)

		if (Object.values(errors).some(e => e !== null)) {
			return
		}

		setIsSubmitting(true)

		try {
			await register({
				email: form.email,
				password: form.password,
				role: form.role,
				username: form.username
			})
			showFeedback({
				description: 'You can now sign in with your credentials.',
				title: 'Account created',
				variant: 'success'
			})
			navigate('/login')
		} catch (error) {
			setApiError(getErrorMessage(error))
		} finally {
			setIsSubmitting(false)
		}
	}

	return {apiError, fieldErrors, form, handleBlur, handleSubmit, isSubmitting, setForm}
}
