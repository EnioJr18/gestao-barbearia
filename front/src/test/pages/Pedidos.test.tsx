import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse, type HttpResponseResolver } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { Pedidos } from '@/pages/Pedidos'
import { server } from '../mocks/server'
import { renderWithProviders } from '../utils'

const API_URL = 'http://localhost:8080/api/v1'
const emptyPage = (page = 0, totalPages = 0) => ({
  content: [],
  page,
  size: 10,
  totalElements: totalPages * 10,
  totalPages,
  first: page === 0,
  last: page === totalPages - 1,
})

const client = {
  id: 'client-uuid',
  nome: 'Camila Souza',
  telefone: null,
  ativo: true,
  createdAt: '2026-01-01T10:00:00Z',
  updatedAt: '2026-01-01T10:00:00Z',
  visitas: 0,
  totalGasto: 0,
  ultimaVisita: null,
}

const item = (id: string, nome: string, ativo: boolean) => ({
  id,
  nome,
  tipo: 'SERVICO',
  preco: 30,
  estoque: null,
  ativo,
  createdAt: '2026-01-01T10:00:00Z',
  updatedAt: '2026-01-01T10:00:00Z',
})

function installOrderDependencies(
  getVendas: HttpResponseResolver,
  items = [item('service-1', 'Corte', true)],
) {
  server.use(
    http.get(`${API_URL}/vendas`, getVendas),
    http.get(`${API_URL}/clientes`, () => HttpResponse.json([client])),
    http.get(`${API_URL}/itens`, () => HttpResponse.json({
      ...emptyPage(),
      content: items,
      size: 100,
      totalElements: items.length,
      totalPages: items.length ? 1 : 0,
    })),
  )
}

describe('Pedidos', () => {
  it('busca nome do cliente somente ao enviar, mantém filtros e paginação', async () => {
    const user = userEvent.setup()
    const requests: URL[] = []
    installOrderDependencies(({ request }) => {
      const url = new URL(request.url)
      requests.push(url)
      return HttpResponse.json(emptyPage(Number(url.searchParams.get('page')), 2))
    })
    renderWithProviders(<Pedidos />)

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    await user.selectOptions(screen.getByLabelText('Status'), 'FINALIZADA')
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('status')).toBe('FINALIZADA'))
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()

    const input = screen.getByLabelText('Buscar por nome do cliente')
    const beforeTyping = requests.length
    await user.type(input, 'Camila')
    expect(requests).toHaveLength(beforeTyping)
    await user.keyboard('{Enter}')
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get('clienteNome')).toBe('Camila')
      expect(requests.at(-1)?.searchParams.get('page')).toBe('0')
    })
    expect(requests.at(-1)?.searchParams.get('status')).toBe('FINALIZADA')
    expect(input).toHaveValue('Camila')
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('page')).toBe('1'))
    expect(requests.at(-1)?.searchParams.get('clienteNome')).toBe('Camila')

    await user.clear(input)
    expect(requests.at(-1)?.searchParams.get('clienteNome')).toBe('Camila')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.has('clienteNome')).toBe(false)
      expect(requests.at(-1)?.searchParams.get('page')).toBe('0')
    })
    expect(requests.at(-1)?.searchParams.get('status')).toBe('FINALIZADA')
  })

  it('combina filtros, reinicia a página e limpa os parâmetros opcionais', async () => {
    const user = userEvent.setup()
    const requests: URL[] = []
    installOrderDependencies(({ request }) => {
      const url = new URL(request.url)
      requests.push(url)
      const page = Number(url.searchParams.get('page'))
      return HttpResponse.json(emptyPage(page, 2))
    })
    renderWithProviders(<Pedidos />)

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('Data inicial'), { target: { value: '2026-06-01' } })
    fireEvent.change(screen.getByLabelText('Data final'), { target: { value: '2026-06-30' } })
    await user.selectOptions(screen.getByLabelText('Cliente'), 'client-uuid')
    await user.selectOptions(screen.getByLabelText('Status'), 'CANCELADA')

    await waitFor(() => {
      const latest = requests.at(-1)
      expect(latest?.searchParams.get('page')).toBe('0')
      expect(latest?.searchParams.get('dataInicial')).toBe('2026-06-01')
      expect(latest?.searchParams.get('dataFinal')).toBe('2026-06-30')
      expect(latest?.searchParams.get('cliente')).toBe('client-uuid')
      expect(latest?.searchParams.get('status')).toBe('CANCELADA')
    })

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    await waitFor(() => {
      const latest = requests.at(-1)
      expect(latest?.searchParams.get('page')).toBe('0')
      expect(latest?.searchParams.has('dataInicial')).toBe(false)
      expect(latest?.searchParams.has('dataFinal')).toBe(false)
      expect(latest?.searchParams.has('cliente')).toBe(false)
      expect(latest?.searchParams.has('status')).toBe(false)
    })
  })

  it('cria venda com cliente opcional e envia pagamento selecionado ao backend', async () => {
    const user = userEvent.setup()
    let payload: unknown
    installOrderDependencies(() => HttpResponse.json(emptyPage()))
    server.use(
      http.post(`${API_URL}/vendas`, async ({ request }) => {
        payload = await request.json()
        return HttpResponse.json({
          id: 'sale-1',
          clienteId: null,
          caixaId: 'cash-1',
          dataVenda: '2026-06-12',
          valorTotal: 30,
          status: 'FINALIZADA',
          createdAt: '2026-06-12T10:00:00Z',
          itens: [{ itemId: 'service-1', quantidade: 1, precoUnitario: 30, subtotal: 30 }],
          pagamentos: [{ formaPagamento: 'DINHEIRO', valor: 30, createdAt: '2026-06-12T10:00:00Z' }],
        }, { status: 201 })
      }),
    )
    renderWithProviders(<Pedidos />)

    await screen.findByText('Nenhum pedido encontrado.')
    await user.click(screen.getByRole('button', { name: 'Novo pedido' }))
    await user.click(await screen.findByRole('button', { name: /Corte/ }))
    await user.selectOptions(screen.getByLabelText('Pagamento'), 'DINHEIRO')
    await user.click(screen.getByRole('button', { name: 'Finalizar venda' }))

    await waitFor(() => expect(payload).toEqual({
      itens: [{ itemId: 'service-1', quantidade: 1 }],
      pagamentos: [{ formaPagamento: 'DINHEIRO', valor: 30 }],
    }))
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Fechar formulário' })).not.toBeInTheDocument())
  })

  it('mantém item inativo no histórico e diferencia venda finalizada de cancelada', async () => {
    const cancelled = {
      id: 'cancelled-sale-1',
      clienteId: null,
      caixaId: 'cash-1',
      dataVenda: '2026-06-10',
      valorTotal: 30,
      status: 'CANCELADA',
      createdAt: '2026-06-10T10:00:00Z',
      itens: [{ itemId: 'service-old', quantidade: 1, precoUnitario: 25, subtotal: 25 }],
      pagamentos: [{ formaPagamento: 'PIX', valor: 30, createdAt: '2026-06-10T10:00:00Z' }],
    }
    installOrderDependencies(() => HttpResponse.json({
      ...emptyPage(0, 1),
      content: [cancelled],
      totalElements: 1,
    }), [item('service-old', 'Serviço antigo', false)])
    renderWithProviders(<Pedidos />)

    expect(await screen.findByText('Serviço antigo')).toBeInTheDocument()
    expect(screen.getByText('Cancelado')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cancelar pedido' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Editar pedido' })).toBeDisabled()
  })

  it('cancela venda somente após confirmação e atualiza o status histórico', async () => {
    const user = userEvent.setup()
    let cancelado = false
    const finalized = {
      id: 'sale-2',
      clienteId: null,
      caixaId: 'cash-1',
      dataVenda: '2026-06-10',
      valorTotal: 30,
      status: 'FINALIZADA',
      createdAt: '2026-06-10T10:00:00Z',
      itens: [{ itemId: 'service-1', quantidade: 1, precoUnitario: 30, subtotal: 30 }],
      pagamentos: [{ formaPagamento: 'PIX', valor: 30, createdAt: '2026-06-10T10:00:00Z' }],
    }
    installOrderDependencies(() => HttpResponse.json({
      ...emptyPage(0, 1),
      content: [{ ...finalized, status: cancelado ? 'CANCELADA' : 'FINALIZADA' }],
      totalElements: 1,
    }))
    server.use(
      http.post(`${API_URL}/vendas/sale-2/cancelar`, () => {
        cancelado = true
        return HttpResponse.json({ ...finalized, status: 'CANCELADA' })
      }),
    )
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderWithProviders(<Pedidos />)

    expect(await screen.findByText('Concluído')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cancelar pedido' }))

    expect(await screen.findByText('Cancelado')).toBeInTheDocument()
    expect(window.confirm).toHaveBeenCalledOnce()
    expect(screen.getByRole('button', { name: 'Cancelar pedido' })).toBeDisabled()
  })
})
