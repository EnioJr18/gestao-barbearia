import { AxiosError } from 'axios'

interface ApiErrorResponse {
  message?: string
}

export const getApiErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof AxiosError) {
    return (error.response?.data as ApiErrorResponse | undefined)?.message ?? fallback
  }

  return fallback
}
