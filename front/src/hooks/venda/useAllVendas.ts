import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { PageResponse } from '../../types/pagination'
import type { ListVendasParams, VendaResponse } from './types'

const getVendas = async (
  params?: ListVendasParams,
): Promise<PageResponse<VendaResponse>> => {
  const { data } = await api.get<PageResponse<VendaResponse>>('/vendas', {
    params,
  })
  return data
}

export const useAllVendas = (params?: ListVendasParams) =>
  useQuery({
    queryKey: ['vendas', params ?? {}],
    queryFn: () => getVendas(params),
    staleTime: 30_000,
  })
