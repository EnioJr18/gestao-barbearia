export interface RelatorioResumo {
  faturamento: number
  despesas: number
  resultado: number
  quantidadeVendas: number
}
export interface RelatorioPagamentos {
  dinheiro: number
  pix: number
  cartaoCredito: number
  cartaoDebito: number
}
export interface ItemRelatorio {
  itemId: string
  nome: string
  tipo: 'SERVICO' | 'PRODUTO'
  quantidade: number
  faturamento: number
}
export interface ClienteRelatorio {
  clienteId: string
  nome: string
  quantidadeVendas: number
  valorTotal: number
}
export interface RelatoriosResponse {
  resumo: RelatorioResumo
  pagamentos: RelatorioPagamentos
  itens: ItemRelatorio[]
  clientes: ClienteRelatorio[]
}
