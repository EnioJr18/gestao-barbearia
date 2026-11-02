export type FormaPagamento =
  'PIX' | 'DINHEIRO' | 'CARTAO_CREDITO' | 'CARTAO_DEBITO'
export type StatusVenda = 'FINALIZADA' | 'CANCELADA'

export interface VendaResponse {
  id: string
  clienteId: string | null
  caixaId: string
  dataVenda: string
  valorTotal: number
  status: StatusVenda
  createdAt: string
  itens: {
    itemId: string
    quantidade: number
    precoUnitario: number
    subtotal: number
  }[]
  pagamentos: {
    formaPagamento: FormaPagamento
    valor: number
    createdAt: string
  }[]
}

export interface CriarVendaRequest {
  clienteId?: string
  itens: { itemId: string; quantidade: number }[]
  pagamentos: { formaPagamento: FormaPagamento; valor: number }[]
}

export interface ListVendasParams {
  dataInicial?: string
  dataFinal?: string
  cliente?: string
  status?: StatusVenda
}
