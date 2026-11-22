import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { Caixa } from '@/pages/Caixa'
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

const caixaAberto = {
  id: 'cash-1',
  dataCaixa: '2026-06-12',
  valorInicial: 100,
  entradasDinheiro: 0,
  saidasDinheiro: 0,
  dinheiro: 0,
  pix: 0,
  debito: 0,
  credito: 0,
  valorEsperado: 100,
  valorApurado: null,
  diferenca: null,
  status: 'ABERTO',
  usuarioAberturaId: 'user-1',
  usuarioFechamentoId: null,
  abertoEm: '2026-06-12T10:00:00Z',
  fechadoEm: null,
  createdAt: '2026-06-12T10:00:00Z',
  updatedAt: '2026-06-12T10:00:00Z',
}

describe('Caixa', () => {
  it('aplica status e período no histórico, preserva filtros ao paginar e limpa a consulta', async () => {
    const user = userEvent.setup()
    const requests: URL[] = []
    let currentCalls = 0
    server.use(
      http.get(`${API_URL}/caixas/atual`, () => {
        currentCalls += 1
        return new HttpResponse(null, { status: 204 })
      }),
      http.get(`${API_URL}/caixas/historico`, ({ request }) => {
        const url = new URL(request.url)
        requests.push(url)
        const page = Number(url.searchParams.get('page'))
        return HttpResponse.json(emptyPage(page, 2))
      }),
    )
    renderWithProviders(<Caixa />)

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Caixa fechado' })).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Status'), 'FECHADO')
    fireEvent.change(screen.getByLabelText('Data inicial'), { target: { value: '2026-06-01' } })
    fireEvent.change(screen.getByLabelText('Data final'), { target: { value: '2026-06-30' } })

    await waitFor(() => {
      const latest = requests.at(-1)
      expect(latest?.searchParams.get('status')).toBe('FECHADO')
      expect(latest?.searchParams.get('dataInicial')).toBe('2026-06-01')
      expect(latest?.searchParams.get('dataFinal')).toBe('2026-06-30')
      expect(latest?.searchParams.get('page')).toBe('0')
    })

    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get('page')).toBe('1')
      expect(requests.at(-1)?.searchParams.get('status')).toBe('FECHADO')
    })

    await user.click(screen.getByRole('button', { name: 'Limpar filtros' }))
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get('page')).toBe('0')
      expect(requests.at(-1)?.searchParams.has('status')).toBe(false)
      expect(requests.at(-1)?.searchParams.has('dataInicial')).toBe(false)
      expect(requests.at(-1)?.searchParams.has('dataFinal')).toBe(false)
    })
    expect(currentCalls).toBe(1)
  })

  it('abre e fecha caixa usando os valores do formulário e atualiza a sessão atual', async () => {
    const user = userEvent.setup()
    let openPayload: unknown
    let closePayload: unknown
    let currentOpen = false
    server.use(
      http.get(`${API_URL}/caixas/atual`, () =>
        currentOpen
          ? HttpResponse.json(caixaAberto)
          : new HttpResponse(null, { status: 204 }),
      ),
      http.get(`${API_URL}/caixas/historico`, () => HttpResponse.json(emptyPage())),
      http.post(`${API_URL}/caixas/abrir`, async ({ request }) => {
        openPayload = await request.json()
        currentOpen = true
        return HttpResponse.json(caixaAberto, { status: 201 })
      }),
      http.post(`${API_URL}/caixas/cash-1/fechar`, async ({ request }) => {
        closePayload = await request.json()
        currentOpen = false
        return HttpResponse.json({
          ...caixaAberto,
          status: 'FECHADO',
          valorApurado: 100,
          diferenca: 0,
          fechadoEm: '2026-06-12T11:00:00Z',
          usuarioFechamentoId: 'user-1',
        })
      }),
    )
    renderWithProviders(<Caixa />)

    await user.click(await screen.findByRole('button', { name: 'Abrir caixa' }))
    await user.click(screen.getAllByRole('button', { name: 'Abrir caixa' }).at(-1)!)
    await waitFor(() => expect(openPayload).toEqual({ valorInicial: 100 }))

    await user.click(await screen.findByRole('button', { name: 'Fechar caixa' }))
    const countedValue = screen.getByLabelText('Valor contado (R$)')
    await user.clear(countedValue)
    await user.type(countedValue, '10000')
    await user.click(screen.getByRole('button', { name: 'Confirmar fechamento' }))

    await waitFor(() => expect(closePayload).toEqual({ valorApurado: 100 }))
    expect(await screen.findByRole('heading', { name: 'Caixa fechado' })).toBeInTheDocument()
  })
})
