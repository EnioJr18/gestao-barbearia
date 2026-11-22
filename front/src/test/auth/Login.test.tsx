import { http, HttpResponse } from 'msw'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { Login } from '@/pages/Login'
import { server } from '../mocks/server'
import { renderWithProviders } from '../utils'

const LOGIN_URL = 'http://localhost:8080/api/v1/auth/login'

function LoginRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<Login />} />
      <Route path="/dashboard" element={<h1>Painel autenticado</h1>} />
    </Routes>
  )
}

describe('login', () => {
  it('envia credenciais válidas, guarda o token e segue para o painel', async () => {
    const user = userEvent.setup()
    let requestBody: unknown
    server.use(
      http.post(LOGIN_URL, async ({ request }) => {
        requestBody = await request.json()
        return HttpResponse.json({ token: 'jwt-de-teste', tipo: 'Bearer' })
      }),
    )
    renderWithProviders(<LoginRoutes />, { route: '/login' })

    await user.type(screen.getByLabelText('E-mail'), 'operador@teste.local')
    await user.type(screen.getByLabelText('Senha'), 'senha-segura')
    await user.click(screen.getByRole('button', { name: 'Entrar no painel' }))

    expect(await screen.findByRole('heading', { name: 'Painel autenticado' })).toBeInTheDocument()
    expect(requestBody).toEqual({ email: 'operador@teste.local', senha: 'senha-segura' })
    expect(localStorage.getItem('token')).toBe('jwt-de-teste')
    expect(localStorage.getItem('tipoToken')).toBe('Bearer')
  })

  it('mostra validações de e-mail e senha sem chamar a API', async () => {
    const user = userEvent.setup()
    let requestCount = 0
    server.use(
      http.post(LOGIN_URL, () => {
        requestCount += 1
        return HttpResponse.json({ token: 'unexpected', tipo: 'Bearer' })
      }),
    )
    renderWithProviders(<LoginRoutes />, { route: '/login' })

    await user.type(screen.getByLabelText('E-mail'), 'email-invalido')
    await user.type(screen.getByLabelText('Senha'), '123')
    await user.click(screen.getByRole('button', { name: 'Entrar no painel' }))

    expect(await screen.findByText('Digite um e-mail válido')).toBeInTheDocument()
    expect(screen.getByText('A senha deve ter pelo menos 6 caracteres')).toBeInTheDocument()
    expect(requestCount).toBe(0)
  })

  it('apresenta a mensagem da API quando as credenciais são inválidas', async () => {
    const user = userEvent.setup()
    server.use(
      http.post(LOGIN_URL, () =>
        HttpResponse.json({ message: 'Credenciais inválidas' }, { status: 401 }),
      ),
    )
    renderWithProviders(<LoginRoutes />, { route: '/login' })

    await user.type(screen.getByLabelText('E-mail'), 'operador@teste.local')
    await user.type(screen.getByLabelText('Senha'), 'senha-errada')
    await user.click(screen.getByRole('button', { name: 'Entrar no painel' }))

    expect(await screen.findByText('Credenciais inválidas')).toBeInTheDocument()
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('indica carregamento e impede uma segunda submissão enquanto aguarda', async () => {
    const user = userEvent.setup()
    let resolveResponse: (() => void) | undefined
    let requestCount = 0
    const gate = new Promise<void>((resolve) => {
      resolveResponse = resolve
    })
    server.use(
      http.post(LOGIN_URL, async () => {
        requestCount += 1
        await gate
        return HttpResponse.json({ token: 'jwt-de-teste', tipo: 'Bearer' })
      }),
    )
    renderWithProviders(<LoginRoutes />, { route: '/login' })

    await user.type(screen.getByLabelText('E-mail'), 'operador@teste.local')
    await user.type(screen.getByLabelText('Senha'), 'senha-segura')
    await user.click(screen.getByRole('button', { name: 'Entrar no painel' }))

    const pendingButton = await screen.findByRole('button', { name: 'Aguarde...' })
    expect(pendingButton).toBeDisabled()
    fireEvent.click(pendingButton)
    await waitFor(() => expect(requestCount).toBe(1))

    resolveResponse?.()
    expect(await screen.findByRole('heading', { name: 'Painel autenticado' })).toBeInTheDocument()
    expect(requestCount).toBe(1)
  })
})
