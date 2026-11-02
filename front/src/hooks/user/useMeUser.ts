import { useQuery } from '@tanstack/react-query'
import { api } from '@/services/api'

export interface MeUserResponse {
  id: string
  nome: string
  email: string
  ativo: boolean
  createdAt: string
  updatedAt: string
}

const getMeUser = async () => {
  const { data } = await api.get<MeUserResponse>('/usuarios/me')
  return data
}

export const useMeUser = () =>
  useQuery({
    queryKey: ['me-user'],
    queryFn: getMeUser,
    enabled: Boolean(localStorage.getItem('token')),
  })
