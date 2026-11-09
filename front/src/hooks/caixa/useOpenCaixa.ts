import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
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
    onSuccess: (caixa) => {
      queryClient.setQueryData(['caixa', 'atual'], caixa)
      queryClient.invalidateQueries({ queryKey: ['caixa'] })
      queryClient.invalidateQueries({ queryKey: ['caixa', 'historico'] })
    },
    onError: (error) => {
      if (error instanceof AxiosError && error.response?.status === 409) {
        queryClient.invalidateQueries({ queryKey: ['caixa', 'atual'] })
      }
    },
  })
}
