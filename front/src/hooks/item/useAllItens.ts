import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ItemResponse, ListItensParams } from './types'

const getItens = async (params?: ListItensParams): Promise<ItemResponse[]> => {
  const { data } = await api.get<ItemResponse[]>('/itens', { params })
  return data
}

export const useAllItens = (params?: ListItensParams) =>
  useQuery({
    queryKey: ['itens', params ?? {}],
    queryFn: () => getItens(params),
    staleTime: 30_000,
  })
