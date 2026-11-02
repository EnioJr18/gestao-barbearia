import { useMemo, useState } from 'react'
import { FiPlus, FiSearch, FiShoppingBag } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAllClientes } from '@/hooks/cliente/useAllClientes'
import { useAllItens } from '@/hooks/item/useAllItens'
import { useAllVendas } from '@/hooks/venda/useAllVendas'
import { useCreateVenda } from '@/hooks/venda/useCreateVenda'
import type { FormaPagamento } from '@/hooks/venda/types'

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    value,
  )
const paymentLabels: Record<FormaPagamento, string> = {
  PIX: 'Pix',
  DINHEIRO: 'Dinheiro',
  CARTAO_CREDITO: 'Cartão de crédito',
  CARTAO_DEBITO: 'Cartão de débito',
}

export function Pedidos() {
  const [form, setForm] = useState(false)
  const [search, setSearch] = useState('')
  const [clientId, setClientId] = useState('')
  const [itemId, setItemId] = useState('')
  const [quantity, setQuantity] = useState(1)
  const [payment, setPayment] = useState<FormaPagamento>('PIX')
  const { data: orders = [], isLoading, error } = useAllVendas()
  const { data: clients = [] } = useAllClientes({ ativo: true })
  const { data: items = [] } = useAllItens({ ativo: true })
  const createVendaMutation = useCreateVenda()
  const itemById = useMemo(
    () => new Map(items.map((item) => [item.id, item])),
    [items],
  )
  const clientById = useMemo(
    () => new Map(clients.map((client) => [client.id, client])),
    [clients],
  )
  const visibleOrders = orders.filter((order) => {
    const client = order.clienteId
      ? clientById.get(order.clienteId)?.nome
      : 'Cliente avulso'
    return `${client} ${order.id}`.toLowerCase().includes(search.toLowerCase())
  })
  const selectedItem = itemById.get(itemId)
  const total = selectedItem ? Number(selectedItem.preco) * quantity : 0

  const submit = () => {
    if (!itemId || quantity < 1 || total <= 0) return
    createVendaMutation.mutate(
      {
        clienteId: clientId || undefined,
        itens: [{ itemId, quantidade: quantity }],
        pagamentos: [{ formaPagamento: payment, valor: total }],
      },
      {
        onSuccess: () => {
          setForm(false)
          setClientId('')
          setItemId('')
          setQuantity(1)
        },
      },
    )
  }

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
        <Button
          size="lg"
          className="h-10 px-4 text-sm [&_svg:not([class*='size-'])]:size-4"
          onClick={() => setForm(!form)}
        >
          <FiPlus /> Novo pedido
        </Button>
      </div>
      {form && (
        <Card className="mb-4 grid gap-3 p-5 md:grid-cols-4 md:items-end">
          <div>
            <h2 className="font-heading text-3xl font-bold">Novo pedido</h2>
            <p className="text-md text-muted-foreground">
              Escolha cliente, item e pagamento.
            </p>
          </div>
          <select
            value={clientId}
            onChange={(event) => setClientId(event.target.value)}
            className="h-12 rounded-lg border bg-background px-3 text-lg"
          >
            <option value="">Cliente avulso</option>
            {clients.map((client) => (
              <option key={client.id} value={client.id}>
                {client.nome}
              </option>
            ))}
          </select>
          <select
            value={itemId}
            onChange={(event) => setItemId(event.target.value)}
            className="h-12 rounded-lg border bg-background px-3 text-lg"
          >
            <option value="">Selecione o item</option>
            {items.map((item) => (
              <option key={item.id} value={item.id}>
                {item.nome} · {formatCurrency(Number(item.preco))}
              </option>
            ))}
          </select>
          <div className="flex gap-2">
            <Input
              type="number"
              min="1"
              value={quantity}
              onChange={(event) => setQuantity(Number(event.target.value))}
              className="h-12 w-24 text-lg"
            />
            <select
              value={payment}
              onChange={(event) =>
                setPayment(event.target.value as FormaPagamento)
              }
              className="h-12 min-w-0 flex-1 rounded-lg border bg-background px-2 text-lg"
            >
              {Object.entries(paymentLabels).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </div>
          <Button
            size="lg"
            className="h-12 px-4 text-lg [&_svg:not([class*='size-'])]:size-4 md:col-span-4"
            onClick={submit}
            disabled={createVendaMutation.isPending || !itemId}
          >
            {createVendaMutation.isPending
              ? 'Salvando...'
              : `Finalizar pedido${total ? ` · ${formatCurrency(total)}` : ''}`}
          </Button>
          {createVendaMutation.error && (
            <p className="md:col-span-4 text-sm text-red-600">
              Não foi possível criar o pedido. Verifique se o caixa está aberto.
            </p>
          )}
        </Card>
      )}
      <Card className="overflow-hidden rounded-xl border shadow-sm">
        <div className="flex flex-col items-stretch justify-between gap-4 border-b bg-muted/20 p-5 text-lg sm:flex-row sm:items-center">
          <div className="flex h-10 w-full max-w-sm items-center gap-2 rounded-lg border bg-background px-3 text-muted-foreground">
            <FiSearch />
            <input
              placeholder="Buscar pedido ou cliente"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              className="w-full bg-transparent text-lg outline-none"
            />
          </div>
          <span className="text-sm text-muted-foreground">
            {visibleOrders.length} pedidos
          </span>
        </div>
        <div className="overflow-x-auto">
          <table className="text-lg [&_td]:text-lg [&_th]:text-sm">
            <thead>
              <tr>
                <th>PEDIDO</th>
                <th>CLIENTE</th>
                <th>ITEM</th>
                <th>VALOR</th>
                <th>PAGAMENTO</th>
                <th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={6} className="text-center">
                    Carregando pedidos...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={6} className="text-center text-red-600">
                    Não foi possível carregar os pedidos.
                  </td>
                </tr>
              ) : visibleOrders.length === 0 ? (
                <tr>
                  <td colSpan={6} className="text-center">
                    Nenhum pedido encontrado.
                  </td>
                </tr>
              ) : (
                visibleOrders.map((order) => (
                  <tr key={order.id}>
                    <td>#{order.id.slice(0, 8)}</td>
                    <td>
                      <strong>
                        {order.clienteId
                          ? (clientById.get(order.clienteId)?.nome ?? 'Cliente')
                          : 'Cliente avulso'}
                      </strong>
                    </td>
                    <td>
                      {order.itens
                        .map(
                          (line) => itemById.get(line.itemId)?.nome ?? 'Item',
                        )
                        .join(', ')}
                    </td>
                    <td>
                      <strong>
                        {formatCurrency(Number(order.valorTotal))}
                      </strong>
                    </td>
                    <td>
                      {order.pagamentos
                        .map((line) => paymentLabels[line.formaPagamento])
                        .join(', ')}
                    </td>
                    <td>
                      <span
                        className={`status-pill ${order.status === 'FINALIZADA' ? 'green' : 'gray'}`}
                      >
                        {order.status === 'FINALIZADA'
                          ? 'Concluído'
                          : 'Cancelado'}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </Card>
      <div className="order-note">
        <FiShoppingBag /> Os pedidos são vinculados ao caixa aberto pelo
        backend.
      </div>
    </>
  )
}
