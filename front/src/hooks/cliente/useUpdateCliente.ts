import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { AtualizarClienteRequest, ClienteResponse } from './types'

const updateCliente = async ({
  id,
  payload,
}: {
  id: string
  payload: AtualizarClienteRequest
}): Promise<ClienteResponse> => {
  const { data } = await api.put<ClienteResponse>(`/clientes/${id}`, payload)
  return data
}

export const useUpdateCliente = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: updateCliente,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientes'] })
    },
  })
}
