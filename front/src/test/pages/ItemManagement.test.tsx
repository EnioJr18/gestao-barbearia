import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { Produtos } from '@/pages/Produtos'
import { Servicos } from '@/pages/Servicos'
import { server } from '../mocks/server'
import { renderWithProviders } from '../utils'

const ITEMS_URL = 'http://localhost:8080/api/v1/itens'

const pageResponse = (page: number, totalPages = 2) => ({
  content: [],
  page,
  size: 10,
  totalElements: totalPages * 10,
  totalPages,
  first: page === 0,
  last: page === totalPages - 1,
})

describe('listagens de serviços e produtos', () => {
  it('busca produtos apenas no envio, preserva o termo entre páginas e limpa o filtro', async () => {
    const user = userEvent.setup()
    const requests: URL[] = []
    server.use(
      http.get(ITEMS_URL, ({ request }) => {
        const url = new URL(request.url)
        requests.push(url)
        return HttpResponse.json(pageResponse(Number(url.searchParams.get('page'))))
      }),
    )
    renderWithProviders(<Produtos />)

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    expect(await screen.findByText('Página 2 de 2')).toBeInTheDocument()

    await user.type(screen.getByPlaceholderText('Buscar produto'), 'Pomada')
    expect(requests).toHaveLength(2)
    expect(screen.getByText('Página 2 de 2')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get('nome')).toBe('Pomada')
      expect(requests.at(-1)?.searchParams.get('page')).toBe('0')
    })
    expect(requests.at(-1)?.searchParams.get('tipo')).toBe('PRODUTO')
    expect(requests.at(-1)?.searchParams.get('size')).toBe('10')
    expect(screen.getByPlaceholderText('Buscar produto')).toHaveValue('Pomada')

    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.get('nome')).toBe('Pomada')
      expect(requests.at(-1)?.searchParams.get('page')).toBe('1')
    })

    await user.clear(screen.getByPlaceholderText('Buscar produto'))
    expect(requests.at(-1)?.searchParams.get('nome')).toBe('Pomada')
    await user.click(screen.getByRole('button', { name: 'Buscar' }))
    await waitFor(() => {
      expect(requests.at(-1)?.searchParams.has('nome')).toBe(false)
      expect(requests.at(-1)?.searchParams.get('page')).toBe('0')
    })
  })

  it('busca serviços com Enter sem consultar a cada caractere', async () => {
    const user = userEvent.setup()
    const requests: URL[] = []
    server.use(http.get(ITEMS_URL, ({ request }) => {
      const url = new URL(request.url)
      requests.push(url)
      return HttpResponse.json(pageResponse(Number(url.searchParams.get('page'))))
    }))
    renderWithProviders(<Servicos />)

    expect(await screen.findByText('Página 1 de 2')).toBeInTheDocument()
    await user.type(screen.getByPlaceholderText('Buscar serviço'), 'Corte')
    expect(requests).toHaveLength(1)
    await user.keyboard('{Enter}')
    await waitFor(() => expect(requests).toHaveLength(2))
    expect(requests.at(-1)?.searchParams.get('nome')).toBe('Corte')
    expect(requests.at(-1)?.searchParams.get('tipo')).toBe('SERVICO')
    expect(requests.at(-1)?.searchParams.get('page')).toBe('0')
    await user.click(screen.getByRole('button', { name: 'Próxima' }))
    await waitFor(() => expect(requests.at(-1)?.searchParams.get('page')).toBe('1'))
    expect(requests.at(-1)?.searchParams.get('nome')).toBe('Corte')
  })

  it('exibe erro e permite tentar novamente ao carregar serviços', async () => {
    let attempts = 0
    let requestedType: string | null = null
    server.use(
      http.get(ITEMS_URL, ({ request }) => {
        attempts += 1
        requestedType = new URL(request.url).searchParams.get('tipo')
        if (attempts === 1) {
          return HttpResponse.json({ message: 'Falha temporária' }, { status: 503 })
        }
        return HttpResponse.json(pageResponse(0, 0))
      }),
    )
    renderWithProviders(<Servicos />)

    expect(await screen.findByText('Não foi possível carregar os serviços.')).toBeInTheDocument()
    await userEvent.setup().click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByText('Nenhum serviço encontrado.')).toBeInTheDocument()
    expect(attempts).toBe(2)
    expect(requestedType).toBe('SERVICO')
  })

  it('cria serviço no endpoint próprio sem enviar estoque', async () => {
    const user = userEvent.setup()
    let requestUrl: string | undefined
    let payload: unknown
    server.use(
      http.get(ITEMS_URL, () => HttpResponse.json(pageResponse(0, 0))),
      http.post(`${ITEMS_URL}/servicos`, async ({ request }) => {
        requestUrl = request.url
        payload = await request.json()
        return HttpResponse.json({
          id: 'servico-1',
          nome: 'Corte',
          tipo: 'SERVICO',
          preco: 30,
          estoque: null,
          ativo: true,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        }, { status: 201 })
      }),
    )
    renderWithProviders(<Servicos />)

    expect(await screen.findByText('Nenhum serviço encontrado.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Novo serviço' }))
    await user.type(screen.getByLabelText('Nome do serviço'), 'Corte')
    await user.type(screen.getByLabelText('Preço'), '3000')
    await user.click(screen.getByRole('button', { name: 'Adicionar serviço' }))

    await waitFor(() => expect(payload).toEqual({ nome: 'Corte', preco: 30 }))
    expect(requestUrl).toBe(`${ITEMS_URL}/servicos`)
    expect(screen.queryByLabelText('Nome do serviço')).not.toBeInTheDocument()
  })

  it('cria produto com estoque inicial, valida estoque negativo e apresenta lista vazia', async () => {
    const user = userEvent.setup()
    let payload: unknown
    server.use(
      http.get(ITEMS_URL, () => HttpResponse.json(pageResponse(0, 0))),
      http.post(`${ITEMS_URL}/produtos`, async ({ request }) => {
        payload = await request.json()
        return HttpResponse.json({
          id: 'produto-1',
          nome: 'Pomada',
          tipo: 'PRODUTO',
          preco: 35,
          estoque: 5,
          ativo: true,
          createdAt: '2026-01-01T00:00:00Z',
          updatedAt: '2026-01-01T00:00:00Z',
        }, { status: 201 })
      }),
    )
    renderWithProviders(<Produtos />)

    expect(await screen.findByText('Nenhum produto encontrado.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Novo produto' }))
    await user.type(screen.getByLabelText('Nome do produto'), 'Pomada')
    await user.type(screen.getByLabelText('Preço'), '3500')
    await user.clear(screen.getByLabelText('Estoque inicial'))
    await user.type(screen.getByLabelText('Estoque inicial'), '-1')
    await user.click(screen.getByRole('button', { name: 'Adicionar produto' }))

    expect(await screen.findByText('Informe um estoque válido')).toBeInTheDocument()
    expect(payload).toBeUndefined()

    await user.clear(screen.getByLabelText('Estoque inicial'))
    await user.type(screen.getByLabelText('Estoque inicial'), '5')
    await user.click(screen.getByRole('button', { name: 'Adicionar produto' }))

    await waitFor(() => expect(payload).toEqual({ nome: 'Pomada', preco: 35, estoque: 5 }))
    await waitFor(() => expect(screen.queryByLabelText('Nome do produto')).not.toBeInTheDocument())
  })
})
