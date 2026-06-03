import './global.css'
import {QueryClient, QueryClientProvider} from '@tanstack/react-query'
import {ReactQueryDevtools} from '@tanstack/react-query-devtools'
import {StrictMode} from 'react'
import {createRoot} from 'react-dom/client'
import {BrowserRouter} from 'react-router'
import {App} from '@/App'

const queryClient = new QueryClient({
	defaultOptions: {
		mutations: {
			retry: 0
		},
		queries: {
			refetchOnWindowFocus: false,
			retry: 1,
			staleTime: 30_000
		}
	}
})

const container = document.querySelector('#root')

if (!container) {
	throw new Error('Root container #root was not found')
}

const root = createRoot(container)
root.render(
	<StrictMode>
		<QueryClientProvider client={queryClient}>
			<BrowserRouter>
				<App />
			</BrowserRouter>
			{import.meta.env.DEV && <ReactQueryDevtools initialIsOpen={false} />}
		</QueryClientProvider>
	</StrictMode>
)
