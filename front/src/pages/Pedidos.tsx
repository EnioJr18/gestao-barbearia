import { useMemo, useState } from 'react'
import {
  FiPlus,
  FiSearch,
  FiShoppingBag,
  FiEdit2,
  FiTrash2,
  FiMinus,
  FiX,
} from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { useAllClientes } from '@/hooks/cliente/useAllClientes'
import { useAllItens } from '@/hooks/item/useAllItens'
import { useAllVendas } from '@/hooks/venda/useAllVendas'
import { useCreateVenda } from '@/hooks/venda/useCreateVenda'
import { useUpdateVenda } from '@/hooks/venda/useUpdateVenda'
import { useDeleteVenda } from '@/hooks/venda/useDeleteVenda'
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
  const [editingId, setEditingId] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [clientId, setClientId] = useState('')
  const [cart, setCart] = useState<Record<string, number>>({})
  const [payment, setPayment] = useState<FormaPagamento>('PIX')
  const { data: orders = [], isLoading, error } = useAllVendas()
  const { data: clients = [] } = useAllClientes({ ativo: true })
  const { data: items = [] } = useAllItens({ ativo: true })
  const createVendaMutation = useCreateVenda()
  const updateVendaMutation = useUpdateVenda()
  const deleteVendaMutation = useDeleteVenda()
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
  const cartItems = items.filter((item) => cart[item.id])
  const total = cartItems.reduce(
    (sum, item) => sum + Number(item.preco) * cart[item.id],
    0,
  )

  const addItem = (id: string) =>
    setCart((current) => ({ ...current, [id]: (current[id] ?? 0) + 1 }))
  const removeItem = (id: string) =>
    setCart((current) => {
      const quantity = (current[id] ?? 0) - 1
      if (quantity <= 0) {
        const next = { ...current }
        delete next[id]
        return next
      }
      return { ...current, [id]: quantity }
    })

  const submit = () => {
    if (!cartItems.length || total <= 0) return
    const payload = {
      clienteId: clientId || undefined,
      itens: cartItems.map((item) => ({ itemId: item.id, quantidade: cart[item.id] })),
      pagamentos: [{ formaPagamento: payment, valor: total }],
    }
    const options = { onSuccess: () => { setForm(false); setEditingId(null); setClientId(''); setCart({}); setPayment('PIX') } }
    if (editingId) updateVendaMutation.mutate({ id: editingId, payload }, options)
    else createVendaMutation.mutate(payload, options)
  }

  const editOrder = (order: (typeof orders)[number]) => {
    setEditingId(order.id)
    setClientId(order.clienteId ?? '')
    setCart(Object.fromEntries(order.itens.map((line) => [line.itemId, line.quantidade])))
    setPayment(order.pagamentos[0]?.formaPagamento ?? 'PIX')
    setForm(true)
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
          className="h-12 rounded-xl px-5 text-lg [&_svg:not([class*='size-'])]:size-5"
          onClick={() => setForm(!form)}
        >
          <FiPlus /> Novo pedido
        </Button>
      </div>
      {form && (
        <div
          className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4"
          onMouseDown={() => setForm(false)}
        >
          <Card
            className="max-h-[92vh] w-full max-w-5xl overflow-y-auto rounded-2xl border border-white/40 p-8 shadow-2xl"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <div className="mb-8 flex items-start justify-between">
              <div>
                <h2 className="font-heading text-4xl font-bold">{editingId ? 'Editar pedido' : 'Novo pedido'}</h2>
                <p className="mt-1 text-lg text-muted-foreground">
                  Selecione os serviços e produtos da venda.
                </p>
              </div>
              <button
                type="button"
                aria-label="Fechar formulário"
                className="rounded-lg p-2 text-muted-foreground hover:bg-muted"
                onClick={() => setForm(false)}
              >
                <FiX className="size-5" />
              </button>
            </div>
            <div className="grid gap-10 lg:grid-cols-[1fr_1fr]">
              <div className="space-y-7">
                {(['SERVICO', 'PRODUTO'] as const).map((type) => (
                  <section key={type}>
                    <h3 className="mb-3 text-lg font-bold uppercase tracking-wider text-muted-foreground">
                      {type === 'SERVICO' ? 'Serviços' : 'Produtos'}
                    </h3>
                    <div className="grid grid-cols-2 gap-4">
                      {items
                        .filter((item) => item.tipo === type)
                        .map((item) => (
                          <button
                            type="button"
                            key={item.id}
                            onClick={() => addItem(item.id)}
                            className="rounded-xl bg-muted/80 p-4 text-left transition-colors hover:bg-primary/15"
                          >
                            <span className="block text-xl font-semibold">
                              {item.nome}
                            </span>
                            <span className="text-lg text-primary">
                              {formatCurrency(Number(item.preco))}
                            </span>
                          </button>
                        ))}
                    </div>
                  </section>
                ))}
              </div>
              <div className="space-y-6">
                <label className="grid gap-2 text-lg font-semibold">
                  Cliente
                  <select
                    value={clientId}
                    onChange={(event) => setClientId(event.target.value)}
                    className="h-12 rounded-xl border bg-background px-3 text-lg font-normal"
                  >
                    <option value="">Cliente avulso</option>
                    {clients.map((client) => (
                      <option key={client.id} value={client.id}>
                        {client.nome}
                      </option>
                    ))}
                  </select>
                </label>
                <div className="rounded-xl border p-4">
                  {cartItems.length ? (
                    cartItems.map((item) => (
                      <div
                        className="flex items-center gap-4 border-b py-4 last:border-0"
                        key={item.id}
                      >
                        <span className="min-w-0 flex-1 truncate text-xl font-semibold">
                          {item.nome}
                        </span>
                        <button
                          type="button"
                          aria-label={`Remover ${item.nome}`}
                          onClick={() => setCart((current) => {
                            const next = { ...current }
                            delete next[item.id]
                            return next
                          })}
                        >
                          <FiTrash2 className="size-5 text-muted-foreground" />
                        </button>
                        <button type="button" onClick={() => removeItem(item.id)}>
                          <FiMinus className="size-5" />
                        </button>
                        <span className="w-8 text-center text-2xl font-bold text-primary">
                          {cart[item.id]}
                        </span>
                        <button type="button" onClick={() => addItem(item.id)}>
                          <FiPlus className="size-5" />
                        </button>
                        <strong className="w-28 text-right text-lg">
                          {formatCurrency(Number(item.preco) * cart[item.id])}
                        </strong>
                      </div>
                    ))
                  ) : (
                    <p className="py-6 text-center text-lg text-muted-foreground">
                      Adicione itens ao pedido.
                    </p>
                  )}
                </div>
                <div className="grid gap-3">
                  <label className="grid gap-2 text-lg font-semibold">
                    Pagamento
                    <select
                      value={payment}
                      onChange={(event) =>
                        setPayment(event.target.value as FormaPagamento)
                      }
                      className="h-12 rounded-xl border bg-background px-3 text-lg font-normal"
                    >
                      {Object.entries(paymentLabels).map(([value, label]) => (
                        <option key={value} value={value}>
                          {label}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <div className="flex items-center justify-between rounded-xl bg-muted p-5">
                  <span className="text-lg">Total</span>
                  <strong className="text-4xl text-primary">
                    {formatCurrency(total)}
                  </strong>
                </div>
                {createVendaMutation.error && (
                  <p className="text-base text-red-600">
                    Não foi possível criar o pedido. Verifique se o caixa está aberto.
                  </p>
                )}
                <div className="flex justify-end gap-3">
                  <Button
                    type="button"
                    variant="outline"
                    className="h-12 px-6 text-lg"
                    onClick={() => setForm(false)}
                  >
                    Cancelar
                  </Button>
                  <Button
                    type="button"
                    className="h-12 px-6 text-lg"
                    onClick={submit}
                    disabled={createVendaMutation.isPending || !cartItems.length}
                  >
                    {createVendaMutation.isPending || updateVendaMutation.isPending ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Finalizar venda'}
                  </Button>
                </div>
              </div>
            </div>
          </Card>
        </div>
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
                <th>AÇÕES</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                <tr>
                  <td colSpan={7} className="text-center">
                    Carregando pedidos...
                  </td>
                </tr>
              ) : error ? (
                <tr>
                  <td colSpan={7} className="text-center text-red-600">
                    Não foi possível carregar os pedidos.
                  </td>
                </tr>
              ) : visibleOrders.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center">
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
                        className={`status-pill px-3 py-1.5 text-sm font-semibold ${order.status === 'FINALIZADA' ? 'green' : 'gray'}`}
                      >
                        {order.status === 'FINALIZADA'
                          ? 'Concluído'
                          : 'Cancelado'}
                      </span>
                    </td>
                    <td>
                      <div className="flex items-center gap-2">
                        <button type="button" title="Editar" aria-label="Editar pedido" disabled={order.status !== 'FINALIZADA'} onClick={() => editOrder(order)} className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-amber-500/15 hover:text-amber-300"><FiEdit2 /></button>
                        <button type="button" title="Excluir" aria-label="Excluir pedido" disabled={order.status !== 'FINALIZADA' || deleteVendaMutation.isPending} onClick={() => window.confirm(`Excluir o pedido #${order.id.slice(0, 8)}?`) && deleteVendaMutation.mutate(order.id)} className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-red-500/15 hover:text-red-300"><FiTrash2 /></button>
                      </div>
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
