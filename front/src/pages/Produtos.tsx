import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { FiBox, FiPlus, FiSearch, FiX } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAllItens } from '@/hooks/item/useAllItens'
import { useCreateItem } from '@/hooks/item/useCreateItem'
import { useInactiveItem } from '@/hooks/item/useInactiveItem'
import type { TipoItem } from '@/hooks/item/types'

const schema = z.object({
  nome: z.string().min(2, 'Informe o nome'),
  preco: z.coerce.number().min(0, 'Informe um preço válido'),
  tipo: z.enum(['SERVICO', 'PRODUTO']),
  estoque: z.coerce.number().int().min(0).optional(),
})
type FormData = z.infer<typeof schema>
type FormInput = z.input<typeof schema>

const formatCurrency = (value: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    value,
  )

export function Produtos() {
  const [modal, setModal] = useState(false)
  const [search, setSearch] = useState('')
  const [tipo, setTipo] = useState<TipoItem | ''>('')
  const {
    data: items = [],
    isLoading,
    error,
  } = useAllItens({
    nome: search || undefined,
    tipo: tipo || undefined,
  })
  const createItemMutation = useCreateItem()
  const inactiveItemMutation = useInactiveItem()
  const form = useForm<FormInput, unknown, FormData>({
    resolver: zodResolver(schema),
    defaultValues: { tipo: 'SERVICO', estoque: 0 },
  })
  const type = useWatch({ control: form.control, name: 'tipo' })
  const submit = (data: FormData) => {
    createItemMutation.mutate(
      {
        ...data,
        estoque: data.tipo === 'PRODUTO' ? (data.estoque ?? 0) : null,
      },
      {
        onSuccess: () => {
          form.reset({ tipo: 'SERVICO', estoque: 0 })
          setModal(false)
        },
      },
    )
  }
  return (
    <>
      <div className="mb-8 flex flex-col items-start justify-between gap-5 sm:flex-row sm:items-center">
        <div>
          <p className="text-[11px] font-bold tracking-[0.12em] text-primary">
            GESTÃO
          </p>
          <h1 className="mt-2 mb-1 font-heading text-3xl font-bold tracking-tight">
            Produtos e serviços
          </h1>
          <p className="text-base leading-6 text-muted-foreground">
            Gerencie tudo o que sua barbearia oferece.
          </p>
        </div>
        <Button onClick={() => setModal(true)}>
          <FiPlus /> Novo item
        </Button>
      </div>
      <Card className="overflow-hidden rounded-xl border shadow-sm">
        <div className="flex flex-col items-stretch justify-between gap-4 border-b bg-muted/20 p-5 sm:flex-row sm:items-center">
          <div className="flex h-10 w-full max-w-sm items-center gap-2 rounded-lg border bg-background px-3 text-muted-foreground shadow-sm focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/15">
            <FiSearch />
            <input
              placeholder="Buscar no catálogo"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />
          </div>
          <select
            value={tipo}
            onChange={(event) => setTipo(event.target.value as TipoItem | '')}
            className="h-10 rounded-lg border bg-background px-3 text-sm text-muted-foreground"
          >
            <option value="">Todos os tipos</option>
            <option value="SERVICO">Serviços</option>
            <option value="PRODUTO">Produtos</option>
          </select>
        </div>
        <table className="w-full min-w-175 border-collapse">
          <thead className="bg-muted/40">
            <tr>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                ITEM
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                TIPO
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                PREÇO
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                ESTOQUE
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                STATUS
              </th>
            </tr>
          </thead>
          <tbody>
            {isLoading ? (
              <tr>
                <td colSpan={5} className="text-center">
                  Carregando itens...
                </td>
              </tr>
            ) : error ? (
              <tr>
                <td colSpan={5} className="text-center text-red-600">
                  Não foi possível carregar o catálogo.
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={5} className="text-center">
                  Nenhum item encontrado.
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr
                  className="border-b transition-colors hover:bg-muted/25 last:border-0"
                  key={item.id}
                >
                  <td className="px-5 py-5 text-sm font-semibold text-foreground">
                    {item.nome}
                  </td>
                  <td className="px-5 py-5 text-sm text-muted-foreground">
                    <span className="rounded-md bg-muted px-2.5 py-1 text-[11px] font-medium">
                      {item.tipo === 'SERVICO' ? 'Serviço' : 'Produto'}
                    </span>
                  </td>
                  <td className="px-5 py-5 text-sm text-muted-foreground">
                    {formatCurrency(Number(item.preco))}
                  </td>
                  <td className="px-5 py-5 text-sm text-muted-foreground">
                    {item.tipo === 'PRODUTO' ? `${item.estoque ?? 0} un.` : '—'}
                  </td>
                  <td className="px-5 py-5 text-sm">
                    <button
                      onClick={() =>
                        item.ativo && inactiveItemMutation.mutate(item.id)
                      }
                      className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] text-emerald-700"
                    >
                      <i className="size-1.5 rounded-full bg-emerald-500" />
                      {item.ativo ? 'Ativo' : 'Inativo'}
                    </button>
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
            className="relative w-full max-w-md rounded-xl bg-background p-7 shadow-xl"
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
                <FiBox />
              </div>
              <div>
                <h2 className="font-heading text-xl font-bold">Novo item</h2>
                <p className="text-xs text-muted-foreground">
                  Adicione um produto ou serviço ao catálogo.
                </p>
              </div>
            </div>
            <form className="grid gap-2" onSubmit={form.handleSubmit(submit)}>
              <Label htmlFor="item-name">Nome do item</Label>
              <Input
                id="item-name"
                placeholder="Ex.: Corte degradê"
                {...form.register('nome')}
              />
              {form.formState.errors.nome && (
                <small className="text-[10px] text-red-500">
                  {form.formState.errors.nome.message}
                </small>
              )}
              <Label htmlFor="item-price">Preço</Label>
              <Input
                id="item-price"
                placeholder="35,00"
                type="number"
                step="0.01"
                {...form.register('preco')}
              />
              {form.formState.errors.preco && (
                <small className="text-[10px] text-red-500">
                  {form.formState.errors.preco.message}
                </small>
              )}
              <Label>Tipo</Label>
              <div className="grid grid-cols-2 gap-2">
                {(['SERVICO', 'PRODUTO'] as const).map((option) => (
                  <button
                    type="button"
                    key={option}
                    className={`rounded-md border p-2 text-xs ${type === option ? 'border-emerald-300 bg-emerald-50 font-bold text-primary' : 'text-muted-foreground'}`}
                    onClick={() => form.setValue('tipo', option)}
                  >
                    {option === 'SERVICO' ? 'Serviço' : 'Produto'}
                  </button>
                ))}
              </div>
              {type === 'PRODUTO' && (
                <>
                  <Label htmlFor="item-stock">Estoque inicial</Label>
                  <Input
                    id="item-stock"
                    type="number"
                    min="0"
                    {...form.register('estoque')}
                  />
                </>
              )}
              <Button type="submit" className="mt-3">
                <FiPlus />{' '}
                {createItemMutation.isPending
                  ? 'Salvando...'
                  : 'Adicionar ao catálogo'}
              </Button>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
