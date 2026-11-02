import { useMutation } from '@tanstack/react-query'
import { api } from '@/services/api'

export interface LoginPayload {
  email: string
  senha: string
}

export interface LoginResponse {
  token: string
  tipo: string
}

const login = async (payload: LoginPayload) => {
  const { data } = await api.post<LoginResponse>('/auth/login', payload)
  localStorage.setItem('token', data.token)
  localStorage.setItem('tipoToken', data.tipo)
  return data
}

export const useLogin = () => useMutation({ mutationFn: login })
