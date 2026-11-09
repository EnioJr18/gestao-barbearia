export type TipoItem = 'SERVICO' | 'PRODUTO'

export interface ItemResponse {
  id: string
  nome: string
  tipo: TipoItem
  preco: number
  estoque: number | null
  ativo: boolean
  createdAt: string
  updatedAt: string
}

export interface CriarItemRequest {
  nome: string
  tipo: TipoItem
  preco: number
  estoque?: number | null
}

export interface CriarProdutoRequest {
  nome: string
  preco: number
  estoque: number
}

export interface CriarServicoRequest {
  nome: string
  preco: number
}

export interface AtualizarItemRequest {
  nome: string
  tipo: TipoItem
  preco: number
  estoque: number | null
}

export interface ListItensParams {
  tipo?: TipoItem
  ativo?: boolean
  nome?: string
}
