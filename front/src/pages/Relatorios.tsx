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
  FiCalendar,
  FiDollarSign,
  FiShoppingBag,
  FiTrendingUp,
  FiUsers,
} from 'react-icons/fi'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { useAllClientes } from '@/hooks/cliente/useAllClientes'
import { useDashboard } from '@/hooks/dashboard/useDashboard'
import { useAllItens } from '@/hooks/item/useAllItens'
import { useRelatorios } from '@/hooks/relatorio/useRelatorios'
import { useAllVendas } from '@/hooks/venda/useAllVendas'

const monthNames = [
  'Janeiro',
  'Fevereiro',
  'Março',
  'Abril',
  'Maio',
  'Junho',
  'Julho',
  'Agosto',
  'Setembro',
  'Outubro',
  'Novembro',
  'Dezembro',
]

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)

const formatDate = (value: string) =>
  new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))

const dateRange = (year: number, month: number) => {
  const start = new Date(Date.UTC(year, month, 1))
  const end = new Date(Date.UTC(year, month + 1, 0))
  return { start: start.toISOString().slice(0, 10), end: end.toISOString().slice(0, 10) }
}

export function Relatorios() {
  const today = new Date()
  const [month, setMonth] = useState(today.getMonth())
  const [year, setYear] = useState(today.getFullYear())
  const selectedRange = dateRange(year, month)
  const yearRange = { start: `${year}-01-01`, end: `${year}-12-31` }
  const report = useRelatorios(selectedRange.start, selectedRange.end)
  const monthDashboard = useDashboard(selectedRange.start, selectedRange.end)
  const yearDashboard = useDashboard(yearRange.start, yearRange.end)
  const sales = useAllVendas({
    dataInicial: selectedRange.start,
    dataFinal: selectedRange.end,
    status: 'FINALIZADA',
  })
  const clients = useAllClientes({ ativo: true })
  const items = useAllItens()

  const clientNames = useMemo(
    () => new Map((clients.data ?? []).map((client) => [client.id, client.nome])),
    [clients.data],
  )
  const itemNames = useMemo(
    () => new Map((items.data ?? []).map((item) => [item.id, item.nome])),
    [items.data],
  )
  const monthly = useMemo(() => {
    const totals = Array.from({ length: 12 }, () => 0)
    for (const item of yearDashboard.data?.evolucaoFaturamento ?? []) {
      totals[new Date(`${item.data}T00:00:00`).getMonth()] += Number(
        item.faturamento,
      )
    }
    return monthNames.map((name, index) => ({
      month: name.slice(0, 3),
      value: totals[index],
    }))
  }, [yearDashboard.data])
  const daily = useMemo(
    () =>
      Array.from({ length: new Date(year, month + 1, 0).getDate() }, (_, day) => {
        const date = `${year}-${String(month + 1).padStart(2, '0')}-${String(day + 1).padStart(2, '0')}`
        const item = monthDashboard.data?.evolucaoFaturamento.find(
          (entry) => entry.data === date,
        )
        return { day: day + 1, value: Number(item?.faturamento ?? 0) }
      }),
    [monthDashboard.data, month, year],
  )
  const itemTotals = useMemo(
    () =>
      (report.data?.itens ?? []).reduce(
        (totals, item) => {
          totals[item.tipo] += Number(item.faturamento)
          return totals
        },
        { SERVICO: 0, PRODUTO: 0 },
      ),
    [report.data],
  )
  const bestMonth = monthly.reduce(
    (best, item) => (item.value > best.value ? item : best),
    monthly[0],
  )
  const hasError =
    report.error || monthDashboard.error || yearDashboard.error || sales.error

  return (
    <>
      <div className="mb-10 flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-end">
        <div>
          <p className="text-base text-muted-foreground">
            Entenda suas vendas e tome melhores decisões
          </p>
          <h1 className="mt-2 font-heading text-4xl font-bold tracking-tight">
            Relatórios
          </h1>
        </div>
        <div className="flex flex-wrap gap-3">
          <label className="flex items-center gap-2 rounded-xl border bg-background px-4 py-2.5 text-base">
            <FiCalendar />
            <select
              aria-label="Mês do relatório"
              className="bg-transparent outline-none"
              value={month}
              onChange={(event) => setMonth(Number(event.target.value))}
            >
              {monthNames.map((name, index) => (
                <option value={index} key={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          <select
            aria-label="Ano do relatório"
            className="rounded-xl border bg-background px-4 py-2.5 text-base"
            value={year}
            onChange={(event) => setYear(Number(event.target.value))}
          >
            {Array.from({ length: 5 }, (_, index) => today.getFullYear() - index).map(
              (optionYear) => (
                <option value={optionYear} key={optionYear}>
                  {optionYear}
                </option>
              ),
            )}
          </select>
        </div>
      </div>
      {hasError ? (
        <div className="mb-5 rounded-xl border border-red-500/30 bg-red-500/10 p-4 text-base text-red-400">
          Não foi possível carregar todos os dados do relatório.
        </div>
      ) : null}
      <div className="mb-5 grid grid-cols-2 gap-4 xl:grid-cols-4">
        <Stat
          icon={<FiDollarSign />}
          label={`Faturamento em ${monthNames[month]}`}
          value={formatCurrency(Number(report.data?.resumo.faturamento ?? 0))}
        />
        <Stat
          icon={<FiTrendingUp />}
          label={`Faturamento em ${year}`}
          value={formatCurrency(
            monthly.reduce((total, item) => total + item.value, 0),
          )}
        />
        <Stat
          icon={<FiShoppingBag />}
          label="Vendas no mês"
          value={String(report.data?.resumo.quantidadeVendas ?? 0)}
          detail={`Ticket médio ${formatCurrency(Number(report.data?.resumo.faturamento ?? 0) / Math.max(Number(report.data?.resumo.quantidadeVendas ?? 0), 1))}`}
        />
        <Stat
          icon={<FiUsers />}
          label="Melhor mês do ano"
          value={bestMonth.value ? bestMonth.month : '—'}
          detail={formatCurrency(bestMonth.value)}
        />
      </div>
      <div className="mb-5 grid gap-5 lg:grid-cols-[1.55fr_1fr]">
        <ChartCard title={`Faturamento mês a mês • ${year}`} data={monthly} />
        <Card className="min-h-88 rounded-2xl">
          <CardHeader>
            <CardTitle className="text-xl">Serviços × Produtos</CardTitle>
          </CardHeader>
          <CardContent className="flex flex-1 items-end pb-8">
            <div className="w-full space-y-4 text-lg">
              <LegendRow color="bg-primary" label="Serviços" value={itemTotals.SERVICO} />
              <LegendRow color="bg-cyan-400" label="Produtos" value={itemTotals.PRODUTO} />
            </div>
          </CardContent>
        </Card>
      </div>
      <ChartCard title={`Faturamento por dia • ${monthNames[month]}`} data={daily} />
      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <Card className="min-h-80 rounded-2xl">
          <CardHeader>
            <CardTitle className="text-xl">
              Clientes que mais compraram • {monthNames[month]}
            </CardTitle>
          </CardHeader>
          <CardContent>
            {report.data?.clientes.length ? (
              report.data.clientes.slice(0, 5).map((client, index) => (
                <div className="flex items-center gap-4 border-b py-4 last:border-0" key={client.clienteId}>
                  <span className="grid size-10 place-items-center rounded-full bg-primary/15 text-base font-bold text-primary">
                    {index + 1}
                  </span>
                  <strong className="flex-1 text-lg">{client.nome}</strong>
                  <span className="text-lg">{formatCurrency(Number(client.valorTotal))}</span>
                </div>
              ))
            ) : (
              <EmptyState text="Sem compras de clientes identificados neste mês." />
            )}
          </CardContent>
        </Card>
        <Card className="min-h-80 rounded-2xl">
          <CardHeader>
            <CardTitle className="text-xl">Últimas 10 vendas</CardTitle>
          </CardHeader>
          <CardContent>
            {(sales.data ?? []).slice(0, 10).map((sale) => (
              <div className="flex items-center gap-3 border-b py-3 last:border-0" key={sale.id}>
                <span className="w-16 shrink-0 text-sm text-muted-foreground">
                  #{sale.id.slice(0, 6)}
                </span>
                <div className="min-w-0 flex-1">
                  <strong className="block truncate text-base">
                    {sale.clienteId ? clientNames.get(sale.clienteId) ?? 'Cliente' : 'Cliente avulso'}
                  </strong>
                  <span className="block truncate text-sm text-muted-foreground">
                    {sale.itens.map((item) => itemNames.get(item.itemId) ?? 'Item').join(', ')}
                  </span>
                </div>
                <span className="hidden text-sm text-muted-foreground xl:block">
                  {formatDate(sale.createdAt)}
                </span>
                <strong className="text-base">{formatCurrency(Number(sale.valorTotal))}</strong>
              </div>
            ))}
            {!sales.data?.length ? <EmptyState text="Sem vendas neste mês." /> : null}
          </CardContent>
        </Card>
      </div>
      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <Card className="min-h-64 rounded-2xl">
          <CardHeader>
            <CardTitle className="text-xl">Mais vendidos no mês</CardTitle>
          </CardHeader>
          <CardContent>
            {report.data?.itens.length ? (
              report.data.itens.slice(0, 5).map((item) => (
                <div className="flex justify-between border-b py-3 text-lg last:border-0" key={item.itemId}>
                  <span>{item.nome}</span>
                  <strong>{item.quantidade} un.</strong>
                </div>
              ))
            ) : (
              <EmptyState text="Sem vendas neste mês." />
            )}
          </CardContent>
        </Card>
        <Card className="min-h-64 rounded-2xl">
          <CardHeader>
            <CardTitle className="text-xl">Formas de pagamento no mês</CardTitle>
          </CardHeader>
          <CardContent>
            <LegendRow label="Dinheiro" value={Number(report.data?.pagamentos.dinheiro ?? 0)} />
            <LegendRow label="Pix" value={Number(report.data?.pagamentos.pix ?? 0)} />
            <LegendRow label="Débito" value={Number(report.data?.pagamentos.cartaoDebito ?? 0)} />
            <LegendRow label="Crédito" value={Number(report.data?.pagamentos.cartaoCredito ?? 0)} />
          </CardContent>
        </Card>
      </div>
    </>
  )
}

function ChartCard({
  title,
  data,
}: {
  title: string
  data: { month?: string; day?: number; value: number }[]
}) {
  return (
    <Card className="min-h-88 rounded-2xl">
      <CardHeader>
        <CardTitle className="text-xl">{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="h-72">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data}>
              <CartesianGrid vertical={false} stroke="#302b27" />
              <XAxis dataKey={data[0]?.month ? 'month' : 'day'} axisLine={false} tickLine={false} />
              <YAxis axisLine={false} tickLine={false} />
              <Tooltip formatter={(value) => formatCurrency(Number(value))} />
              <Bar dataKey="value" fill="#a88d6d" radius={[6, 6, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  )
}

function LegendRow({
  color,
  label,
  value,
}: {
  color?: string
  label: string
  value: number
}) {
  return (
    <div className="flex items-center gap-3 border-b border-border/50 py-2 last:border-0">
      {color ? <span className={`size-3 rounded-full ${color}`} /> : null}
      <span className="flex-1">{label}</span>
      <strong>{formatCurrency(value)}</strong>
    </div>
  )
}

function EmptyState({ text }: { text: string }) {
  return <p className="py-12 text-center text-base text-muted-foreground">{text}</p>
}

function Stat({
  icon,
  label,
  value,
  detail,
}: {
  icon: React.ReactNode
  label: string
  value: string
  detail?: string
}) {
  return (
    <Card className="min-h-40 gap-4 rounded-2xl p-6">
      <div className="grid size-12 place-items-center rounded-xl bg-primary/10 text-xl text-primary">
        {icon}
      </div>
      <p className="text-lg text-muted-foreground">{label}</p>
      <strong className="font-heading text-3xl">{value}</strong>
      {detail ? <span className="text-sm text-muted-foreground">{detail}</span> : null}
    </Card>
  )
}
