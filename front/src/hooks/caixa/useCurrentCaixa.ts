import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CaixaResponse } from './types'

const getCurrentCaixa = async (): Promise<CaixaResponse> => {
  const { data } = await api.get<CaixaResponse>('/caixas/atual')
  return data
}

export const useCurrentCaixa = () =>
  useQuery({
    queryKey: ['caixa', 'atual'],
    queryFn: getCurrentCaixa,
    staleTime: 30_000,
  })
