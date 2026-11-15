import { useMemo, useState } from 'react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import {
  FiActivity,
  FiDollarSign,
  FiMinusCircle,
  FiPlus,
  FiRefreshCw,
  FiShoppingBag,
} from 'react-icons/fi'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useDashboard } from '@/hooks/dashboard/useDashboard'

type PeriodType = 'month' | 'year'

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)

const formatLocalDate = (value: string) => {
  const [year, month, day] = value.split('-').map(Number)
  return new Intl.DateTimeFormat('pt-BR').format(
    new Date(year, month - 1, day),
  )
}

const currentMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const resolvePeriod = (monthValue: string, type: PeriodType) => {
  const [year, month] = monthValue.split('-').map(Number)
  if (type === 'year') {
    return { dataInicial: `${year}-01-01`, dataFinal: `${year}-12-31` }
  }

  const lastDay = new Date(year, month, 0).getDate()
  return {
    dataInicial: `${year}-${String(month).padStart(2, '0')}-01`,
    dataFinal: `${year}-${String(month).padStart(2, '0')}-${lastDay}`,
  }
}

export function Dashboard() {
  const navigate = useNavigate()
  const [periodType, setPeriodType] = useState<PeriodType>('month')
  const [month, setMonth] = useState(currentMonth)
  const period = useMemo(
    () => resolvePeriod(month, periodType),
    [month, periodType],
  )
  const { data, isLoading, isFetching, error, refetch } = useDashboard(
    period.dataInicial,
    period.dataFinal,
  )

  const caixa = data?.caixaAtual
  const hasOperations = Boolean(
    data &&
      (data.resumo.quantidadeVendas > 0 ||
        data.resumo.despesas !== 0 ||
        data.evolucaoFaturamento.length > 0 ||
        data.itensMaisVendidos.length > 0),
  )
  const chartData =
    data?.evolucaoFaturamento.map((item) => ({
      ...item,
      label: formatLocalDate(item.data),
    })) ?? []

  return (
    <>
      <div className="mb-8 flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <p className="text-base text-muted-foreground">
            {new Date()
              .toLocaleDateString('pt-BR', {
                weekday: 'long',
                day: '2-digit',
                month: 'long',
              })
              .replace(/(^\w)/, (char) => char.toUpperCase())}
          </p>
          <h1 className="mt-2 font-heading text-4xl font-bold tracking-tight text-primary">
            Visão geral
          </h1>
        </div>
        <div className="flex items-center gap-3 self-center sm:self-end">
          <Button
            variant="outline"
            onClick={() => navigate('/caixa')}
            className="h-12 rounded-xl px-6 text-lg whitespace-nowrap [&_svg:not([class*='size-'])]:size-6"
          >
            <FiDollarSign /> Caixa
          </Button>
          <Button
            onClick={() => navigate('/pedidos')}
            className="h-12 rounded-xl px-6 text-lg whitespace-nowrap [&_svg:not([class*='size-'])]:size-6"
          >
            <FiPlus /> Novo pedido
          </Button>
        </div>
      </div>

      <Card className="mb-5 rounded-2xl">
        <CardContent className="flex flex-col gap-4 p-5 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm font-semibold text-muted-foreground">
              PERÍODO CONSULTADO
            </p>
            <p className="mt-1 text-lg font-semibold">
              {data
                ? `${formatLocalDate(data.periodo.dataInicial)} a ${formatLocalDate(data.periodo.dataFinal)}`
                : `${formatLocalDate(period.dataInicial)} a ${formatLocalDate(period.dataFinal)}`}
            </p>
          </div>
          <div className="flex flex-col gap-3 sm:flex-row">
            <label className="grid gap-1 text-sm font-medium text-muted-foreground">
              Referência
              <input
                type="month"
                value={month}
                onChange={(event) => {
                  if (event.target.value) setMonth(event.target.value)
                }}
                className="h-11 rounded-xl border bg-background px-4 text-base text-foreground"
              />
            </label>
            <label className="grid gap-1 text-sm font-medium text-muted-foreground">
              Intervalo
              <select
                value={periodType}
                onChange={(event) =>
                  setPeriodType(event.target.value as PeriodType)
                }
                className="h-11 rounded-xl border bg-background px-4 text-base text-foreground"
              >
                <option value="month">Mês selecionado</option>
                <option value="year">Ano selecionado</option>
              </select>
            </label>
          </div>
        </CardContent>
      </Card>

      {error ? (
        <Card className="rounded-2xl border-red-200">
          <CardContent className="flex min-h-48 flex-col items-center justify-center gap-4 text-center">
            <p className="text-lg text-red-600">
              Não foi possível carregar o Dashboard para este período.
            </p>
            <Button
              variant="outline"
              onClick={() => refetch()}
              disabled={isFetching}
            >
              <FiRefreshCw />
              {isFetching ? 'Tentando novamente...' : 'Tentar novamente'}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="mb-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
            <Stat
              icon={<FiDollarSign />}
              label="Faturamento"
              value={data ? formatCurrency(data.resumo.faturamento) : ''}
              loading={isLoading}
            />
            <Stat
              icon={<FiShoppingBag />}
              label="Vendas finalizadas"
              value={data ? String(data.resumo.quantidadeVendas) : ''}
              loading={isLoading}
            />
            <Stat
              icon={<FiMinusCircle />}
              label="Despesas"
              value={data ? formatCurrency(data.resumo.despesas) : ''}
              loading={isLoading}
            />
            <Stat
              icon={<FiActivity />}
              label="Resultado"
              value={data ? formatCurrency(data.resumo.resultado) : ''}
              loading={isLoading}
            />
          </div>

          {!isLoading && data && !hasOperations && (
            <div className="mb-5 rounded-xl border bg-muted/20 p-4 text-center text-muted-foreground">
              Não há vendas ou despesas registradas no período selecionado.
            </div>
          )}

          <div className="mb-5 grid gap-5 lg:grid-cols-[1.55fr_1fr]">
            <Card className="min-h-88 rounded-2xl">
              <CardHeader>
                <CardTitle className="text-xl">
                  Evolução do faturamento
                </CardTitle>
                <CardDescription className="text-base">
                  Somente vendas finalizadas no período
                </CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <LoadingBlock className="h-72" />
                ) : chartData.length === 0 ? (
                  <EmptyState message="Nenhum faturamento diário disponível neste período." />
                ) : (
                  <div className="mt-5 h-72">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart data={chartData}>
                        <CartesianGrid vertical={false} stroke="#302b27" />
                        <XAxis
                          dataKey="label"
                          axisLine={false}
                          tickLine={false}
                        />
                        <YAxis axisLine={false} tickLine={false} />
                        <Tooltip
                          formatter={(value) =>
                            formatCurrency(Number(value))
                          }
                        />
                        <Bar
                          dataKey="faturamento"
                          name="Faturamento"
                          fill="#efad43"
                          radius={[5, 5, 0, 0]}
                        />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                )}
              </CardContent>
            </Card>

            <Card className="min-h-88 rounded-2xl">
              <CardHeader>
                <CardTitle className="text-xl">Caixa atual</CardTitle>
                <CardDescription className="text-base">
                  Situação financeira do caixa aberto
                </CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <LoadingBlock className="h-64" />
                ) : caixa ? (
                  <>
                    <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-3.5 py-2 text-base text-emerald-700">
                      <i className="size-1.5 rounded-full bg-emerald-500" />
                      Aberto
                    </span>
                    <div className="mt-6 font-heading text-5xl font-bold">
                      {formatCurrency(caixa.valorEsperado)}
                    </div>
                    <p className="my-1 mb-8 text-lg text-muted-foreground">
                      Valor esperado
                    </p>
                    <div className="grid gap-5 border-y py-5 text-lg">
                      <div>
                        <span>Entradas em dinheiro</span>
                        <b className="text-emerald-600">
                          + {formatCurrency(caixa.entradasDinheiro)}
                        </b>
                      </div>
                      <div>
                        <span>Saídas em dinheiro</span>
                        <b className="text-red-500">
                          - {formatCurrency(caixa.saidasDinheiro)}
                        </b>
                      </div>
                    </div>
                  </>
                ) : (
                  <EmptyState message="Não há caixa aberto no momento." />
                )}
                <Button
                  variant="outline"
                  className="mt-7 h-12 w-full rounded-xl px-4 text-lg"
                  onClick={() => navigate('/caixa')}
                >
                  {caixa ? 'Ver caixa' : 'Abrir caixa'}
                </Button>
              </CardContent>
            </Card>
          </div>

          <div className="grid gap-5 lg:grid-cols-2">
            <Card className="rounded-2xl shadow-sm">
              <CardHeader>
                <CardTitle className="text-xl">Itens mais vendidos</CardTitle>
                <CardDescription className="text-base">
                  Ranking por faturamento no período
                </CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <LoadingBlock className="h-48" />
                ) : data?.itensMaisVendidos.length ? (
                  <div className="grid">
                    {data.itensMaisVendidos.map((item) => (
                      <div
                        className="flex items-center gap-5 border-b py-4 last:border-0"
                        key={item.itemId}
                      >
                        <div className="grid size-10 place-items-center rounded-full bg-emerald-50 font-bold text-primary">
                          {item.quantidade}
                        </div>
                        <div className="grid flex-1 gap-0.5">
                          <strong className="text-lg">{item.nome}</strong>
                          <span className="text-sm text-muted-foreground">
                            {item.tipo === 'SERVICO' ? 'Serviço' : 'Produto'}
                          </span>
                        </div>
                        <b className="text-lg">
                          {formatCurrency(item.faturamento)}
                        </b>
                      </div>
                    ))}
                  </div>
                ) : (
                  <EmptyState message="Nenhum item vendido neste período." />
                )}
              </CardContent>
            </Card>

            <Card className="rounded-2xl shadow-sm">
              <CardHeader>
                <CardTitle className="text-xl">
                  Formas de pagamento
                </CardTitle>
                <CardDescription className="text-base">
                  Valores recebidos em vendas finalizadas
                </CardDescription>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <LoadingBlock className="h-48" />
                ) : data ? (
                  <div className="grid gap-4 text-lg">
                    <PaymentRow
                      label="Dinheiro"
                      value={data.pagamentos.dinheiro}
                    />
                    <PaymentRow label="Pix" value={data.pagamentos.pix} />
                    <PaymentRow
                      label="Cartão de crédito"
                      value={data.pagamentos.cartaoCredito}
                    />
                    <PaymentRow
                      label="Cartão de débito"
                      value={data.pagamentos.cartaoDebito}
                    />
                  </div>
                ) : null}
              </CardContent>
            </Card>
          </div>
        </>
      )}
    </>
  )
}

function Stat({
  icon,
  label,
  value,
  loading,
}: {
  icon: React.ReactNode
  label: string
  value: string
  loading: boolean
}) {
  return (
    <Card className="stat-card min-h-40 gap-4 rounded-2xl p-6">
      <div className="stat-top">
        <div className="stat-icon size-12 text-xl">{icon}</div>
      </div>
      <p className="text-lg">{label}</p>
      {loading ? (
        <LoadingBlock className="h-9 w-32" />
      ) : (
        <strong className="text-3xl">{value}</strong>
      )}
    </Card>
  )
}

function PaymentRow({ label, value }: { label: string; value: number }) {
  return (
    <div className="flex items-center justify-between border-b pb-3 last:border-0">
      <span className="text-muted-foreground">{label}</span>
      <strong>{formatCurrency(value)}</strong>
    </div>
  )
}

function EmptyState({ message }: { message: string }) {
  return (
    <div className="grid min-h-40 place-items-center text-center text-base text-muted-foreground">
      {message}
    </div>
  )
}

function LoadingBlock({ className }: { className: string }) {
  return (
    <div
      aria-label="Carregando"
      className={`animate-pulse rounded-xl bg-muted ${className}`}
    />
  )
}
