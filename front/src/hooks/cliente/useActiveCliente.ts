import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ClienteResponse } from './types'

const activateCliente = async (id: string): Promise<ClienteResponse> => {
  const { data } = await api.patch<ClienteResponse>(`/clientes/${id}/ativar`)
  return data
}

export const useActiveCliente = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: activateCliente,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientes'] })
    },
  })
}
