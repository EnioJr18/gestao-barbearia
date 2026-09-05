import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CriarVendaRequest, VendaResponse } from './types'

const createVenda = async (
  payload: CriarVendaRequest,
): Promise<VendaResponse> => {
  const { data } = await api.post<VendaResponse>('/vendas', payload)
  return data
}

export const useCreateVenda = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createVenda,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendas'] })
      queryClient.invalidateQueries({ queryKey: ['caixa'] })
    },
  })
}
