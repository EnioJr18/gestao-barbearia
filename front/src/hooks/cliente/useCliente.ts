import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ClienteResponse } from './types'

const getClienteById = async (id: string): Promise<ClienteResponse> => {
  const { data } = await api.get<ClienteResponse>(`/clientes/${id}`)
  return data
}

export const useCliente = (id?: string) =>
  useQuery({
    queryKey: ['clientes', id],
    queryFn: () => getClienteById(id!),
    enabled: !!id,
    staleTime: 30_000,
  })
