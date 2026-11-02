import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { ItemResponse } from './types'

const inactiveItem = async (id: string): Promise<ItemResponse> => {
  const { data } = await api.patch<ItemResponse>(`/itens/${id}/inativar`)
  return data
}

export const useInactiveItem = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: inactiveItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
