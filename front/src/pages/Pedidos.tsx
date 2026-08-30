import { useState } from 'react'
import { FiPlus, FiSearch, FiShoppingBag } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
const initial = [
  {
    id: '00124',
    client: 'Lucas Andrade',
    item: 'Corte + barba',
    value: 'R$ 55,00',
  },
  {
    id: '00123',
    client: 'Rafael Moreira',
    item: 'Corte tradicional',
    value: 'R$ 35,00',
  },
  {
    id: '00122',
    client: 'Caio Nascimento',
    item: 'Pomada modeladora',
    value: 'R$ 42,90',
  },
]
export function Pedidos() {
  const [orders, setOrders] = useState(initial)
  const [form, setForm] = useState(false)
  const [client, setClient] = useState('')
  return (
    <>
      <div className="mb-8 flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-center">
        <div>
          <p className="text-[11px] font-bold tracking-[0.12em] text-primary">
            OPERAÇÃO
          </p>
          <h1 className="mt-2 mb-1 font-heading text-3xl font-bold tracking-tight">
            Pedidos
          </h1>
          <p className="text-base leading-6 text-muted-foreground">
            Registre vendas e acompanhe o histórico da operação.
          </p>
        </div>
        <Button onClick={() => setForm(!form)}>
          <FiPlus /> Novo pedido
        </Button>
      </div>
      {form && (
        <Card className="mb-4 grid gap-3 p-5 md:grid-cols-[1.4fr_1fr_1fr_auto] md:items-end">
          <div>
            <h2>Novo pedido</h2>
            <p className="subtitle">Escolha o cliente e o item vendido.</p>
          </div>
          <Input
            placeholder="Nome do cliente"
            value={client}
            onChange={(event) => setClient(event.target.value)}
          />
          <select className="filter-select">
            <option>Corte + barba · R$ 55,00</option>
            <option>Corte tradicional · R$ 35,00</option>
            <option>Pomada modeladora · R$ 42,90</option>
          </select>
          <Button
            onClick={() => {
              if (client) {
                setOrders([
                  {
                    id: `${125 + orders.length}`,
                    client,
                    item: 'Corte + barba',
                    value: 'R$ 55,00',
                  },
                  ...orders,
                ])
                setClient('')
                setForm(false)
              }
            }}
          >
            <FiPlus /> Finalizar pedido
          </Button>
        </Card>
      )}
      <Card className="overflow-hidden rounded-xl border shadow-sm">
        <div className="flex flex-col items-stretch justify-between gap-4 border-b bg-muted/20 p-5 sm:flex-row sm:items-center">
          <div className="flex h-10 w-full max-w-sm items-center gap-2 rounded-lg border bg-background px-3 text-muted-foreground shadow-sm focus-within:border-primary">
            <FiSearch />
            <input placeholder="Buscar pedido ou cliente" />
          </div>
          <select className="h-10 rounded-lg border bg-background px-3 text-sm text-muted-foreground">
            <option>Este mês</option>
            <option>Últimos 30 dias</option>
          </select>
        </div>
        <table className="w-full min-w-175 border-collapse">
          <thead className="bg-muted/40">
            <tr>
              <th>PEDIDO</th>
              <th>CLIENTE</th>
              <th>ITEM</th>
              <th>VALOR</th>
              <th>STATUS</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr
                className="border-b transition-colors hover:bg-muted/25 last:border-0"
                key={order.id}
              >
                <td>#{order.id}</td>
                <td>
                  <strong>{order.client}</strong>
                </td>
                <td>{order.item}</td>
                <td>
                  <strong>{order.value}</strong>
                </td>
                <td>
                  <span className="status-pill green">
                    <i />
                    Concluído
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      <div className="order-note">
        <FiShoppingBag /> Os pedidos criados aqui serão vinculados ao caixa
        quando o backend estiver disponível.
      </div>
    </>
  )
}
