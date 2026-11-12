export interface DashboardFaturamentoDiario {
  data: string
  faturamento: number
}

export interface DashboardResponse {
  evolucaoFaturamento: DashboardFaturamentoDiario[]
}
