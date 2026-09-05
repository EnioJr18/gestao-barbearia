import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { AbrirCaixaRequest, CaixaResponse } from './types'

const openCaixa = async (
  payload: AbrirCaixaRequest,
): Promise<CaixaResponse> => {
  const { data } = await api.post<CaixaResponse>('/caixas/abrir', payload)
  return data
}

export const useOpenCaixa = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: openCaixa,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['caixa'] })
    },
  })
}
