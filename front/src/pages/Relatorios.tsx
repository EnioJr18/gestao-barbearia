import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { FiActivity, FiDollarSign, FiUsers } from 'react-icons/fi'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { useRelatorios } from '@/hooks/relatorio/useRelatorios'

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    value,
  )

export function Relatorios() {
  const year = new Date().getFullYear()
  const dataInicial = `${year}-01-01`
  const dataFinal = `${year}-12-31`
  const { data, isLoading, error } = useRelatorios(dataInicial, dataFinal)
  const monthly =
    data?.itens.map((item) => ({
      month: item.nome,
      value: Number(item.faturamento),
    })) ?? []
  return (
    <>
      <div className="welcome-row page-heading">
        <div>
          <p className="eyebrow">ANÁLISE DE DADOS</p>
          <h1>Relatórios</h1>
          <p className="subtitle">
            Entenda o que está fazendo seu negócio crescer.
          </p>
        </div>
        <select className="period-select report-period">
          <option>2026</option>
          <option>Agosto 2026</option>
        </select>
      </div>
      <div className="report-stats">
        <Stat
          icon={<FiDollarSign />}
          label="Faturamento no ano"
          value={formatCurrency(Number(data?.resumo.faturamento ?? 0))}
        />
        <Stat
          icon={<FiUsers />}
          label="Vendas no período"
          value={String(data?.resumo.quantidadeVendas ?? 0)}
        />
        <Stat
          icon={<FiActivity />}
          label="Resultado no ano"
          value={formatCurrency(Number(data?.resumo.resultado ?? 0))}
        />
      </div>
      <div className="content-grid">
        <Card className="chart-card">
          <CardHeader>
            <CardTitle>Faturamento por item</CardTitle>
            <CardDescription>
              Itens com maior faturamento no período
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="chart-wrap large">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthly}>
                  <CartesianGrid vertical={false} stroke="#302b27" />
                  <XAxis dataKey="month" axisLine={false} tickLine={false} />
                  <YAxis axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#d99536" radius={[5, 5, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>Clientes que mais compram</CardTitle>
            <CardDescription>Ranking do período selecionado</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="sales-list">
              {(data?.clientes ?? []).slice(0, 5).map((item, index) => (
                <div className="sale-row" key={item.clienteId}>
                  <div className="small-avatar">{index + 1}</div>
                  <div className="sale-info">
                    <strong>{item.nome}</strong>
                    <span>{item.quantidadeVendas} pedidos no período</span>
                  </div>
                  <b>{formatCurrency(Number(item.valorTotal))}</b>
                </div>
              ))}
              {isLoading && (
                <p className="text-sm text-muted-foreground">
                  Carregando relatório...
                </p>
              )}
              {error && (
                <p className="text-sm text-red-600">
                  Não foi possível carregar o relatório.
                </p>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    </>
  )
}
function Stat({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode
  label: string
  value: string
}) {
  return (
    <Card className="stat-card">
      <div className="stat-icon">{icon}</div>
      <p>{label}</p>
      <strong>{value}</strong>
    </Card>
  )
}
