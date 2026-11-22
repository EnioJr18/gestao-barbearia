import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll, vi } from 'vitest'
import { api } from '../services/api'
import { server } from './mocks/server'

// O adapter Fetch permite que o MSW em Node intercepte as chamadas Axios no jsdom.
api.defaults.adapter = 'fetch'

beforeAll(() => {
  server.listen({ onUnhandledFrame: 'error' })
})

afterEach(() => {
  cleanup()
  server.resetHandlers()
  localStorage.clear()
  sessionStorage.clear()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

afterAll(() => {
  server.close()
})
