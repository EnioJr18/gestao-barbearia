import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { Clientes } from '@/pages/Clientes'
import { server } from '../mocks/server'
import { renderWithProviders } from '../utils'

const CLIENTES_URL = 'http://localhost:8080/api/v1/clientes'

describe('formulário de clientes', () => {
  it('aplica busca por nome ou telefone somente no envio e restaura a lista ao limpar', async () => {
    const user = userEvent.setup()
    let requests = 0
    const cliente = (id: string, nome: string, telefone: string) => ({
      id, nome, telefone, ativo: true,
      createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
      visitas: 0, totalGasto: 0, ultimaVisita: null,
    })
    server.use(http.get(CLIENTES_URL, () => {
      requests += 1
      return HttpResponse.json([
        cliente('a', 'Ana Silva', '(82) 99999-1111'),
        cliente('b', 'Bruno Costa', '(82) 99999-2222'),
      ])
    }))
    renderWithProviders(<Clientes />)

    expect(await screen.findByText('Ana Silva')).toBeInTheDocument()
    expect(screen.getByText('Bruno Costa')).toBeInTheDocument()
    const input = screen.getByPlaceholderText('Buscar cliente')
    await user.type(input, 'Ana')
    expect(requests).toBe(1)
    expect(screen.getByText('Bruno Costa')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(screen.queryByText('Bruno Costa')).not.toBeInTheDocument()
    expect(input).toHaveValue('Ana')

    await user.clear(input)
    await user.type(input, '2222{Enter}')
    expect(screen.queryByText('Ana Silva')).not.toBeInTheDocument()
    expect(screen.getByText('Bruno Costa')).toBeInTheDocument()
    await user.clear(input)
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(screen.getByText('Ana Silva')).toBeInTheDocument()
    expect(requests).toBe(1)
  })

  it('valida nome obrigatório e cria cliente sem telefone', async () => {
    const user = userEvent.setup()
    let payload: unknown
    server.use(
      http.get(CLIENTES_URL, () => HttpResponse.json([])),
      http.post(CLIENTES_URL, async ({ request }) => {
        payload = await request.json()
        return HttpResponse.json({
          id: 'cliente-1',
          nome: 'João Silva',
          telefone: null,
          ativo: true,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
          visitas: 0,
          totalGasto: 0,
          ultimaVisita: null,
        }, { status: 201 })
      }),
    )
    renderWithProviders(<Clientes />)

    expect(await screen.findByText('Nenhum cliente encontrado.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Novo cliente' }))
    await user.click(screen.getByRole('button', { name: 'Cadastrar cliente' }))
    expect(await screen.findByText('Informe o nome')).toBeInTheDocument()
    expect(payload).toBeUndefined()

    await user.type(screen.getByLabelText('Nome completo'), 'João Silva')
    await user.click(screen.getByRole('button', { name: 'Cadastrar cliente' }))

    await waitFor(() => expect(payload).toEqual({ nome: 'João Silva', telefone: null }))
    await waitFor(() => expect(screen.queryByLabelText('Nome completo')).not.toBeInTheDocument())
  })
})
