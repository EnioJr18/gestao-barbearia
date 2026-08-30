import { useState, type ReactNode } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { motion } from 'framer-motion'
import { useLocation, useNavigate } from 'react-router-dom'
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
  FiBarChart2,
  FiBox,
  FiChevronLeft,
  FiChevronRight,
  FiDollarSign,
  FiGrid,
  FiMenu,
  FiPlus,
  FiSearch,
  FiShoppingBag,
  FiUsers,
  FiX,
} from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

type View = 'overview' | 'catalog' | 'clients' | 'orders' | 'reports'
type Product = {
  name: string
  type: 'Serviço' | 'Produto'
  price: string
  stock: string
  status: string
}
const chartData = [
  { name: '01', total: 280 },
  { name: '05', total: 420 },
  { name: '10', total: 360 },
  { name: '15', total: 580 },
  { name: '20', total: 490 },
  { name: '25', total: 720 },
  { name: '30', total: 640 },
]
const initialProducts: Product[] = [
  {
    name: 'Corte tradicional',
    type: 'Serviço',
    price: 'R$ 35,00',
    stock: '—',
    status: 'Ativo',
  },
  {
    name: 'Corte + barba',
    type: 'Serviço',
    price: 'R$ 55,00',
    stock: '—',
    status: 'Ativo',
  },
  {
    name: 'Pomada modeladora',
    type: 'Produto',
    price: 'R$ 42,90',
    stock: '18 un.',
    status: 'Ativo',
  },
  {
    name: 'Salgadinho artesanal',
    type: 'Produto',
    price: 'R$ 8,00',
    stock: '32 un.',
    status: 'Ativo',
  },
]
const clients = [
  {
    name: 'Lucas Andrade',
    phone: '(11) 98842-1022',
    visits: 12,
    spent: 'R$ 660,00',
  },
  {
    name: 'Rafael Moreira',
    phone: '(11) 99731-4402',
    visits: 8,
    spent: 'R$ 420,00',
  },
  {
    name: 'Caio Nascimento',
    phone: '(11) 99102-7731',
    visits: 6,
    spent: 'R$ 310,00',
  },
  {
    name: 'Bruno Tavares',
    phone: '(11) 98421-8890',
    visits: 4,
    spent: 'R$ 180,00',
  },
]
const sales = [
  {
    client: 'Lucas Andrade',
    item: 'Corte + barba',
    value: 'R$ 55,00',
    time: 'Hoje, 10:42',
  },
  {
    client: 'Rafael Moreira',
    item: 'Corte tradicional',
    value: 'R$ 35,00',
    time: 'Hoje, 09:18',
  },
  {
    client: 'Caio Nascimento',
    item: 'Pomada modeladora',
    value: 'R$ 42,90',
    time: 'Ontem, 18:30',
  },
]
const productSchema = z.object({
  name: z.string().min(2, 'Informe um nome'),
  price: z.string().min(1, 'Informe o preço'),
  type: z.enum(['Serviço', 'Produto']),
})
type ProductForm = z.infer<typeof productSchema>
const navItems: { id: View; label: string; icon: typeof FiGrid }[] = [
  { id: 'overview', label: 'Visão geral', icon: FiGrid },
  { id: 'catalog', label: 'Produtos e serviços', icon: FiBox },
  { id: 'clients', label: 'Clientes', icon: FiUsers },
  { id: 'orders', label: 'Pedidos', icon: FiShoppingBag },
  { id: 'reports', label: 'Relatórios', icon: FiBarChart2 },
]

export function Home() {
  const [collapsed, setCollapsed] = useState(false)
  const [cashOpen, setCashOpen] = useState(true)
  const [modal, setModal] = useState(false)
  const [products, setProducts] = useState(initialProducts)
  const location = useLocation()
  const navigate = useNavigate()
  const view: View =
    location.pathname === '/produtos'
      ? 'catalog'
      : location.pathname === '/clientes'
        ? 'clients'
        : location.pathname === '/pedidos'
          ? 'orders'
          : location.pathname === '/relatorios'
            ? 'reports'
            : 'overview'
  const setView = (nextView: View) =>
    navigate(
      nextView === 'overview'
        ? '/dashboard'
        : `/${nextView === 'catalog' ? 'produtos' : nextView}`,
    )
  const form = useForm<ProductForm>({
    resolver: zodResolver(productSchema),
    defaultValues: { type: 'Serviço' },
  })
  const selectedType = useWatch({ control: form.control, name: 'type' })
  const activeLabel =
    navItems.find((item) => item.id === view)?.label ?? 'Visão geral'
  function submitProduct(values: ProductForm) {
    setProducts([
      ...products,
      {
        name: values.name,
        type: values.type,
        price: `R$ ${values.price}`,
        stock: values.type === 'Produto' ? '0 un.' : '—',
        status: 'Ativo',
      },
    ])
    form.reset({ type: 'Serviço' })
    setModal(false)
  }
  return (
    <div className="app-shell">
      <aside className={`sidebar ${collapsed ? 'sidebar-collapsed' : ''}`}>
        <div className="brand">
          <div className="brand-mark">C</div>
          {!collapsed && (
            <div>
              <strong>
                Corte<span> &</span> Cia
              </strong>
              <small>gestão inteligente</small>
            </div>
          )}
        </div>
        <button
          className="collapse-toggle"
          onClick={() => setCollapsed(!collapsed)}
          aria-label="Alternar menu"
        >
          {collapsed ? <FiChevronRight /> : <FiChevronLeft />}
        </button>
        {!collapsed && <p className="nav-caption">MENU PRINCIPAL</p>}
        <nav>
          {navItems.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              className={`nav-item ${view === id ? 'active' : ''}`}
              onClick={() => setView(id)}
              title={collapsed ? label : undefined}
            >
              <Icon />
              {!collapsed && <span>{label}</span>}
              {!collapsed && id === 'catalog' && <em>4</em>}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          {!collapsed && (
            <div className="upgrade-card">
              <FiActivity />
              <strong>Seu negócio em movimento</strong>
              <span>Acompanhe seus resultados todos os dias.</span>
            </div>
          )}
          <button className="profile">
            <div className="avatar">MS</div>
            {!collapsed && (
              <div>
                <strong>Marcos Silva</strong>
                <small>Administrador</small>
              </div>
            )}
          </button>
        </div>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <button
            className="mobile-menu"
            onClick={() => setCollapsed(!collapsed)}
          >
            <FiMenu />
          </button>
          <div className="breadcrumb">
            <span>Workspace</span>
            <b>/</b>
            <strong>{activeLabel}</strong>
          </div>
          <div className="top-actions">
            <span className="live-dot"></span>
            <span className="open-label">Operação aberta</span>
            <div className="top-avatar">MS</div>
          </div>
        </header>
        <div className="page-content">
          <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.35 }}
          >
            {view === 'overview' && (
              <Overview
                cashOpen={cashOpen}
                setCashOpen={setCashOpen}
                setView={setView}
              />
            )}
            {view === 'catalog' && (
              <DataPage
                title="Produtos e serviços"
                description="Gerencie tudo o que sua barbearia oferece."
                action="Novo item"
                onAction={() => setModal(true)}
              >
                <Catalog products={products} />
              </DataPage>
            )}
            {view === 'clients' && (
              <DataPage
                title="Clientes"
                description="Conheça quem mantém sua cadeira em movimento."
                action="Novo cliente"
              >
                <ClientTable />
              </DataPage>
            )}
            {view === 'orders' && (
              <DataPage
                title="Pedidos"
                description="Registre vendas e acompanhe o histórico da operação."
                action="Novo pedido"
              >
                <Orders />
              </DataPage>
            )}
            {view === 'reports' && <Reports />}
          </motion.div>
        </div>
      </main>
      {modal && (
        <div className="modal-backdrop" onMouseDown={() => setModal(false)}>
          <div
            className="modal"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button className="modal-close" onClick={() => setModal(false)}>
              <FiX />
            </button>
            <div className="modal-heading">
              <div className="icon-box amber">
                <FiBox />
              </div>
              <div>
                <h2>Novo item</h2>
                <p>Adicione um produto ou serviço ao catálogo.</p>
              </div>
            </div>
            <form onSubmit={form.handleSubmit(submitProduct)}>
              <Label htmlFor="name">Nome do item</Label>
              <Input
                id="name"
                placeholder="Ex.: Corte degradê"
                {...form.register('name')}
              />
              {form.formState.errors.name && (
                <small className="error">
                  {form.formState.errors.name.message}
                </small>
              )}
              <Label htmlFor="price">Preço</Label>
              <Input
                id="price"
                placeholder="35,00"
                {...form.register('price')}
              />
              <Label>Tipo</Label>
              <div className="type-options">
                {['Serviço', 'Produto'].map((type) => (
                  <button
                    type="button"
                    key={type}
                    className={selectedType === type ? 'selected' : ''}
                    onClick={() =>
                      form.setValue('type', type as ProductForm['type'])
                    }
                  >
                    {type}
                  </button>
                ))}
              </div>
              <Button type="submit" className="submit-button">
                <FiPlus /> Adicionar ao catálogo
              </Button>
            </form>
          </div>
        </div>
      )}
    </div>
  )
}

function Overview({
  cashOpen,
  setCashOpen,
  setView,
}: {
  cashOpen: boolean
  setCashOpen: (value: boolean) => void
  setView: (view: View) => void
}) {
  return (
    <>
      <div className="welcome-row">
        <div>
          <p className="eyebrow">QUARTA-FEIRA, 27 DE AGOSTO</p>
          <h1>
            Bom dia, Marcos <span>✦</span>
          </h1>
          <p className="subtitle">Aqui está o resumo da sua barbearia hoje.</p>
        </div>
        <Button onClick={() => setView('orders')}>
          <FiPlus /> Novo pedido
        </Button>
      </div>
      <div className="stats-grid">
        <Stat
          icon={<FiDollarSign />}
          label="Faturamento hoje"
          value="R$ 842,90"
          change="12,5%"
        />
        <Stat
          icon={<FiShoppingBag />}
          label="Pedidos realizados"
          value="24"
          change="8,2%"
        />
        <Stat
          icon={<FiUsers />}
          label="Clientes atendidos"
          value="18"
          change="3,1%"
        />
        <Stat
          icon={<FiActivity />}
          label="Ticket médio"
          value="R$ 46,83"
          change="2,4%"
        />
      </div>
      <div className="content-grid">
        <Card className="chart-card">
          <CardHeader>
            <CardTitle>Faturamento do mês</CardTitle>
            <CardDescription>
              Acompanhe a evolução das suas vendas
            </CardDescription>
            <select className="period-select">
              <option>Agosto 2026</option>
              <option>Julho 2026</option>
            </select>
          </CardHeader>
          <CardContent>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} barSize={22}>
                  <CartesianGrid vertical={false} stroke="#e9eef0" />
                  <XAxis
                    dataKey="name"
                    axisLine={false}
                    tickLine={false}
                    tick={{ fill: '#91a0a3', fontSize: 11 }}
                  />
                  <YAxis
                    axisLine={false}
                    tickLine={false}
                    tick={{ fill: '#91a0a3', fontSize: 11 }}
                    tickFormatter={(value) => `R$${value}`}
                    width={54}
                  />
                  <Tooltip
                    formatter={(value) => [`R$ ${value},00`, 'Faturamento']}
                  />
                  <Bar dataKey="total" fill="#0d6b68" radius={[5, 5, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
        <Card className="cash-card">
          <CardHeader>
            <div className="card-heading-line">
              <div>
                <CardTitle>Caixa do dia</CardTitle>
                <CardDescription>27 de agosto, 2026</CardDescription>
              </div>
              <span className={`status-pill ${cashOpen ? 'green' : 'gray'}`}>
                <i></i>
                {cashOpen ? 'Aberto' : 'Fechado'}
              </span>
            </div>
          </CardHeader>
          <CardContent>
            <div className="cash-total">R$ 842,90</div>
            <p className="muted">Saldo atual</p>
            <div className="cash-lines">
              <div>
                <span>Entradas</span>
                <b className="income">+ R$ 910,00</b>
              </div>
              <div>
                <span>Saídas</span>
                <b className="expense">- R$ 67,10</b>
              </div>
            </div>
            <Button
              variant={cashOpen ? 'outline' : 'default'}
              className="cash-button"
              onClick={() => setCashOpen(!cashOpen)}
            >
              {cashOpen ? 'Fechar caixa' : 'Abrir caixa'}
            </Button>
          </CardContent>
        </Card>
      </div>
      <div className="lower-grid">
        <Card>
          <CardHeader>
            <div className="card-heading-line">
              <div>
                <CardTitle>Últimas vendas</CardTitle>
                <CardDescription>
                  As movimentações mais recentes
                </CardDescription>
              </div>
              <button className="text-button" onClick={() => setView('orders')}>
                Ver todas
              </button>
            </div>
          </CardHeader>
          <CardContent>
            <div className="sales-list">
              {sales.map((sale) => (
                <div className="sale-row" key={sale.time}>
                  <div className="sale-icon">✂</div>
                  <div className="sale-info">
                    <strong>{sale.client}</strong>
                    <span>
                      {sale.item} · {sale.time}
                    </span>
                  </div>
                  <b>{sale.value}</b>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
        <Card className="quick-card">
          <CardHeader>
            <CardTitle>Acesso rápido</CardTitle>
            <CardDescription>O que você deseja fazer?</CardDescription>
          </CardHeader>
          <CardContent>
            <button onClick={() => setView('catalog')}>
              <FiBox />
              <span>
                <strong>Catálogo</strong>
                <small>Produtos e serviços</small>
              </span>
              <FiChevronRight />
            </button>
            <button onClick={() => setView('clients')}>
              <FiUsers />
              <span>
                <strong>Clientes</strong>
                <small>Gerencie sua base</small>
              </span>
              <FiChevronRight />
            </button>
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
  change,
}: {
  icon: ReactNode
  label: string
  value: string
  change: string
}) {
  return (
    <Card className="stat-card">
      <div className="stat-top">
        <div className="stat-icon">{icon}</div>
        <span className="change positive">
          <FiArrowUpRight />
          {change}
        </span>
      </div>
      <p>{label}</p>
      <strong>{value}</strong>
    </Card>
  )
}
function DataPage({
  title,
  description,
  action,
  onAction,
  children,
}: {
  title: string
  description: string
  action: string
  onAction?: () => void
  children: ReactNode
}) {
  return (
    <>
      <div className="welcome-row page-heading">
        <div>
          <p className="eyebrow">GESTÃO</p>
          <h1>{title}</h1>
          <p className="subtitle">{description}</p>
        </div>
        <Button onClick={onAction}>
          <FiPlus /> {action}
        </Button>
      </div>
      {children}
    </>
  )
}
function Catalog({ products }: { products: Product[] }) {
  return (
    <Card className="table-card">
      <div className="table-toolbar">
        <div className="search-field">
          <FiSearch />
          <input placeholder="Buscar no catálogo" />
        </div>
        <select className="filter-select">
          <option>Todos os tipos</option>
          <option>Serviços</option>
          <option>Produtos</option>
        </select>
      </div>
      <table>
        <thead>
          <tr>
            <th>ITEM</th>
            <th>TIPO</th>
            <th>PREÇO</th>
            <th>ESTOQUE</th>
            <th>STATUS</th>
          </tr>
        </thead>
        <tbody>
          {products.map((product) => (
            <tr key={product.name}>
              <td>
                <strong>{product.name}</strong>
              </td>
              <td>
                <span className="type-label">{product.type}</span>
              </td>
              <td>{product.price}</td>
              <td>{product.stock}</td>
              <td>
                <span className="status-pill green">
                  <i></i>
                  {product.status}
                </span>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  )
}
function ClientTable() {
  return (
    <Card className="table-card">
      <div className="table-toolbar">
        <div className="search-field">
          <FiSearch />
          <input placeholder="Buscar cliente" />
        </div>
        <span className="table-count">124 clientes cadastrados</span>
      </div>
      <table>
        <thead>
          <tr>
            <th>CLIENTE</th>
            <th>TELEFONE</th>
            <th>VISITAS</th>
            <th>TOTAL GASTO</th>
            <th>ÚLTIMA VISITA</th>
          </tr>
        </thead>
        <tbody>
          {clients.map((client, index) => (
            <tr key={client.name}>
              <td>
                <div className="client-cell">
                  <div className="small-avatar">
                    {client.name
                      .split(' ')
                      .map((part) => part[0])
                      .join('')}
                  </div>
                  <strong>{client.name}</strong>
                </div>
              </td>
              <td>{client.phone}</td>
              <td>{client.visits}</td>
              <td>
                <strong>{client.spent}</strong>
              </td>
              <td>{index === 0 ? 'Hoje' : `${index + 1} dias atrás`}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </Card>
  )
}
function Orders() {
  return (
    <Card className="table-card">
      <div className="table-toolbar">
        <div className="search-field">
          <FiSearch />
          <input placeholder="Buscar pedido ou cliente" />
        </div>
        <select className="filter-select">
          <option>Este mês</option>
          <option>Últimos 30 dias</option>
        </select>
      </div>
      <table>
        <thead>
          <tr>
            <th>PEDIDO</th>
            <th>CLIENTE</th>
            <th>ITENS</th>
            <th>VALOR</th>
            <th>STATUS</th>
          </tr>
        </thead>
        <tbody>
          {sales
            .concat([
              {
                client: 'Bruno Tavares',
                item: 'Corte tradicional',
                value: 'R$ 35,00',
                time: '26 ago, 16:20',
              },
            ])
            .map((sale, index) => (
              <tr key={sale.client + sale.time}>
                <td>#00{124 + index}</td>
                <td>
                  <strong>{sale.client}</strong>
                </td>
                <td>{sale.item}</td>
                <td>
                  <strong>{sale.value}</strong>
                </td>
                <td>
                  <span className="status-pill green">
                    <i></i>Concluído
                  </span>
                </td>
              </tr>
            ))}
        </tbody>
      </table>
    </Card>
  )
}
function Reports() {
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
          <option>Agosto 2026</option>
          <option>2026</option>
        </select>
      </div>
      <div className="report-stats">
        <Stat
          icon={<FiDollarSign />}
          label="Faturamento no período"
          value="R$ 18.420"
          change="18,4%"
        />
        <Stat
          icon={<FiUsers />}
          label="Novos clientes"
          value="42"
          change="9,8%"
        />
        <Stat
          icon={<FiActivity />}
          label="Serviço mais vendido"
          value="Corte + barba"
          change=""
        />
      </div>
      <Card className="chart-card report-chart">
        <CardHeader>
          <CardTitle>Faturamento por período</CardTitle>
          <CardDescription>
            Visão consolidada dos últimos 30 dias
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="chart-wrap large">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart
                data={chartData.concat([{ name: '31', total: 780 }])}
                barSize={30}
              >
                <CartesianGrid vertical={false} stroke="#e9eef0" />
                <XAxis
                  dataKey="name"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#91a0a3', fontSize: 11 }}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: '#91a0a3', fontSize: 11 }}
                />
                <Bar dataKey="total" fill="#d99536" radius={[5, 5, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>
    </>
  )
}
