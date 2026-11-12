import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { FiEdit2, FiPlus, FiScissors, FiSearch, FiTrash2, FiX } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Switch } from '@/components/ui/switch'
import { useAllItens } from '@/hooks/item/useAllItens'
import { useCreateServico } from '@/hooks/item/useCreateServico'
import { useInactiveItem } from '@/hooks/item/useInactiveItem'
import { useActiveItem } from '@/hooks/item/useActiveItem'
import { useUpdateItem } from '@/hooks/item/useUpdateItem'
import { useDeleteItem } from '@/hooks/item/useDeleteItem'

const schema = z.object({
  nome: z.string().min(2, 'Informe o nome'),
  preco: z.string().min(1, 'Informe o preço'),
})
type FormData = z.infer<typeof schema>
const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value)
const parsePrice = (value: string) =>
  Number(value.replace(/[^\d,]/g, '').replace(/\./g, '').replace(',', '.'))

const formatMoneyInput = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 12)
  const cents = digits.padStart(3, '0')
  const integer = cents.slice(0, -2).replace(/^0+(?=\d)/, '') || '0'
  return `${integer},${cents.slice(-2)}`
}

export function Servicos() {
  const [modal, setModal] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const { data: items = [], isLoading, error } = useAllItens({ nome: search || undefined, tipo: 'SERVICO' })
  const createMutation = useCreateServico()
  const inactiveMutation = useInactiveItem()
  const activeMutation = useActiveItem()
  const updateMutation = useUpdateItem()
  const deleteMutation = useDeleteItem()
  const form = useForm<FormData>({ resolver: zodResolver(schema), defaultValues: { nome: '', preco: '' } })
  const priceField = form.register('preco')
  const submit = (data: FormData) => {
    const payload = { nome: data.nome, preco: parsePrice(data.preco) }
    const options = { onSuccess: () => { form.reset(); setModal(false); setEditingId(null) } }
    if (editingId) updateMutation.mutate({ id: editingId, payload: { ...payload, tipo: 'SERVICO', estoque: null } }, options)
    else createMutation.mutate(payload, options)
  }

  const editService = (item: (typeof items)[number]) => {
    setEditingId(item.id)
    form.reset({ nome: item.nome, preco: String(item.preco).replace('.', ',') })
    setModal(true)
  }

  return (
    <>
      <div className="mb-10 flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-center">
        <div>
          <p className="text-sm font-bold tracking-[0.12em] text-primary">GESTÃO</p>
          <h1 className="mt-2 mb-2 font-heading text-4xl font-bold tracking-tight">Serviços</h1>
          <p className="text-lg leading-7 text-muted-foreground">Gerencie os serviços oferecidos pela barbearia.</p>
        </div>
        <Button size="lg" className="h-12 rounded-xl px-5 text-lg" onClick={() => { setEditingId(null); form.reset(); setModal(true) }}><FiPlus /> Novo serviço</Button>
      </div>
      <Card className="overflow-hidden rounded-2xl border shadow-sm">
        <div className="flex flex-col items-stretch justify-between gap-4 border-b bg-muted/20 p-5 text-lg sm:flex-row sm:items-center">
          <div className="flex h-10 w-full max-w-sm items-center gap-2 rounded-lg border bg-background px-3 text-muted-foreground">
            <FiSearch /><input placeholder="Buscar serviço" value={search} onChange={(event) => setSearch(event.target.value)} className="w-full bg-transparent text-lg outline-none" />
          </div>
        </div>
        <table className="w-full min-w-125 border-collapse text-xl [&_td]:text-xl [&_th]:text-base">
          <thead className="bg-muted/40"><tr><th className="px-6 py-5 text-left">SERVIÇO</th><th className="px-6 py-5 text-left">PREÇO</th><th className="px-6 py-5 text-left">STATUS</th><th className="px-6 py-5 text-left">AÇÕES</th></tr></thead>
          <tbody>
            {isLoading ? <tr><td colSpan={4} className="text-center">Carregando serviços...</td></tr> : error ? <tr><td colSpan={4} className="text-center text-red-600">Não foi possível carregar os serviços.</td></tr> : items.length === 0 ? <tr><td colSpan={4} className="text-center">Nenhum serviço encontrado.</td></tr> : items.map((item) => (
              <tr className="border-b transition-colors hover:bg-muted/25 last:border-0" key={item.id}>
                <td className="px-6 py-6 font-semibold text-foreground">{item.nome}</td>
                <td className="px-6 py-6 text-muted-foreground">{formatCurrency(Number(item.preco))}</td>
                <td className="px-6 py-6"><div className="flex items-center gap-2">
                  <Switch
                    checked={item.ativo}
                    aria-label={`${item.ativo ? 'Desativar' : 'Ativar'} serviço ${item.nome}`}
                    onCheckedChange={(checked) => checked ? activeMutation.mutate(item.id) : inactiveMutation.mutate(item.id)}
                    disabled={activeMutation.isPending || inactiveMutation.isPending}
                  />
                  <span className="text-lg text-muted-foreground">{item.ativo ? 'Ativo' : 'Inativo'}</span>
                </div></td>
                <td className="px-6 py-6"><div className="flex items-center gap-2">
                  <button type="button" title="Editar" aria-label="Editar serviço" onClick={() => editService(item)} className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-amber-500/15 hover:text-amber-300"><FiEdit2 /></button>
                  <button type="button" title="Excluir" aria-label="Excluir serviço" disabled={deleteMutation.isPending} onClick={() => window.confirm(`Excluir ${item.nome}?`) && deleteMutation.mutate(item.id)} className="rounded-lg border-0 bg-transparent p-2 text-muted-foreground transition-colors hover:bg-red-500/15 hover:text-red-300"><FiTrash2 /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
      </Card>
      {modal && <div className="fixed inset-0 z-50 grid place-items-center bg-foreground/40 p-5" onMouseDown={() => setModal(false)}>
        <div className="relative w-full max-w-xl rounded-2xl bg-background p-8 shadow-xl" onMouseDown={(event) => event.stopPropagation()}>
          <button className="absolute right-4 top-4 text-muted-foreground" onClick={() => setModal(false)}><FiX /></button>
          <div className="mb-6 flex items-center gap-3"><div className="grid size-10 place-items-center rounded-lg bg-amber-50 text-amber-600"><FiScissors /></div><div><h2 className="font-heading text-3xl font-bold">{editingId ? 'Editar serviço' : 'Novo serviço'}</h2><p className="text-md text-muted-foreground">Cadastre um serviço para sua agenda.</p></div></div>
          <form className="grid gap-2" onSubmit={form.handleSubmit(submit)}>
            <Label className="text-lg" htmlFor="service-name">Nome do serviço</Label><Input id="service-name" className="h-12 text-lg" placeholder="Ex.: Corte degradê" {...form.register('nome')} />{form.formState.errors.nome && <small className="text-red-500">{form.formState.errors.nome.message}</small>}
            <Label className="text-lg" htmlFor="service-price">Preço</Label><Input id="service-price" className="h-12 text-lg" placeholder="35,00" inputMode="decimal" {...priceField} onChange={(event) => { event.target.value = formatMoneyInput(event.target.value); priceField.onChange(event) }} />{form.formState.errors.preco && <small className="text-red-500">{form.formState.errors.preco.message}</small>}
            <Button type="submit" size="lg" className="mt-3 h-12 rounded-xl text-lg" disabled={createMutation.isPending || updateMutation.isPending}><FiPlus /> {createMutation.isPending || updateMutation.isPending ? 'Salvando...' : editingId ? 'Salvar alterações' : 'Adicionar serviço'}</Button>
          </form>
        </div>
      </div>}
    </>
  )
}
