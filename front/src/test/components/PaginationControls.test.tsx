import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { PaginationControls } from '@/components/PaginationControls'
import { renderWithProviders } from '../utils'

describe('PaginationControls', () => {
  it('mostra a página iniciada em zero para o usuário e a quantidade total', () => {
    renderWithProviders(
      <PaginationControls
        page={1}
        totalPages={4}
        totalElements={35}
        first={false}
        last={false}
        onPageChange={vi.fn()}
      />,
    )

    expect(screen.getByText('Página 2 de 4')).toBeInTheDocument()
    expect(screen.getByText('35 registros')).toBeInTheDocument()
  })

  it('desabilita anterior na primeira página e próxima na última', () => {
    const onPageChange = vi.fn()
    const { rerender } = renderWithProviders(
      <PaginationControls
        page={0}
        totalPages={2}
        totalElements={11}
        first
        last={false}
        onPageChange={onPageChange}
      />,
    )

    expect(screen.getByRole('button', { name: 'Anterior' })).toBeDisabled()
    expect(screen.getByRole('button', { name: 'Próxima' })).toBeEnabled()

    rerender(
      <PaginationControls
        page={1}
        totalPages={2}
        totalElements={11}
        first={false}
        last
        onPageChange={onPageChange}
      />,
    )

    expect(screen.getByRole('button', { name: 'Anterior' })).toBeEnabled()
    expect(screen.getByRole('button', { name: 'Próxima' })).toBeDisabled()
  })

  it('navega para as páginas anterior e seguinte com índices zero-based', () => {
    const onPageChange = vi.fn()
    renderWithProviders(
      <PaginationControls
        page={1}
        totalPages={3}
        totalElements={25}
        onPageChange={onPageChange}
      />,
    )

    fireEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    fireEvent.click(screen.getByRole('button', { name: 'Próxima' }))

    expect(onPageChange).toHaveBeenNthCalledWith(1, 0)
    expect(onPageChange).toHaveBeenNthCalledWith(2, 2)
  })

  it('não renderiza controles quando a resposta não tem páginas', () => {
    const { container } = renderWithProviders(
      <PaginationControls
        page={0}
        totalPages={0}
        totalElements={0}
        onPageChange={vi.fn()}
      />,
    )

    expect(container).toBeEmptyDOMElement()
  })
})
