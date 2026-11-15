import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { VendaResponse } from './types'

const cancelVenda = async (id: string): Promise<VendaResponse> => {
  const response = await api.post<VendaResponse>(`/vendas/${id}/cancelar`)
  return response.data
}

export const useCancelVenda = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: cancelVenda,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendas'] })
      queryClient.invalidateQueries({ queryKey: ['itens'] })
      queryClient.invalidateQueries({ queryKey: ['caixa'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['relatorios'] })
    },
  })
}
