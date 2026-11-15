import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'

const deleteItem = async (id: string): Promise<void> => {
  await api.delete(`/itens/${id}`)
}

export const useDeleteItem = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deleteItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
