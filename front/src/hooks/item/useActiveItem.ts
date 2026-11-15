import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ItemResponse } from './types'

const activateItem = async (id: string): Promise<ItemResponse> => {
  const { data } = await api.patch<ItemResponse>(`/itens/${id}/ativar`)
  return data
}

export const useActiveItem = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: activateItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
