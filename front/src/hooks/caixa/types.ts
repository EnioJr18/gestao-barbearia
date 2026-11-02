export type StatusCaixa = 'ABERTO' | 'FECHADO'

export interface CaixaResponse {
  id: string
  dataCaixa: string
  valorInicial: number
  entradasDinheiro: number
  saidasDinheiro: number
  valorEsperado: number
  valorApurado: number
  diferenca: number
  status: StatusCaixa
  usuarioAberturaId: string
  usuarioFechamentoId: string | null
  abertoEm: string
  fechadoEm: string | null
  createdAt: string
  updatedAt: string
}

export interface AbrirCaixaRequest {
  valorInicial: number
}

export interface FecharCaixaRequest {
  valorApurado: number
}
