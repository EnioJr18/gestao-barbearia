import axios from 'axios'

export const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')

  if (token) {
    config.headers = config.headers ?? {}
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const requestUrl = error.config?.url ?? ''
    const isAuthenticationRequest = requestUrl.includes('/auth/login')

    if (status === 401 && !isAuthenticationRequest) {
      localStorage.removeItem('token')
      localStorage.removeItem('tipoToken')
      window.dispatchEvent(new Event('auth:logout'))

      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }

    return Promise.reject(error)
  },
)
