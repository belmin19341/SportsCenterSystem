import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {type RenderOptions, render as rtlRender} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type {PropsWithChildren, ReactElement} from 'react'
import {MemoryRouter} from 'react-router'

export const queryClient = new QueryClient({
	defaultOptions: {
		queries: {gcTime: Number.POSITIVE_INFINITY, retry: false}
	}
})

export function render(
	ui: ReactElement,
	{route, ...options}: Omit<RenderOptions, 'wrapper'> & {route?: string} = {
		reactStrictMode: true
	}
) {
	const initialRoute = route || '/'

	return {
		user: userEvent.setup(),
		...rtlRender(ui, {
			wrapper: ({children}: PropsWithChildren) => (
				<QueryClientProvider client={queryClient}>
					<MemoryRouter initialEntries={[initialRoute]}>
						{children}
					</MemoryRouter>
				</QueryClientProvider>
			),
			...options
		})
	}
}

export {fireEvent, screen, waitFor, within} from '@testing-library/react'
