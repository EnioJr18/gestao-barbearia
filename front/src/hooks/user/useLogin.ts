import { useMutation } from '@tanstack/react-query'
import { api } from '@/services/api'

export interface LoginPayload {
  email: string
  senha: string
}

export interface LoginResponse {
  token: string
  usuario: {
    id: string
    nome: string
    email: string
  }
}

const login = async (payload: LoginPayload) => {
  const { data } = await api.post<LoginResponse>('/auth/login', payload)
  localStorage.setItem('token', data.token)
  localStorage.setItem('usuario', JSON.stringify(data.usuario))
  return data
}

export const useLogin = () => useMutation({ mutationFn: login })
