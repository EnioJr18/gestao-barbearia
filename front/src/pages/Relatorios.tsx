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
const monthly = [
  { month: 'Jan', value: 8200 },
  { month: 'Fev', value: 10400 },
  { month: 'Mar', value: 9700 },
  { month: 'Abr', value: 12600 },
  { month: 'Mai', value: 14100 },
  { month: 'Jun', value: 18420 },
]
const ranking = [
  { name: 'Lucas Andrade', value: 'R$ 660,00' },
  { name: 'Rafael Moreira', value: 'R$ 420,00' },
  { name: 'Caio Nascimento', value: 'R$ 310,00' },
]
export function Relatorios() {
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
          value="R$ 78.420"
        />
        <Stat icon={<FiUsers />} label="Clientes atendidos" value="486" />
        <Stat
          icon={<FiActivity />}
          label="Serviço mais vendido"
          value="Corte + barba"
        />
      </div>
      <div className="content-grid">
        <Card className="chart-card">
          <CardHeader>
            <CardTitle>Faturamento mensal</CardTitle>
            <CardDescription>Comparativo de receita em 2026</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="chart-wrap large">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthly}>
                  <CartesianGrid vertical={false} stroke="#e9eef0" />
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
              {ranking.map((item, index) => (
                <div className="sale-row" key={item.name}>
                  <div className="small-avatar">{index + 1}</div>
                  <div className="sale-info">
                    <strong>{item.name}</strong>
                    <span>{12 - index * 3} pedidos no período</span>
                  </div>
                  <b>{item.value}</b>
                </div>
              ))}
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
