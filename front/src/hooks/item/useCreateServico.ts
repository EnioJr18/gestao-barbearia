import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CriarServicoRequest, ItemResponse } from './types'

const createServico = async (
  payload: CriarServicoRequest,
): Promise<ItemResponse> => {
  const { data } = await api.post<ItemResponse>('/itens/servicos', payload)
  return data
}

export const useCreateServico = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: createServico,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['itens'] }),
  })
}
