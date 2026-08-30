import { useState } from 'react'
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
  FiArrowUpRight,
  FiDollarSign,
  FiPlus,
  FiShoppingBag,
  FiUsers,
} from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'

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

export function Dashboard() {
  const [open, setOpen] = useState(true)
  return (
    <>
      <div className="mb-8 flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-end">
        <div>
          <p className="text-[11px] font-bold tracking-[0.12em] text-primary">
            QUARTA-FEIRA, 27 DE AGOSTO
          </p>
          <h1 className="mt-2 mb-1 font-heading text-3xl font-bold tracking-tight">
            Bom dia, Marcos <span>✦</span>
          </h1>
          <p className="text-base leading-6 text-muted-foreground">
            Aqui está o resumo da sua barbearia hoje.
          </p>
        </div>
        <Button>
          <FiPlus /> Novo pedido
        </Button>
      </div>
      <div className="mb-4 grid grid-cols-2 gap-3 lg:grid-cols-4">
        <Stat
          icon={<FiDollarSign />}
          label="Faturamento hoje"
          value="R$ 842,90"
        />
        <Stat icon={<FiShoppingBag />} label="Pedidos realizados" value="24" />
        <Stat icon={<FiUsers />} label="Clientes atendidos" value="18" />
        <Stat icon={<FiActivity />} label="Ticket médio" value="R$ 46,83" />
      </div>
      <div className="mb-4 grid gap-4 lg:grid-cols-[1.55fr_1fr]">
        <Card className="min-h-72">
          <CardHeader>
            <CardTitle>Faturamento do mês</CardTitle>
            <CardDescription>
              Acompanhe a evolução das suas vendas
            </CardDescription>
            <select className="float-right -mt-10 rounded-md border bg-background px-3 py-1.5 text-sm text-muted-foreground">
              <option>Agosto 2026</option>
              <option>2026</option>
            </select>
          </CardHeader>
          <CardContent>
            <div className="mt-3 h-60">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chart}>
                  <CartesianGrid vertical={false} stroke="#e9eef0" />
                  <XAxis dataKey="day" axisLine={false} tickLine={false} />
                  <YAxis axisLine={false} tickLine={false} />
                  <Tooltip />
                  <Bar dataKey="value" fill="#0d6b68" radius={[5, 5, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
        <Card className="min-h-72">
          <CardHeader>
            <div className="card-heading-line">
              <div>
                <CardTitle>Caixa do dia</CardTitle>
                <CardDescription>27 de agosto, 2026</CardDescription>
              </div>
              <span
                className={`inline-flex items-center gap-1 rounded-full px-2.5 py-1 text-xs ${open ? 'bg-emerald-50 text-emerald-700' : 'bg-muted text-muted-foreground'}`}
              >
                <i className="size-1.5 rounded-full bg-emerald-500" />
                {open ? 'Aberto' : 'Fechado'}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="mt-4 font-heading text-3xl font-bold">
              R$ 842,90
            </div>
            <p className="my-1 mb-6 text-sm text-muted-foreground">
              Saldo atual
            </p>
            <div className="grid gap-3 border-y py-3 text-sm">
              <div>
                <span>Entradas</span>
                <b className="text-emerald-600">+ R$ 910,00</b>
              </div>
              <div>
                <span>Saídas</span>
                <b className="text-red-500">- R$ 67,10</b>
              </div>
            </div>
            <Button
              variant={open ? 'outline' : 'default'}
              className="mt-5 w-full"
              onClick={() => setOpen(!open)}
            >
              {open ? 'Fechar caixa' : 'Abrir caixa'}
            </Button>
          </CardContent>
        </Card>
      </div>
      <Card className="shadow-sm">
        <CardHeader>
          <CardTitle>Últimas vendas</CardTitle>
          <CardDescription>As movimentações mais recentes</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid">
            {sales.map((sale) => (
              <div
                className="flex items-center gap-3 border-b py-3 last:border-0"
                key={sale.client}
              >
                <div className="grid size-8 place-items-center rounded-full bg-emerald-50 text-primary">
                  ✂
                </div>
                <div className="grid flex-1 gap-0.5">
                  <strong className="text-sm">{sale.client}</strong>
                  <span className="text-xs text-muted-foreground">
                    {sale.item}
                  </span>
                </div>
                <b className="text-sm">{sale.value}</b>
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
    <Card className="stat-card">
      <div className="stat-top">
        <div className="stat-icon">{icon}</div>
        <span className="change positive">
          <FiArrowUpRight />
          12,5%
        </span>
      </div>
      <p>{label}</p>
      <strong>{value}</strong>
    </Card>
  )
}
