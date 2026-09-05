import { useMutation } from '@tanstack/react-query'
import { api } from '@/services/api'

export interface CreateUserPayload {
	nome: string
	email: string
	senha: string
}

export interface UserResponse {
	id: string
	nome: string
	email: string
}

const createUser = async (payload: CreateUserPayload) => {
	const { data } = await api.post<UserResponse>('/usuarios', payload)
	return data
}

export const useCreateUser = () =>
	useMutation({ mutationFn: createUser })
