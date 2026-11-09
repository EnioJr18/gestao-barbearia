import { useMemo, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { FiEdit2, FiPlus, FiSearch, FiTrash2, FiUser, FiX } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { useAllClientes } from '@/hooks/cliente/useAllClientes'
import { useCreateCliente } from '@/hooks/cliente/useCreateCliente'
import { useUpdateCliente } from '@/hooks/cliente/useUpdateCliente'
import { useDeleteCliente } from '@/hooks/cliente/useDeleteCliente'
import { useActiveCliente } from '@/hooks/cliente/useActiveCliente'
import { useInactiveCliente } from '@/hooks/cliente/useInactiveCliente'

const schema = z.object({
  nome: z.string().min(3, 'Informe o nome'),
  telefone: z
    .string()
    .min(8, 'Informe o telefone')
    .optional()
    .or(z.literal('')),
})

type ClienteForm = z.infer<typeof schema>

const formatPhone = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 11)
  if (digits.length <= 2) return digits.length ? `(${digits}` : ''
  if (digits.length <= 7) return `(${digits.slice(0, 2)}) ${digits.slice(2)}`
  return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`
}

export function Clientes() {
  const [modal, setModal] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [buscar, setBuscar] = useState('')
  const { data: clientes = [], isLoading } = useAllClientes()
  const createClienteMutation = useCreateCliente()
  const updateClienteMutation = useUpdateCliente()
  const deleteClienteMutation = useDeleteCliente()
  const activeClienteMutation = useActiveCliente()
  const inactiveClienteMutation = useInactiveCliente()

  const filteredClientes = useMemo(() => {
    const value = buscar.trim().toLowerCase()
    if (!value) return clientes

    return clientes.filter(
      (cliente) =>
        cliente.nome.toLowerCase().includes(value) ||
        (cliente.telefone ?? '').toLowerCase().includes(value),
    )
  }, [clientes, buscar])

  const form = useForm<ClienteForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      nome: '',
      telefone: '',
    },
  })
  const phoneField = form.register('telefone')

  const submit = (data: ClienteForm) => {
    const payload = { nome: data.nome, telefone: data.telefone?.trim() || null }
    const options = { onSuccess: () => { form.reset(); setModal(false); setEditingId(null) } }
    if (editingId) updateClienteMutation.mutate({ id: editingId, payload }, options)
    else createClienteMutation.mutate(payload, options)
  }

  const editCliente = (cliente: (typeof clientes)[number]) => {
    setEditingId(cliente.id)
    form.reset({ nome: cliente.nome, telefone: formatPhone(cliente.telefone ?? '') })
    setModal(true)
  }

  return (
    <>
      <div className="mb-8 flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-center">
        <div>
          <p className="text-[11px] font-bold tracking-[0.12em] text-primary">
            RELACIONAMENTO
          </p>
          <h1 className="mt-2 mb-1 font-heading text-3xl font-bold tracking-tight">
            Clientes
          </h1>
          <p className="text-base leading-6 text-muted-foreground">
            Conheça quem mantém sua cadeira em movimento.
          </p>
        </div>
        <Button
          size="lg"
          className="h-10 px-4 text-sm [&_svg:not([class*='size-'])]:size-4"
          onClick={() => { setEditingId(null); form.reset(); setModal(true) }}
        >
          <FiPlus /> Novo cliente
        </Button>
      </div>
      <Card className="overflow-hidden rounded-xl border shadow-sm">
        <div className="flex flex-col items-stretch justify-between gap-4 border-b bg-muted/20 p-5 text-lg sm:flex-row sm:items-center">
          <div className="flex h-10 w-full max-w-sm items-center gap-2 rounded-lg border bg-background px-3 text-muted-foreground">
            <FiSearch />
            <input
              placeholder="Buscar cliente"
              value={buscar}
              onChange={(event) => setBuscar(event.target.value)}
              className="w-full bg-transparent text-lg outline-none"
            />
          </div>
          <span className="text-sm text-muted-foreground">
            {filteredClientes.length} clientes cadastrados
          </span>
        </div>
        <table className="w-full min-w-[700px] border-collapse text-lg [&_td]:text-lg [&_th]:text-sm">
          <thead className="bg-muted/40">
            <tr>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">
                CLIENTE
              </th>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">
                TELEFONE
              </th>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">
                VISITAS
              </th>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">
                TOTAL GASTO
              </th>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">
                ÚLTIMA VISITA
              </th>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">STATUS</th>
              <th className="px-5 py-4 text-left text-sm font-bold tracking-wider text-muted-foreground">AÇÕES</th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-5 py-8 text-center text-sm text-muted-foreground"
                >
                  Carregando clientes...
                </td>
              </tr>
            ) : filteredClientes.length === 0 ? (
              <tr>
                <td
                  colSpan={7}
                  className="px-5 py-8 text-center text-sm text-muted-foreground"
                >
                  Nenhum cliente encontrado.
                </td>
              </tr>
            ) : (
              filteredClientes.map((cliente) => (
                <tr
                  className="border-b transition-colors hover:bg-muted/25 last:border-0"
                  key={cliente.id}
                >
                  <td>
                    <div className="flex items-center gap-3 px-5 py-5">
                      <div className="grid size-8 place-items-center rounded-full bg-emerald-100 text-xs font-bold text-primary">
                        {cliente.nome
                          .split(' ')
                          .map((part) => part[0])
                          .join('')
                          .slice(0, 2)}
                      </div>
                      <strong className="text-lg text-foreground">
                        {cliente.nome}
                      </strong>
                    </div>
                  </td>
                  <td className="px-5 py-5 text-base text-muted-foreground">
                    {cliente.telefone || '—'}
                  </td>
                  <td className="px-5 py-5 text-base text-muted-foreground">
                    {cliente.visitas}
                  </td>
                  <td className="px-5 py-5 text-base">
                    <strong className="text-foreground">
                      {new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(Number(cliente.totalGasto))}
                    </strong>
                  </td>
                  <td className="px-5 py-5 text-base text-muted-foreground">
                    {cliente.ultimaVisita
                      ? new Intl.DateTimeFormat('pt-BR').format(new Date(`${cliente.ultimaVisita}T00:00:00`))
                      : '—'}
                  </td>
                  <td className="px-5 py-5">
                    <div className="flex items-center gap-2">
                      <Switch
                        checked={cliente.ativo}
                        aria-label={`${cliente.ativo ? 'Desativar' : 'Ativar'} cliente ${cliente.nome}`}
                        onCheckedChange={(checked) => checked
                          ? activeClienteMutation.mutate(cliente.id)
                          : inactiveClienteMutation.mutate(cliente.id)}
                        disabled={activeClienteMutation.isPending || inactiveClienteMutation.isPending}
                      />
                      <span className="text-lg text-muted-foreground">{cliente.ativo ? 'Ativo' : 'Inativo'}</span>
                    </div>
                  </td>
                  <td className="px-5 py-5">
                    <div className="flex items-center gap-2">
                      <button type="button" title="Editar" aria-label="Editar cliente" onClick={() => editCliente(cliente)} className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-amber-500/15 hover:text-amber-300"><FiEdit2 /></button>
                      <button type="button" title="Excluir" aria-label="Excluir cliente" disabled={deleteClienteMutation.isPending} onClick={() => window.confirm(`Excluir ${cliente.nome}?`) && deleteClienteMutation.mutate(cliente.id)} className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-red-500/15 hover:text-red-300"><FiTrash2 /></button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </Card>
      {modal && (
        <div
          className="fixed inset-0 z-50 grid place-items-center bg-foreground/40 p-5"
          onMouseDown={() => setModal(false)}
        >
          <div
            className="relative w-full max-w-xl rounded-xl bg-background p-8 shadow-xl"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button
              className="absolute right-4 top-4 border-0 bg-transparent text-muted-foreground"
              onClick={() => setModal(false)}
            >
              <FiX />
            </button>
            <div className="mb-6 flex items-center gap-3">
              <div className="grid size-10 place-items-center rounded-lg bg-amber-50 text-amber-600">
                <FiUser />
              </div>
              <div>
                <h2 className="font-heading text-3xl font-bold">
                  {editingId ? 'Editar cliente' : 'Novo cliente'}
                </h2>
                <p className="text-md text-muted-foreground">
                  Cadastre um cliente para acompanhar seu histórico.
                </p>
              </div>
            </div>
            <form className="grid gap-2" onSubmit={form.handleSubmit(submit)}>
              <Label className="text-md" htmlFor="client-name">
                Nome completo
              </Label>
              <Input
                id="client-name"
                placeholder="Ex.: João da Silva"
                className="h-12 text-lg placeholder:text-lg"
                {...form.register('nome')}
              />
              {form.formState.errors.nome && (
                <span className="text-xs text-red-600">
                  {form.formState.errors.nome.message}
                </span>
              )}

              <Label className="text-lg" htmlFor="client-phone">
                Telefone
              </Label>
              <Input
                id="client-phone"
                placeholder="(11) 99999-9999"
                className="h-12 text-lg placeholder:text-lg"
                {...phoneField}
                onChange={(event) => {
                  event.target.value = formatPhone(event.target.value)
                  phoneField.onChange(event)
                }}
              />
              {form.formState.errors.telefone && (
                <span className="text-xs text-red-600">
                  {form.formState.errors.telefone.message}
                </span>
              )}

              <Button
                type="submit"
                size="lg"
                className="mt-3 h-12 px-4 text-lg [&_svg:not([class*='size-'])]:size-4"
                disabled={createClienteMutation.isPending}
              >
                <FiPlus /> {createClienteMutation.isPending || updateClienteMutation.isPending
                  ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Cadastrar cliente'}
              </Button>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
