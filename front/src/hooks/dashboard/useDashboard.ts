import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { DashboardResponse } from './types'

const getDashboard = async (
  dataInicial: string,
  dataFinal: string,
): Promise<DashboardResponse> => {
  const { data } = await api.get<DashboardResponse>('/dashboard', {
    params: { dataInicial, dataFinal },
  })
  return data
}

export const useDashboard = (dataInicial: string, dataFinal: string) =>
  useQuery({
    queryKey: ['dashboard', dataInicial, dataFinal],
    queryFn: () => getDashboard(dataInicial, dataFinal),
    staleTime: 30_000,
  })
