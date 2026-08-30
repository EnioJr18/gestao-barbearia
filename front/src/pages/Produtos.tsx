import { useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { FiBox, FiPlus, FiSearch, FiX } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

type Item = {
  name: string
  type: 'Serviço' | 'Produto'
  price: string
  stock: string
}
const schema = z.object({
  name: z.string().min(2, 'Informe o nome'),
  price: z.string().min(1, 'Informe o preço'),
  type: z.enum(['Serviço', 'Produto']),
})
type FormData = z.infer<typeof schema>
const initial: Item[] = [
  { name: 'Corte tradicional', type: 'Serviço', price: 'R$ 35,00', stock: '—' },
  { name: 'Corte + barba', type: 'Serviço', price: 'R$ 55,00', stock: '—' },
  {
    name: 'Pomada modeladora',
    type: 'Produto',
    price: 'R$ 42,90',
    stock: '18 un.',
  },
  {
    name: 'Salgadinho artesanal',
    type: 'Produto',
    price: 'R$ 8,00',
    stock: '32 un.',
  },
]
export function Produtos() {
  const [items, setItems] = useState(initial)
  const [modal, setModal] = useState(false)
  const form = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: { type: 'Serviço' },
  })
  const type = useWatch({ control: form.control, name: 'type' })
  const submit = (data: FormData) => {
    setItems([
      ...items,
      {
        ...data,
        price: `R$ ${data.price}`,
        stock: data.type === 'Produto' ? '0 un.' : '—',
      },
    ])
    form.reset({ type: 'Serviço' })
    setModal(false)
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
            <input placeholder="Buscar no catálogo" />
          </div>
          <select className="h-10 rounded-lg border bg-background px-3 text-sm text-muted-foreground">
            <option>Todos os tipos</option>
            <option>Serviços</option>
            <option>Produtos</option>
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
            {items.map((item) => (
              <tr
                className="border-b transition-colors hover:bg-muted/25 last:border-0"
                key={item.name}
              >
                <td className="px-5 py-5 text-sm font-semibold text-foreground">
                  {item.name}
                </td>
                <td className="px-5 py-5 text-sm text-muted-foreground">
                  <span className="rounded-md bg-muted px-2.5 py-1 text-[11px] font-medium">
                    {item.type}
                  </span>
                </td>
                <td className="px-5 py-5 text-sm text-muted-foreground">
                  {item.price}
                </td>
                <td className="px-5 py-5 text-sm text-muted-foreground">
                  {item.stock}
                </td>
                <td className="px-5 py-5 text-sm">
                  <span className="inline-flex items-center gap-1 rounded-full bg-emerald-50 px-2.5 py-1 text-[11px] text-emerald-700">
                    <i className="size-1.5 rounded-full bg-emerald-500" />
                    Ativo
                  </span>
                </td>
              </tr>
            ))}
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
                {...form.register('name')}
              />
              {form.formState.errors.name && (
                <small className="text-[10px] text-red-500">
                  {form.formState.errors.name.message}
                </small>
              )}
              <Label htmlFor="item-price">Preço</Label>
              <Input
                id="item-price"
                placeholder="35,00"
                {...form.register('price')}
              />
              <Label>Tipo</Label>
              <div className="grid grid-cols-2 gap-2">
                {['Serviço', 'Produto'].map((option) => (
                  <button
                    type="button"
                    key={option}
                    className={`rounded-md border p-2 text-xs ${type === option ? 'border-emerald-300 bg-emerald-50 font-bold text-primary' : 'text-muted-foreground'}`}
                    onClick={() =>
                      form.setValue('type', option as FormData['type'])
                    }
                  >
                    {option}
                  </button>
                ))}
              </div>
              <Button type="submit" className="mt-3">
                <FiPlus /> Adicionar ao catálogo
              </Button>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
