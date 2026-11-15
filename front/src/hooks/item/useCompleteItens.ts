import { useQuery } from '@tanstack/react-query'
import type { ItemResponse, ListItensParams } from './types'
import { getItensPage } from './useAllItens'

const MAX_PAGE_SIZE = 100

const getCompleteItens = async (
  params?: Omit<ListItensParams, 'page' | 'size'>,
): Promise<ItemResponse[]> => {
  const firstPage = await getItensPage({
    ...params,
    page: 0,
    size: MAX_PAGE_SIZE,
  })
  const itemsById = new Map(firstPage.content.map((item) => [item.id, item]))

  for (let page = 1; page < firstPage.totalPages; page += 1) {
    const nextPage = await getItensPage({
      ...params,
      page,
      size: MAX_PAGE_SIZE,
    })
    nextPage.content.forEach((item) => {
      if (!itemsById.has(item.id)) itemsById.set(item.id, item)
    })
  }

  return [...itemsById.values()]
}

export const useCompleteItens = (
  params?: Omit<ListItensParams, 'page' | 'size'>,
) =>
  useQuery({
    queryKey: ['itens', 'complete', params ?? {}],
    queryFn: () => getCompleteItens(params),
    staleTime: 30_000,
  })
