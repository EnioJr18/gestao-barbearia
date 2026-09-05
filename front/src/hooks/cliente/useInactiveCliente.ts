import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ClienteResponse } from './types'

const deactivateCliente = async (id: string): Promise<ClienteResponse> => {
  const { data } = await api.patch<ClienteResponse>(`/clientes/${id}/inativar`)
  return data
}

export const useInactiveCliente = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: deactivateCliente,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientes'] })
    },
  })
}
