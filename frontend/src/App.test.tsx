import {App} from '@/App'
import {fireEvent, render, screen, waitFor, within} from '@/test-utils'

function storeSignedInSession() {
	localStorage.setItem(
		'sportscenter-session',
		JSON.stringify({
			accessToken: 'access-token',
			accessTokenExpiresAt: Date.now() + 60_000,
			email: 'john@example.com',
			refreshToken: 'refresh-token',
			refreshTokenExpiresAt: Date.now() + 600_000,
			role: 'USER',
			tokenType: 'Bearer',
			userId: 1,
			username: 'john_doe'
		})
	)
}

async function chooseBookingTime(
	user: ReturnType<typeof render>['user'],
	fieldLabel: RegExp,
	timeLabel: string
) {
	await user.click(await screen.findByLabelText(fieldLabel))
	const dialog = screen.getByRole('dialog', {
		name: /choose booking date and time/iu
	})
	await user.click(within(dialog).getByRole('button', {name: /tomorrow/iu}))
	await user.click(within(dialog).getByRole('button', {name: timeLabel}))
}

it('renders the public discovery view with live sections', async () => {
	render(<App />, {route: '/'})

	await expect(
		screen.findByRole('heading', {
			name: /find courts, compare availability signals, and reserve your next slot/iu
		})
	).resolves.toBeInTheDocument()

	await expect(
		screen.findByRole('heading', {name: 'Padel Arena Alpha'})
	).resolves.toBeInTheDocument()
	await expect(
		screen.findByText('Carbon Racket Set')
	).resolves.toBeInTheDocument()
})

it('redirects protected routes to login when there is no session', async () => {
	render(<App />, {route: '/dashboard'})

	await expect(
		screen.findByRole('heading', {name: /sign in to continue/iu})
	).resolves.toBeInTheDocument()
})

it('validates login before calling the backend', async () => {
	const {user} = render(<App />, {route: '/login'})

	await user.type(screen.getByLabelText(/username/iu), 'ab')
	await user.type(screen.getByLabelText(/password/iu), '123')
	await user.click(screen.getByRole('button', {name: /sign in/iu}))

	expect(
		await screen.findByText(/username must be between 3 and 100 characters/iu)
	).toBeInTheDocument()
})

it('logs in and loads secured dashboard data', async () => {
	const {user} = render(<App />, {route: '/login'})

	await user.type(screen.getByLabelText(/username/iu), 'john_doe')
	await user.type(screen.getByLabelText(/password/iu), 'password123')
	await user.click(screen.getByRole('button', {name: /sign in/iu}))

	await expect(screen.findByText('john_doe')).resolves.toBeInTheDocument()
	await expect(screen.findByText('SILVER')).resolves.toBeInTheDocument()
})

it('creates a booking only after quote and conflict checks pass', async () => {
	storeSignedInSession()
	const {user} = render(<App />, {route: '/bookings/new?facilityId=1'})

	await screen.findByRole('heading', {name: /create a booking/iu})
	await chooseBookingTime(user, /start time/iu, '10:00')
	expect(screen.getByLabelText(/end time/iu)).toHaveTextContent('11:00')

	await expect(
		screen.findByText(/selected time is available/iu)
	).resolves.toBeInTheDocument()
	await expect(screen.findByText(/quoted total/iu)).resolves.toBeInTheDocument()
	await expect(
		screen.findByText(/standard hourly rate applied/iu)
	).resolves.toBeInTheDocument()

	await waitFor(() => {
		expect(screen.getByRole('button', {name: /book now/iu})).toBeEnabled()
	})
	fireEvent.click(screen.getByRole('button', {name: /book now/iu}))

	await expect(
		screen.findByText(/booking created/iu)
	).resolves.toBeInTheDocument()
})

it('keeps the booking draft when navigating away and back', async () => {
	storeSignedInSession()
	const {user} = render(<App />, {route: '/bookings/new?facilityId=1'})

	await chooseBookingTime(user, /start time/iu, '10:00')
	expect(screen.getByLabelText(/end time/iu)).toHaveTextContent('11:00')

	await user.click(screen.getByRole('link', {name: /dashboard/iu}))
	await screen.findByRole('heading', {name: /profile/iu})
	await user.click(screen.getByRole('link', {name: /new booking/iu}))

	await expect(screen.findByLabelText(/start time/iu)).resolves.toHaveTextContent(
		'10:00'
	)
	expect(screen.getByLabelText(/end time/iu)).toHaveTextContent('11:00')
})
