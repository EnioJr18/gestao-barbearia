import { fireEvent, screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { ProtectedRoute } from '@/components/ProtectedRoute'
import { renderWithProviders } from '../utils'

function ProtectedRoutes() {
  return (
    <Routes>
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <h1>Dashboard protegido</h1>
          </ProtectedRoute>
        }
      />
      <Route path="/login" element={<h1>Tela de login</h1>} />
    </Routes>
  )
}

describe('ProtectedRoute', () => {
  it('direciona para login quando não há token', async () => {
    renderWithProviders(<ProtectedRoutes />, { route: '/dashboard' })

    expect(await screen.findByRole('heading', { name: 'Tela de login' })).toBeInTheDocument()
  })

  it('permite acesso à rota protegida com token salvo', () => {
    localStorage.setItem('token', 'jwt-de-teste')

    renderWithProviders(<ProtectedRoutes />, { route: '/dashboard' })

    expect(screen.getByRole('heading', { name: 'Dashboard protegido' })).toBeInTheDocument()
  })

  it('redireciona para login quando recebe o evento de logout', async () => {
    localStorage.setItem('token', 'jwt-de-teste')
    renderWithProviders(<ProtectedRoutes />, { route: '/dashboard' })

    localStorage.removeItem('token')
    fireEvent(window, new Event('auth:logout'))

    expect(await screen.findByRole('heading', { name: 'Tela de login' })).toBeInTheDocument()
  })
})
