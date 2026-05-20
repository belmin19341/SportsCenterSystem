import '@testing-library/jest-dom/vitest'
import {server} from '@/mocks/server'
import {queryClient} from '@/test-utils'

beforeAll(() => server.listen())
afterEach(() => {
	localStorage.clear()
	sessionStorage.clear()
	queryClient.clear()
	server.resetHandlers()
})
afterAll(() => server.close())
