import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { api } from '@/services/api'
import { server } from '../mocks/server'

const API_URL = 'http://localhost:8080/api/v1'

describe('interceptors HTTP da API', () => {
  it('envia o access token no header Authorization', async () => {
    localStorage.setItem('token', 'jwt-de-teste')
    let authorization: string | null = null
    server.use(
      http.get(`${API_URL}/clientes`, ({ request }) => {
        authorization = request.headers.get('Authorization')
        return HttpResponse.json([])
      }),
    )

    await api.get('/clientes')

    expect(authorization).toBe('Bearer jwt-de-teste')
  })

  it('remove a sessão e solicita navegação ao login em HTTP 401 protegido', async () => {
    localStorage.setItem('token', 'jwt-expirado')
    localStorage.setItem('tipoToken', 'Bearer')
    const dispatchEvent = vi.fn()
    const replace = vi.fn()
    vi.stubGlobal('window', {
      location: { pathname: '/dashboard', replace },
      dispatchEvent,
    })
    server.use(
      http.get(`${API_URL}/clientes`, () =>
        HttpResponse.json({ message: 'Autenticação necessária' }, { status: 401 }),
      ),
    )

    await expect(api.get('/clientes')).rejects.toMatchObject({ response: { status: 401 } })

    expect(localStorage.getItem('token')).toBeNull()
    expect(localStorage.getItem('tipoToken')).toBeNull()
    expect(dispatchEvent).toHaveBeenCalledWith(expect.objectContaining({ type: 'auth:logout' }))
    expect(replace).toHaveBeenCalledWith('/login')
  })

  it('não encerra sessão automaticamente em HTTP 403', async () => {
    localStorage.setItem('token', 'jwt-valido')
    const dispatchEvent = vi.fn()
    const replace = vi.fn()
    vi.stubGlobal('window', {
      location: { pathname: '/dashboard', replace },
      dispatchEvent,
    })
    server.use(
      http.get(`${API_URL}/usuarios`, () =>
        HttpResponse.json({ message: 'Acesso negado' }, { status: 403 }),
      ),
    )

    await expect(api.get('/usuarios')).rejects.toMatchObject({ response: { status: 403 } })

    expect(localStorage.getItem('token')).toBe('jwt-valido')
    expect(dispatchEvent).not.toHaveBeenCalled()
    expect(replace).not.toHaveBeenCalled()
  })

  it('não apaga a sessão por um HTTP 401 da própria rota de login', async () => {
    localStorage.setItem('token', 'token-anterior')
    const dispatchEvent = vi.fn()
    const replace = vi.fn()
    vi.stubGlobal('window', {
      location: { pathname: '/login', replace },
      dispatchEvent,
    })
    server.use(
      http.post(`${API_URL}/auth/login`, () =>
        HttpResponse.json({ message: 'Credenciais inválidas' }, { status: 401 }),
      ),
    )

    await expect(api.post('/auth/login', { email: 'x', senha: 'x' })).rejects.toMatchObject({
      response: { status: 401 },
    })

    expect(localStorage.getItem('token')).toBe('token-anterior')
    expect(dispatchEvent).not.toHaveBeenCalled()
    expect(replace).not.toHaveBeenCalled()
  })
})
