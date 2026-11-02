import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CaixaResponse, FecharCaixaRequest } from './types'

const closeCaixa = async ({
  id,
  payload,
}: {
  id: string
  payload: FecharCaixaRequest
}): Promise<CaixaResponse> => {
  const { data } = await api.post<CaixaResponse>(
    `/caixas/${id}/fechar`,
    payload,
  )
  return data
}

export const useCloseCaixa = () => {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: closeCaixa,
    onSuccess: async () => {
      queryClient.setQueryData(['caixa', 'atual'], null)
      await queryClient.invalidateQueries({ queryKey: ['caixa'] })
    },
  })
}
