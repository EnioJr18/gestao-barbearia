import { renderHook, waitFor } from '@testing-library/react'
import { http, HttpResponse, type JsonBodyType } from 'msw'
import { describe, expect, it } from 'vitest'
import { useAllCaixas } from '@/hooks/caixa/useAllCaixas'
import { useAllItens } from '@/hooks/item/useAllItens'
import { useCompleteItens } from '@/hooks/item/useCompleteItens'
import { useRelatorios } from '@/hooks/relatorio/useRelatorios'
import { useAllVendas } from '@/hooks/venda/useAllVendas'
import { server } from '../mocks/server'
import { createQueryWrapper, createTestQueryClient } from '../utils'

const API_URL = 'http://localhost:8080/api/v1'

const emptyPage = {
  content: [],
  page: 0,
  size: 10,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
}

describe('hooks de consulta', () => {
  it('envia filtros e paginação de itens e separa caches por parâmetros', async () => {
    const requests: URL[] = []
    server.use(
      http.get(`${API_URL}/itens`, ({ request }) => {
        requests.push(new URL(request.url))
        const page = Number(new URL(request.url).searchParams.get('page'))
        return HttpResponse.json({ ...emptyPage, page, size: 10 })
      }),
    )
    const queryClient = createTestQueryClient()
    const wrapper = createQueryWrapper(queryClient)
    const first = renderHook(
      () => useAllItens({ tipo: 'PRODUTO', nome: 'Pomada', ativo: true, page: 0, size: 10 }),
      { wrapper },
    )
    const second = renderHook(
      () => useAllItens({ tipo: 'PRODUTO', nome: 'Pomada', ativo: true, page: 1, size: 10 }),
      { wrapper },
    )

    await waitFor(() => expect(first.result.current.isSuccess && second.result.current.isSuccess).toBe(true))

    expect(requests).toHaveLength(2)
    expect(requests.map((url) => url.searchParams.get('page'))).toEqual(['0', '1'])
    for (const request of requests) {
      expect(request.searchParams.get('tipo')).toBe('PRODUTO')
      expect(request.searchParams.get('nome')).toBe('Pomada')
      expect(request.searchParams.get('ativo')).toBe('true')
      expect(request.searchParams.get('size')).toBe('10')
    }
    first.unmount()
    second.unmount()
    queryClient.clear()
  })

  it('envia período, cliente e status da listagem de vendas', async () => {
    let requestUrl: URL | undefined
    server.use(
      http.get(`${API_URL}/vendas`, ({ request }) => {
        requestUrl = new URL(request.url)
        return HttpResponse.json(emptyPage)
      }),
    )
    const { result } = renderHook(
      () => useAllVendas({
        page: 2,
        size: 10,
        dataInicial: '2026-05-01',
        dataFinal: '2026-05-31',
        cliente: 'cliente-uuid',
        status: 'CANCELADA',
      }),
      { wrapper: createQueryWrapper() },
    )

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(requestUrl?.searchParams.get('page')).toBe('2')
    expect(requestUrl?.searchParams.get('size')).toBe('10')
    expect(requestUrl?.searchParams.get('dataInicial')).toBe('2026-05-01')
    expect(requestUrl?.searchParams.get('dataFinal')).toBe('2026-05-31')
    expect(requestUrl?.searchParams.get('cliente')).toBe('cliente-uuid')
    expect(requestUrl?.searchParams.get('status')).toBe('CANCELADA')
  })

  it('envia status e intervalo de datas ao histórico de caixas', async () => {
    let requestUrl: URL | undefined
    server.use(
      http.get(`${API_URL}/caixas/historico`, ({ request }) => {
        requestUrl = new URL(request.url)
        return HttpResponse.json(emptyPage)
      }),
    )
    const { result } = renderHook(
      () => useAllCaixas({
        page: 1,
        size: 10,
        status: 'FECHADO',
        dataInicial: '2026-05-01',
        dataFinal: '2026-05-31',
      }),
      { wrapper: createQueryWrapper() },
    )

    await waitFor(() => expect(result.current.isSuccess).toBe(true))
    expect(requestUrl?.searchParams.get('status')).toBe('FECHADO')
    expect(requestUrl?.searchParams.get('dataInicial')).toBe('2026-05-01')
    expect(requestUrl?.searchParams.get('dataFinal')).toBe('2026-05-31')
    expect(requestUrl?.searchParams.get('page')).toBe('1')
  })

  it('carrega progressivamente todas as páginas de itens, inclusive itens inativos', async () => {
    const pages: number[] = []
    server.use(
      http.get(`${API_URL}/itens`, ({ request }) => {
        const url = new URL(request.url)
        const page = Number(url.searchParams.get('page'))
        pages.push(page)
        const item = {
          id: `item-${page}`,
          nome: page === 0 ? 'Serviço atual' : 'Produto histórico inativo',
          tipo: page === 0 ? 'SERVICO' : 'PRODUTO',
          preco: 30,
          estoque: page === 0 ? null : 4,
          ativo: page === 0,
          createdAt: '2026-01-01T10:00:00Z',
          updatedAt: '2026-01-01T10:00:00Z',
        }
        return HttpResponse.json({
          ...emptyPage,
          content: [item],
          page,
          size: 100,
          totalElements: 2,
          totalPages: 2,
          first: page === 0,
          last: page === 1,
        })
      }),
    )
    const { result } = renderHook(() => useCompleteItens(), {
      wrapper: createQueryWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(pages).toEqual([0, 1])
    expect(result.current.data?.map((item) => item.nome)).toEqual([
      'Serviço atual',
      'Produto histórico inativo',
    ])
    expect(result.current.data?.[1].ativo).toBe(false)
  })

  it('consulta relatórios para o período e representa corretamente o retorno vazio', async () => {
    const requestedPaths: string[] = []
    server.use(
      http.get(`${API_URL}/relatorios/:relatorio`, ({ request, params }) => {
        requestedPaths.push(String(params.relatorio))
        expect(new URL(request.url).searchParams.get('dataInicial')).toBe('2026-04-01')
        expect(new URL(request.url).searchParams.get('dataFinal')).toBe('2026-04-30')
        const payloads: Record<string, JsonBodyType> = {
          resumo: { faturamento: 0, quantidadeVendas: 0, ticketMedio: 0 },
          pagamentos: [],
          itens: [],
          clientes: [],
        }
        return HttpResponse.json(payloads[String(params.relatorio)])
      }),
    )
    const { result } = renderHook(() => useRelatorios('2026-04-01', '2026-04-30'), {
      wrapper: createQueryWrapper(),
    })

    await waitFor(() => expect(result.current.isSuccess).toBe(true))

    expect(requestedPaths.sort()).toEqual(['clientes', 'itens', 'pagamentos', 'resumo'])
    expect(result.current.data).toMatchObject({
      resumo: { faturamento: 0, quantidadeVendas: 0 },
      pagamentos: [],
      itens: [],
      clientes: [],
    })
  })
})
