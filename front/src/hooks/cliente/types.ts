export interface ClienteResponse {
  id: string
  nome: string
  telefone: string | null
  ativo: boolean
  createdAt: string
  updatedAt: string
  visitas: number
  totalGasto: number
  ultimaVisita: string | null
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
