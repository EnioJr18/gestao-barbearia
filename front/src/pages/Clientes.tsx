import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { FiPlus, FiSearch, FiUser, FiX } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
const schema = z.object({
  name: z.string().min(3, 'Informe o nome'),
  phone: z.string().min(8, 'Informe o telefone'),
})
type Client = z.infer<typeof schema>
const seed: Client[] = [
  { name: 'Lucas Andrade', phone: '(11) 98842-1022' },
  { name: 'Rafael Moreira', phone: '(11) 99731-4402' },
  { name: 'Caio Nascimento', phone: '(11) 99102-7731' },
  { name: 'Bruno Tavares', phone: '(11) 98421-8890' },
]
export function Clientes() {
  const [clients, setClients] = useState(seed)
  const [modal, setModal] = useState(false)
  const form = useForm<Client>({ resolver: zodResolver(schema) })
  const submit = (data: Client) => {
    setClients([...clients, data])
    form.reset()
    setModal(false)
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
        <Button onClick={() => setModal(true)}>
          <FiPlus /> Novo cliente
        </Button>
      </div>
      <Card className="overflow-hidden rounded-xl border shadow-sm">
        <div className="flex flex-col items-stretch justify-between gap-4 border-b bg-muted/20 p-5 sm:flex-row sm:items-center">
          <div className="flex h-10 w-full max-w-sm items-center gap-2 rounded-lg border bg-background px-3 text-muted-foreground shadow-sm focus-within:border-primary focus-within:ring-2 focus-within:ring-primary/15">
            <FiSearch />
            <input placeholder="Buscar cliente" />
          </div>
          <span className="text-sm text-muted-foreground">
            {clients.length} clientes cadastrados
          </span>
        </div>
        <table className="w-full min-w-[700px] border-collapse">
          <thead className="bg-muted/40">
            <tr>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                CLIENTE
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                TELEFONE
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                VISITAS
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                TOTAL GASTO
              </th>
              <th className="px-5 py-4 text-left text-[11px] font-bold tracking-wider text-muted-foreground">
                ÚLTIMA VISITA
              </th>
            </tr>
          </thead>
          <tbody>
            {clients.map((client, index) => (
              <tr
                className="border-b transition-colors hover:bg-muted/25 last:border-0"
                key={client.name}
              >
                <td>
                  <div className="flex items-center gap-3">
                    <div className="grid size-8 place-items-center rounded-full bg-emerald-100 text-[10px] font-bold text-primary">
                      {client.name
                        .split(' ')
                        .map((part) => part[0])
                        .join('')}
                    </div>
                    <strong className="text-sm text-foreground">
                      {client.name}
                    </strong>
                  </div>
                </td>
                <td className="px-5 py-5 text-sm text-muted-foreground">
                  {client.phone}
                </td>
                <td className="px-5 py-5 text-sm text-muted-foreground">
                  {12 - index}
                </td>
                <td className="px-5 py-5 text-sm">
                  <strong className="text-foreground">
                    R$ {660 - index * 120},00
                  </strong>
                </td>
                <td className="px-5 py-5 text-sm text-muted-foreground">
                  {index === 0 ? 'Hoje' : `${index + 1} dias atrás`}
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
                <FiUser />
              </div>
              <div>
                <h2 className="font-heading text-xl font-bold">Novo cliente</h2>
                <p className="text-xs text-muted-foreground">
                  Cadastre um cliente para acompanhar seu histórico.
                </p>
              </div>
            </div>
            <form className="grid gap-2" onSubmit={form.handleSubmit(submit)}>
              <Label htmlFor="client-name">Nome completo</Label>
              <Input
                id="client-name"
                placeholder="Ex.: João da Silva"
                {...form.register('name')}
              />
              <Label htmlFor="client-phone">Telefone</Label>
              <Input
                id="client-phone"
                placeholder="(11) 99999-9999"
                {...form.register('phone')}
              />
              <Button type="submit" className="mt-3">
                <FiPlus /> Cadastrar cliente
              </Button>
            </form>
          </div>
        </div>
      )}
    </>
  )
}
