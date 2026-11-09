import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'

const deleteVenda = async (id: string): Promise<void> => {
  await api.delete(`/vendas/${id}`)
}

export const useDeleteVenda = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteVenda,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendas'] })
      queryClient.invalidateQueries({ queryKey: ['itens'] })
    },
  })
}
