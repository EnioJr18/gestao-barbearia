import { useQuery } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { RelatoriosResponse } from './types'

const getRelatorios = async (
  dataInicial: string,
  dataFinal: string,
): Promise<RelatoriosResponse> => {
  const params = { dataInicial, dataFinal }
  const [resumo, pagamentos, itens, clientes] = await Promise.all([
    api.get('/relatorios/resumo', { params }),
    api.get('/relatorios/pagamentos', { params }),
    api.get('/relatorios/itens', { params }),
    api.get('/relatorios/clientes', { params }),
  ])
  return {
    resumo: resumo.data,
    pagamentos: pagamentos.data,
    itens: itens.data,
    clientes: clientes.data,
  }
}

export const useRelatorios = (dataInicial: string, dataFinal: string) =>
  useQuery({
    queryKey: ['relatorios', dataInicial, dataFinal],
    queryFn: () => getRelatorios(dataInicial, dataFinal),
    staleTime: 30_000,
  })
