import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../../services/api'
import type { CriarVendaRequest, VendaResponse } from './types'

const updateVenda = async ({
  id,
  payload,
}: {
  id: string
  payload: CriarVendaRequest
}): Promise<VendaResponse> => {
  const { data } = await api.put<VendaResponse>(`/vendas/${id}`, payload)
  return data
}

export const useUpdateVenda = () => {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateVenda,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['vendas'] })
      queryClient.invalidateQueries({ queryKey: ['itens'] })
    },
  })
}
