import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CriarProdutoRequest, ItemResponse } from './types'

const createProduto = async (
  payload: CriarProdutoRequest,
): Promise<ItemResponse> => {
  const { data } = await api.post<ItemResponse>('/itens/produtos', payload)
  return data
}

export const useCreateProduto = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createProduto,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
