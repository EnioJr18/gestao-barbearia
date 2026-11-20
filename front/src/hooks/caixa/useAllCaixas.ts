import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { PageResponse } from '../../types/pagination'
import type { CaixaResponse } from './types'

export interface ListCaixasParams {
  page: number
  size: number
  status?: 'ABERTO' | 'FECHADO'
  dataInicial?: string
  dataFinal?: string
}

const getCaixas = async (
  params: ListCaixasParams,
): Promise<PageResponse<CaixaResponse>> => {
  const { data } = await api.get<PageResponse<CaixaResponse>>('/caixas/historico', {
    params,
  })
  return data
}

export const useAllCaixas = (
  params: ListCaixasParams,
  options?: { enabled?: boolean },
) =>
  useQuery({
    queryKey: ['caixa', 'historico', params],
    queryFn: () => getCaixas(params),
    staleTime: 30_000,
    ...options,
  })
