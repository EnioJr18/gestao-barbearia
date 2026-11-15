import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { AtualizarItemRequest, ItemResponse } from './types'

const updateItem = async ({
  id,
  payload,
}: {
  id: string
  payload: AtualizarItemRequest
}): Promise<ItemResponse> => {
  const { data } = await api.put<ItemResponse>(`/itens/${id}`, payload)
  return data
}

export const useUpdateItem = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateItem,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
