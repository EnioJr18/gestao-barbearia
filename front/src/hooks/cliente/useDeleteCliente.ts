import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'

const deleteCliente = async (id: string): Promise<void> => {
  await api.delete(`/clientes/${id}`)
}

export const useDeleteCliente = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteCliente,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['clientes'] }),
  })
}
