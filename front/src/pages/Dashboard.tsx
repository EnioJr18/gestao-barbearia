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
  FiPlus,
  FiShoppingBag,
  FiUsers,
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
import { useCurrentCaixa } from '@/hooks/caixa/useCurrentCaixa'

const chart = [
  { day: '01', value: 280 },
  { day: '05', value: 420 },
  { day: '10', value: 360 },
  { day: '15', value: 580 },
  { day: '20', value: 490 },
  { day: '25', value: 720 },
  { day: '30', value: 640 },
]
const sales = [
  { client: 'Lucas Andrade', item: 'Corte + barba', value: 'R$ 55,00' },
  { client: 'Rafael Moreira', item: 'Corte tradicional', value: 'R$ 35,00' },
  { client: 'Caio Nascimento', item: 'Pomada modeladora', value: 'R$ 42,90' },
]

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  }).format(value)

export function Dashboard() {
  const navigate = useNavigate()
  const {
    data: caixa,
    isLoading: caixaLoading,
    error: caixaError,
  } = useCurrentCaixa()

  const caixaAberta = caixa?.status === 'ABERTO'

  const saldoAtual = caixa ? Number(caixa.valorEsperado ?? 0) : 0
  const entradas = caixa ? Number(caixa.entradasDinheiro ?? 0) : 0
  const saidas = caixa ? Number(caixa.saidasDinheiro ?? 0) : 0

  return (
    <>
      <div className="mb-10 flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-end">
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
            <FiDollarSign />
            Abrir caixa
          </Button>
          <Button
            onClick={() => navigate('/pedidos')}
            className="h-12 rounded-xl px-6 text-lg whitespace-nowrap [&_svg:not([class*='size-'])]:size-6"
          >
            <FiPlus />
            Novo pedido
          </Button>
        </div>
      </div>
      {caixaError && !caixaLoading ? (
        <div className="mb-4 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-700">
          Não foi possível carregar o caixa no momento.
        </div>
      ) : null}
      <div className="mb-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
        <Stat
          icon={<FiDollarSign />}
          label="Faturamento hoje"
          value="R$ 842,90"
        />
        <Stat icon={<FiShoppingBag />} label="Pedidos realizados" value="24" />
        <Stat icon={<FiUsers />} label="Clientes atendidos" value="18" />
        <Stat icon={<FiActivity />} label="Ticket médio" value="R$ 46,83" />
      </div>
      <div className="mb-5 grid gap-5 lg:grid-cols-[1.55fr_1fr]">
        <Card className="min-h-88 rounded-2xl">
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <CardTitle className="text-xl">Faturamento do mês</CardTitle>
              <CardDescription className="text-base">
                Acompanhe a evolução das suas vendas
              </CardDescription>
            </div>
            <select className="rounded-xl border bg-background px-4 py-2.5 text-base text-muted-foreground">
              <option>Agosto 2026</option>
              <option>2026</option>
            </select>
          </CardHeader>
          <CardContent>
            <div className="mt-5 h-72">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chart}>
                  <CartesianGrid vertical={false} stroke="#302b27" />
                  <XAxis dataKey="day" axisLine={false} tickLine={false} />
                  <YAxis axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#efad43" radius={[5, 5, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
        <Card className="min-h-88 rounded-2xl">
          <CardHeader>
            <div className="card-heading-line">
              <div>
                <CardTitle className="text-xl">Caixa do dia</CardTitle>
                <CardDescription className="text-base">
                  {caixa?.dataCaixa
                    ? new Date(caixa.dataCaixa).toLocaleDateString('pt-BR')
                    : 'Carregando data...'}
                </CardDescription>
              </div>
              <span
                className={`inline-flex items-center gap-1 rounded-full px-3.5 py-2 text-base ${caixaAberta ? 'bg-emerald-50 text-emerald-700' : 'bg-muted text-muted-foreground'}`}
              >
                <i className="size-1.5 rounded-full bg-emerald-500" />
                {caixaAberta ? 'Aberto' : 'Fechado'}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="mt-6 font-heading text-5xl font-bold">
              {caixa ? formatCurrency(saldoAtual) : 'R$ 0,00'}
            </div>
            <p className="my-1 mb-8 text-lg text-muted-foreground">
              Saldo atual
            </p>
            <div className="grid gap-5 border-y py-5 text-lg">
              <div>
                <span>Entradas</span>
                <b className="text-emerald-600">
                  + {caixa ? formatCurrency(entradas) : 'R$ 0,00'}
                </b>
              </div>
              <div>
                <span>Saídas</span>
                <b className="text-red-500">
                  - {caixa ? formatCurrency(saidas) : 'R$ 0,00'}
                </b>
              </div>
            </div>
            <Button
              variant="outline"
              className="mt-7 h-12 w-full rounded-xl px-4 text-lg [&_svg:not([class*='size-'])]:size-5"
              onClick={() => navigate('/caixa')}
            >
              {caixaAberta ? 'Ver caixa' : 'Abrir caixa'}
            </Button>
          </CardContent>
        </Card>
      </div>
      <Card className="rounded-2xl shadow-sm">
        <CardHeader>
          <CardTitle className="text-xl">Últimas vendas</CardTitle>
          <CardDescription className="text-base">
            As movimentações mais recentes
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid">
            {sales.map((sale) => (
              <div
                className="flex items-center gap-5 border-b py-5 last:border-0"
                key={sale.client}
              >
                <div className="grid size-12 place-items-center rounded-full bg-emerald-50 text-xl text-primary">
                  ✂
                </div>
                <div className="grid flex-1 gap-0.5">
                  <strong className="text-lg">{sale.client}</strong>
                  <span className="text-base text-muted-foreground">
                    {sale.item}
                  </span>
                </div>
                <b className="text-lg">{sale.value}</b>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
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
    <Card className="stat-card min-h-40 gap-4 rounded-2xl p-6">
      <div className="stat-top">
        <div className="stat-icon size-12 text-xl">{icon}</div>
        <span className="change positive text-sm">12,5%</span>
      </div>
      <p className="text-lg">{label}</p>
      <strong className="text-3xl">{value}</strong>
    </Card>
  )
}
