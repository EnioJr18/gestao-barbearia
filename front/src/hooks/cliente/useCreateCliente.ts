import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ClienteResponse, CriarClienteRequest } from './types'

const createCliente = async (
  payload: CriarClienteRequest,
): Promise<ClienteResponse> => {
  const { data } = await api.post<ClienteResponse>('/clientes', payload)
  return data
}

export const useCreateCliente = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createCliente,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientes'] })
    },
  })
}
