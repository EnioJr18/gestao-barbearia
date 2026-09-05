import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ClienteResponse, ListClientesParams } from './types'

const getClientes = async (
  params?: ListClientesParams,
): Promise<ClienteResponse[]> => {
  const { data } = await api.get<ClienteResponse[]>('/clientes', { params })
  return data
}

export const useAllClientes = (params?: ListClientesParams) =>
  useQuery({
    queryKey: ['clientes', params ?? {}],
    queryFn: () => getClientes(params),
    staleTime: 30_000,
  })
