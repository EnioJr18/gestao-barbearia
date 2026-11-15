import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { PageResponse } from '../../types/pagination'
import type { ItemResponse, ListItensParams } from './types'

export const getItensPage = async (
  params?: ListItensParams,
): Promise<PageResponse<ItemResponse>> => {
  const { data } = await api.get<PageResponse<ItemResponse>>('/itens', {
    params,
  })
  return data
}

export const useAllItens = (params?: ListItensParams) =>
  useQuery({
    queryKey: ['itens', params ?? {}],
    queryFn: () => getItensPage(params),
    staleTime: 30_000,
  })
