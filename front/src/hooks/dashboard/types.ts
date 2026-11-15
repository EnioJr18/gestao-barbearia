export type DashboardTipoItem = 'SERVICO' | 'PRODUTO'
export type DashboardStatusCaixa = 'ABERTO' | 'FECHADO'

export interface DashboardPeriodo {
  dataInicial: string
  dataFinal: string
}

export interface DashboardResumo {
  faturamento: number
  despesas: number
  resultado: number
  quantidadeVendas: number
}

export interface DashboardPagamentos {
  dinheiro: number
  pix: number
  cartaoCredito: number
  cartaoDebito: number
}

export interface DashboardCaixaAtual {
  id: string
  status: DashboardStatusCaixa
  valorInicial: number
  entradasDinheiro: number
  saidasDinheiro: number
  valorEsperado: number
  valorApurado: number | null
  diferenca: number | null
}

export interface DashboardItemMaisVendido {
  itemId: string
  nome: string
  tipo: DashboardTipoItem
  quantidade: number
  faturamento: number
}

export interface DashboardFaturamentoDiario {
  data: string
  faturamento: number
}

export interface DashboardResponse {
  periodo: DashboardPeriodo
  resumo: DashboardResumo
  pagamentos: DashboardPagamentos
  caixaAtual: DashboardCaixaAtual | null
  itensMaisVendidos: DashboardItemMaisVendido[]
  evolucaoFaturamento: DashboardFaturamentoDiario[]
}
