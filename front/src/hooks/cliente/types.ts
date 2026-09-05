export interface ClienteResponse {
  id: string
  nome: string
  telefone: string | null
  ativo: boolean
  createdAt: string
  updatedAt: string
}

export interface CriarClienteRequest {
  nome: string
  telefone?: string | null
}

export interface AtualizarClienteRequest {
  nome: string
  telefone?: string | null
}

export interface ListClientesParams {
  nome?: string
  ativo?: boolean
}
