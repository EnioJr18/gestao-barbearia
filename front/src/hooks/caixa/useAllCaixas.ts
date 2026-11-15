import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CaixaResponse } from './types'

const getCaixas = async (): Promise<CaixaResponse[]> => {
  const { data } = await api.get<CaixaResponse[]>('/caixas/historico')
  return data
}

export const useAllCaixas = () =>
  useQuery({
    queryKey: ['caixa', 'historico'],
    queryFn: getCaixas,
    staleTime: 30_000,
  })
