import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CriarItemRequest, ItemResponse } from './types'

const createItem = async (payload: CriarItemRequest): Promise<ItemResponse> => {
  const { data } = await api.post<ItemResponse>('/itens', payload)
  return data
}

export const useCreateItem = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
