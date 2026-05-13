import {App} from '@/App'
import {render, screen} from '@/test-utils'

it('renders the public discovery view with live sections', async () => {
	render(<App />, {route: '/'})

	await expect(
		screen.findByRole('heading', {
			name: /find courts, understand pricing, and move into authenticated booking flows/i
		})
	).resolves.toBeInTheDocument()

	await expect(screen.findByText('Padel Arena Alpha')).resolves.toBeInTheDocument()
	await expect(screen.findByText('Carbon Racket Set')).resolves.toBeInTheDocument()
})

it('redirects protected routes to login when there is no session', async () => {
	render(<App />, {route: '/dashboard'})

	await expect(
		screen.findByRole('heading', {name: /sign in to continue/i})
	).resolves.toBeInTheDocument()
})
