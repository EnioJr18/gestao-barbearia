import { useQuery } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { api } from '../../services/api'
import type { CaixaResponse } from './types'

const getCurrentCaixa = async (): Promise<CaixaResponse | null> => {
  try {
    const { data } = await api.get<CaixaResponse>('/caixas/atual')
    return data ?? null
  } catch (error) {
    if (error instanceof AxiosError && error.response?.status === 404) {
      return null
    }

    throw error
  }
}

export const useCurrentCaixa = () =>
  useQuery({
    queryKey: ['caixa', 'atual'],
    queryFn: getCurrentCaixa,
    staleTime: 30_000,
    retry: false,
  })
