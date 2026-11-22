import { useMemo, useState } from 'react'
import {
  FiPlus,
  FiShoppingBag,
  FiEdit2,
  FiTrash2,
  FiMinus,
  FiX,
  FiXCircle,
  FiSearch,
} from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { PaginationControls } from '@/components/PaginationControls'
import { useAllClientes } from '@/hooks/cliente/useAllClientes'
import { useCompleteItens } from '@/hooks/item/useCompleteItens'
import { useAllVendas } from '@/hooks/venda/useAllVendas'
import { useCreateVenda } from '@/hooks/venda/useCreateVenda'
import { useUpdateVenda } from '@/hooks/venda/useUpdateVenda'
import { useCancelVenda } from '@/hooks/venda/useCancelVenda'
import type { FormaPagamento, StatusVenda } from '@/hooks/venda/types'
import { getApiErrorMessage } from '@/services/apiError'

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
  const [page, setPage] = useState(0)
  const [dataInicial, setDataInicial] = useState('')
  const [dataFinal, setDataFinal] = useState('')
  const [clienteFiltro, setClienteFiltro] = useState('')
  const [statusFiltro, setStatusFiltro] = useState<StatusVenda | ''>('')
  const [searchInput, setSearchInput] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [clientId, setClientId] = useState('')
  const [cart, setCart] = useState<Record<string, number>>({})
  const [historicalPrices, setHistoricalPrices] = useState<Record<string, number>>({})
  const [payment, setPayment] = useState<FormaPagamento>('PIX')
  const {
    data,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useAllVendas({
    page,
    size: 10,
    dataInicial: dataInicial || undefined,
    dataFinal: dataFinal || undefined,
    cliente: clienteFiltro || undefined,
    clienteNome: appliedSearch || undefined,
    status: statusFiltro || undefined,
  })
  const orders = data?.content ?? []
  const currentPage = data?.page ?? page
  const { data: clients = [] } = useAllClientes()
  const {
    data: items = [],
    isLoading: itemsLoading,
    error: itemsError,
  } = useCompleteItens()
  const createVendaMutation = useCreateVenda()
  const updateVendaMutation = useUpdateVenda()
  const cancelVendaMutation = useCancelVenda()
  const itemById = useMemo(
    () => new Map(items.map((item) => [item.id, item])),
    [items],
  )
  const activeItems = useMemo(() => items.filter((item) => item.ativo), [items])
  const clientById = useMemo(
    () => new Map(clients.map((client) => [client.id, client])),
    [clients],
  )
  const selectableClients = useMemo(
    () => clients.filter((client) => client.ativo || client.id === clientId),
    [clients, clientId],
  )
  const cartItems = items.filter((item) => cart[item.id])
  const total = cartItems.reduce(
    (sum, item) =>
      sum + (historicalPrices[item.id] ?? Number(item.preco)) * cart[item.id],
    0,
  )

  const addItem = (id: string) => {
    if (!itemById.get(id)?.ativo) return
    setCart((current) => ({ ...current, [id]: (current[id] ?? 0) + 1 }))
  }
  const removeCartItem = (id: string) => {
    setCart((current) => {
      const next = { ...current }
      delete next[id]
      return next
    })
  }

  const removeItem = (id: string) => {
    if ((cart[id] ?? 0) <= 1) {
      removeCartItem(id)
      return
    }
    setCart((current) => ({ ...current, [id]: current[id] - 1 }))
  }

  const openNewOrder = () => {
    setEditingId(null)
    setClientId('')
    setCart({})
    setHistoricalPrices({})
    setPayment('PIX')
    setForm(true)
  }

  const submit = () => {
    if (!cartItems.length || total <= 0) return
    const payload = {
      clienteId: clientId || undefined,
      itens: cartItems.map((item) => ({ itemId: item.id, quantidade: cart[item.id] })),
      pagamentos: [{ formaPagamento: payment, valor: total }],
    }
    const options = { onSuccess: () => { setForm(false); setEditingId(null); setClientId(''); setCart({}); setHistoricalPrices({}); setPayment('PIX') } }
    if (editingId) updateVendaMutation.mutate({ id: editingId, payload }, options)
    else createVendaMutation.mutate(payload, options)
  }

  const editOrder = (order: (typeof orders)[number]) => {
    setEditingId(order.id)
    setClientId(order.clienteId ?? '')
    setCart(Object.fromEntries(order.itens.map((line) => [line.itemId, line.quantidade])))
    setHistoricalPrices(Object.fromEntries(order.itens.map((line) => [line.itemId, Number(line.precoUnitario)])))
    setPayment(order.pagamentos[0]?.formaPagamento ?? 'PIX')
    setForm(true)
  }

  const clearFilters = () => {
    setSearchInput('')
    setAppliedSearch('')
    setDataInicial('')
    setDataFinal('')
    setClienteFiltro('')
    setStatusFiltro('')
    setPage(0)
  }

  const changeStatusFilter = (value: string) => {
    if (value === '' || value === 'FINALIZADA' || value === 'CANCELADA') {
      setStatusFiltro(value)
      setPage(0)
    }
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
          onClick={openNewOrder}
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
                {itemsLoading ? (
                  <p className="text-lg text-muted-foreground">Carregando itens...</p>
                ) : itemsError ? (
                  <p className="text-lg text-red-600">Não foi possível carregar os itens.</p>
                ) : activeItems.length === 0 ? (
                  <p className="text-lg text-muted-foreground">Nenhum item ativo disponível.</p>
                ) : (
                  (['SERVICO', 'PRODUTO'] as const).map((type) => (
                    <section key={type}>
                      <h3 className="mb-3 text-lg font-bold uppercase tracking-wider text-muted-foreground">
                        {type === 'SERVICO' ? 'Serviços' : 'Produtos'}
                      </h3>
                      <div className="grid grid-cols-2 gap-4">
                        {activeItems
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
                  ))
                )}
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
                    {selectableClients.map((client) => (
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
                          <small className="block text-sm font-normal text-muted-foreground">
                            {formatCurrency(historicalPrices[item.id] ?? Number(item.preco))}
                            {!item.ativo ? ' · Item inativo' : ''}
                          </small>
                        </span>
                        <button
                          type="button"
                          aria-label={`Remover ${item.nome}`}
                          onClick={() => removeCartItem(item.id)}
                        >
                          <FiTrash2 className="size-5 text-muted-foreground" />
                        </button>
                        <button type="button" onClick={() => removeItem(item.id)}>
                          <FiMinus className="size-5" />
                        </button>
                        <span className="w-8 text-center text-2xl font-bold text-primary">
                          {cart[item.id]}
                        </span>
                        <button type="button" disabled={!item.ativo} onClick={() => addItem(item.id)}>
                          <FiPlus className="size-5" />
                        </button>
                        <strong className="w-28 text-right text-lg">
                          {formatCurrency(
                            (historicalPrices[item.id] ?? Number(item.preco)) * cart[item.id],
                          )}
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
                    disabled={
                      createVendaMutation.isPending ||
                      updateVendaMutation.isPending ||
                      itemsLoading ||
                      Boolean(itemsError) ||
                      !cartItems.length
                    }
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
        {cancelVendaMutation.error && (
          <p className="border-b px-5 py-3 text-base text-red-600">
            {getApiErrorMessage(
              cancelVendaMutation.error,
              'Não foi possível cancelar o pedido.',
            )}
          </p>
        )}
        <div className="grid gap-4 border-b bg-muted/20 p-5 md:grid-cols-2 xl:grid-cols-[1fr_1fr_1.4fr_1fr_auto] xl:items-end">
          <form className="flex items-end gap-2 md:col-span-2 xl:col-span-5" onSubmit={(event) => {
            event.preventDefault()
            setAppliedSearch(searchInput.trim())
            setPage(0)
          }}>
            <label className="grid min-w-0 flex-1 gap-1.5 text-sm font-semibold">
              Buscar por nome do cliente
              <input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Nome do cliente"
                className="h-10 rounded-lg border bg-background px-3 text-base font-normal outline-none focus-visible:ring-2 focus-visible:ring-ring/30"
              />
            </label>
            <Button type="submit" variant="outline" className="h-10 gap-2 rounded-lg bg-transparent px-4 text-sm hover:bg-muted/50">
              <FiSearch /> Buscar
            </Button>
          </form>
          <label className="grid gap-1.5 text-sm font-semibold">
            Data inicial
            <input
              type="date"
              value={dataInicial}
              onChange={(event) => { setDataInicial(event.target.value); setPage(0) }}
              className="h-10 rounded-lg border bg-background px-3 text-base font-normal"
            />
          </label>
          <label className="grid gap-1.5 text-sm font-semibold">
            Data final
            <input
              type="date"
              value={dataFinal}
              onChange={(event) => { setDataFinal(event.target.value); setPage(0) }}
              className="h-10 rounded-lg border bg-background px-3 text-base font-normal"
            />
          </label>
          <label className="grid gap-1.5 text-sm font-semibold">
            Cliente
            <select
              value={clienteFiltro}
              onChange={(event) => { setClienteFiltro(event.target.value); setPage(0) }}
              className="h-10 rounded-lg border bg-background px-3 text-base font-normal"
            >
              <option value="">Todos os clientes</option>
              {clients.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.nome}{client.ativo ? '' : ' (inativo)'}
                </option>
              ))}
            </select>
          </label>
          <label className="grid gap-1.5 text-sm font-semibold">
            Status
            <select
              value={statusFiltro}
              onChange={(event) => changeStatusFilter(event.target.value)}
              className="h-10 rounded-lg border bg-background px-3 text-base font-normal"
            >
              <option value="">Todos</option>
              <option value="FINALIZADA">Finalizados</option>
              <option value="CANCELADA">Cancelados</option>
            </select>
          </label>
          <Button type="button" variant="outline" className="h-10" onClick={clearFilters}>
            Limpar filtros
          </Button>
          {isFetching && !isLoading ? (
            <span className="text-sm text-muted-foreground md:col-span-2 xl:col-span-5">
              Atualizando resultados...
            </span>
          ) : null}
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
                    <div className="flex flex-col items-center gap-3">
                      <span>Não foi possível carregar os pedidos.</span>
                      <Button type="button" variant="outline" onClick={() => void refetch()}>
                        Tentar novamente
                      </Button>
                    </div>
                  </td>
                </tr>
              ) : orders.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center">
                    Nenhum pedido encontrado.
                  </td>
                </tr>
              ) : (
                orders.map((order) => (
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
                        <button
                          type="button"
                          title="Cancelar"
                          aria-label="Cancelar pedido"
                          disabled={order.status !== 'FINALIZADA' || cancelVendaMutation.isPending}
                          onClick={() => window.confirm(
                            `Cancelar o pedido #${order.id.slice(0, 8)}? O cancelamento preserva o histórico e não poderá ser desfeito pelo fluxo normal.`,
                          ) && cancelVendaMutation.mutate(order.id)}
                          className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-red-500/15 hover:text-red-300"
                        >
                          <FiXCircle />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        <PaginationControls
          page={currentPage}
          totalPages={data?.totalPages ?? 0}
          totalElements={data?.totalElements ?? 0}
          first={data?.first}
          last={data?.last}
          onPageChange={setPage}
        />
      </Card>
      <div className="order-note">
        <FiShoppingBag /> Os pedidos são vinculados ao caixa aberto pelo
        backend.
      </div>
    </>
  )
}
