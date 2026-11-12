import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { AxiosError } from 'axios'
import { FiAlertCircle, FiLock, FiUnlock } from 'react-icons/fi'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useAllCaixas } from '@/hooks/caixa/useAllCaixas'
import { useCloseCaixa } from '@/hooks/caixa/useCloseCaixa'
import { useCurrentCaixa } from '@/hooks/caixa/useCurrentCaixa'
import { useOpenCaixa } from '@/hooks/caixa/useOpenCaixa'

const currency = (value: number | null | undefined) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(
    Number(value ?? 0),
  )

const dateTime = (value: string | null) =>
  value
    ? new Intl.DateTimeFormat('pt-BR', {
        day: '2-digit',
        month: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
      }).format(new Date(value))
    : '—'

const parseMoney = (value: string) => {
  const normalized = value.replace(/[^\d,.-]/g, '').replace(/\./g, '').replace(',', '.')
  const parsed = Number(normalized)
  return Number.isFinite(parsed) ? parsed : 0
}

const formatMoneyInput = (value: string) => {
  const digits = value.replace(/\D/g, '').slice(0, 12)
  const cents = digits.padStart(3, '0')
  const integer = cents.slice(0, -2).replace(/^0+(?=\d)/, '') || '0'
  return `${integer},${cents.slice(-2)}`
}

const getApiErrorMessage = (error: unknown, fallback: string) =>
  error instanceof AxiosError
    ? (error.response?.data?.message ?? fallback)
    : fallback

type OpenForm = { valorInicial: string }
type CloseForm = { valorApurado: string }

export function Caixa() {
  const [openModal, setOpenModal] = useState(false)
  const [closeModal, setCloseModal] = useState(false)
  const { data: caixa, isLoading: currentLoading, error: currentError } =
    useCurrentCaixa()
  const { data: historico = [], isLoading: historyLoading, error: historyError } =
    useAllCaixas()
  const openMutation = useOpenCaixa()
  const closeMutation = useCloseCaixa()
  const openForm = useForm<OpenForm>({ defaultValues: { valorInicial: '100,00' } })
  const closeForm = useForm<CloseForm>({
    defaultValues: { valorApurado: '' },
  })
  const openingField = openForm.register('valorInicial')
  const countedField = closeForm.register('valorApurado')

  const caixaAberto = caixa?.status === 'ABERTO'
  const abrir = (data: OpenForm) => {
    openMutation.mutate(
      { valorInicial: parseMoney(data.valorInicial) },
      { onSuccess: () => { openForm.reset({ valorInicial: '100,00' }); setOpenModal(false) } },
    )
  }
  const fechar = (data: CloseForm) => {
    if (!caixa) return
    closeMutation.mutate(
      { id: caixa.id, payload: { valorApurado: parseMoney(data.valorApurado) } },
      { onSuccess: () => { closeForm.reset(); setCloseModal(false) } },
    )
  }

  return (
    <>
      <div className="mb-10">
        <p className="text-sm font-bold tracking-[0.12em] text-primary">OPERAÇÃO</p>
        <h1 className="mt-2 mb-2 font-heading text-4xl font-bold tracking-tight">Caixa</h1>
        <p className="text-lg leading-7 text-muted-foreground">Abra e feche o caixa quantas vezes precisar no dia.</p>
      </div>
      {currentError && <p className="mb-4 flex items-center gap-2 text-red-500"><FiAlertCircle /> Não foi possível consultar o caixa atual.</p>}
      {openMutation.error && (
        <p className="mb-4 flex items-center gap-2 text-red-500">
          <FiAlertCircle />
          {getApiErrorMessage(openMutation.error, 'Não foi possível abrir o caixa.')}
        </p>
      )}
      {closeMutation.error && (
        <p className="mb-4 flex items-center gap-2 text-red-500">
          <FiAlertCircle />
          {getApiErrorMessage(closeMutation.error, 'Não foi possível fechar o caixa.')}
        </p>
      )}
      <div className="grid gap-5 lg:grid-cols-[1.05fr_1fr_1fr]">
        <Card className="min-h-40 rounded-2xl p-6">
          <div className="mb-6 flex items-center gap-4">
            <div className={`grid size-12 place-items-center rounded-xl ${caixaAberto ? 'bg-emerald-500/15 text-emerald-500' : 'bg-red-500/15 text-red-500'}`}>
              {caixaAberto ? <FiUnlock className="size-7" /> : <FiLock className="size-7" />}
            </div>
            <div>
              <h2 className="font-heading text-2xl font-bold">{currentLoading ? 'Carregando...' : caixaAberto ? 'Caixa aberto' : 'Caixa fechado'}</h2>
              <p className="text-base text-muted-foreground">{caixaAberto ? 'Pronto para registrar pedidos' : 'Abra para registrar pedidos'}</p>
            </div>
          </div>
          {!caixaAberto ? (
            <Button className="h-12 w-full rounded-xl text-lg" onClick={() => { openMutation.reset(); setOpenModal(true) }}>Abrir caixa</Button>
          ) : (
            <Button variant="outline" className="h-12 w-full rounded-xl text-lg" onClick={() => { closeForm.reset({ valorApurado: formatMoneyInput(String(caixa.valorEsperado ?? 0)) }); setCloseModal(true) }}>
              Fechar caixa
            </Button>
          )}
        </Card>
        <Card className="min-h-40 rounded-2xl p-6">
          <p className="text-base text-muted-foreground">Vendido nesta sessão</p>
          <strong className="mt-3 block font-heading text-4xl text-primary">{currency((caixa?.dinheiro ?? 0) + (caixa?.pix ?? 0) + (caixa?.debito ?? 0) + (caixa?.credito ?? 0))}</strong>
          <span className="text-base text-muted-foreground">Entradas registradas</span>
        </Card>
        <Card className="min-h-40 rounded-2xl p-6">
          <p className="text-base text-muted-foreground">Dinheiro esperado na gaveta</p>
          <strong className="mt-3 block font-heading text-4xl">{currency(caixa?.valorEsperado)}</strong>
          <span className="text-base text-muted-foreground">Fundo + vendas em dinheiro</span>
        </Card>
      </div>
      <div className="mt-5 grid grid-cols-2 gap-4 lg:grid-cols-4">
        {[
          ['Dinheiro', caixa?.dinheiro],
          ['Pix', caixa?.pix],
          ['Débito', caixa?.debito],
          ['Crédito', caixa?.credito],
        ].map(([label, value]) => (
          <Card className="flex min-h-16 items-center justify-between rounded-2xl p-5" key={label as string}>
            <span className="text-lg text-muted-foreground">{label}</span>
            <strong className="text-lg">{currency(value as number)}</strong>
          </Card>
        ))}
      </div>
      <Card className="mt-6 overflow-hidden rounded-2xl">
        <div className="border-b p-6"><h2 className="font-heading text-2xl font-bold">Histórico de sessões</h2></div>
        <div className="overflow-x-auto">
          <table className="w-full min-w-225 border-collapse text-lg [&_td]:text-lg [&_th]:text-sm">
            <thead><tr><th>ABERTURA</th><th>FECHAMENTO</th><th>VENDAS</th><th>ESPERADO</th><th>CONTADO</th><th>DIFERENÇA</th></tr></thead>
            <tbody>
              {historyLoading ? <tr><td colSpan={6} className="text-center text-lg">Carregando histórico...</td></tr> : historyError ? <tr><td colSpan={6} className="text-center text-lg text-red-500">Não foi possível carregar o histórico.</td></tr> : historico.length === 0 ? <tr><td colSpan={6} className="text-center text-lg">Nenhuma sessão registrada.</td></tr> : historico.map((session) => {
                const vendas = session.dinheiro + session.pix + session.debito + session.credito
                return <tr key={session.id}><td>{dateTime(session.abertoEm)}</td><td>{dateTime(session.fechadoEm)}</td><td>{currency(vendas)}</td><td>{currency(session.valorEsperado)}</td><td>{currency(session.valorApurado)}</td><td className={session.diferenca >= 0 ? 'text-emerald-500' : 'text-red-500'}>{currency(session.diferenca)}</td></tr>
              })}
            </tbody>
          </table>
        </div>
      </Card>
      {openModal && <div className="fixed inset-0 z-50 grid place-items-center bg-foreground/40 p-5" onMouseDown={() => { if (!openMutation.isPending) { openMutation.reset(); setOpenModal(false) } }}>
        <div className="w-full max-w-md rounded-2xl bg-background p-8 shadow-xl" onMouseDown={(event) => event.stopPropagation()}>
          <h2 className="font-heading text-3xl font-bold">Abrir caixa</h2><p className="mt-1 text-base text-muted-foreground">Informe o valor inicial da gaveta.</p>
          <form className="mt-6 grid gap-2" onSubmit={openForm.handleSubmit(abrir)}>
            <Label className="text-lg" htmlFor="opening-value">Valor inicial (R$)</Label>
            <Input
              id="opening-value"
              inputMode="decimal"
              className="h-12 text-lg"
              {...openingField}
              onChange={(event) => {
                event.target.value = formatMoneyInput(event.target.value)
                openingField.onChange(event)
              }}
            />
            {openMutation.error && (
              <p className="flex items-center gap-2 text-sm text-red-500">
                <FiAlertCircle />
                {getApiErrorMessage(openMutation.error, 'Não foi possível abrir o caixa.')}
              </p>
            )}
            <Button type="submit" className="mt-3 h-12 rounded-xl text-lg" disabled={openMutation.isPending}>
              {openMutation.isPending ? 'Abrindo...' : 'Abrir caixa'}
            </Button>
          </form>
        </div>
      </div>}
      {closeModal && <div className="fixed inset-0 z-50 grid place-items-center bg-foreground/40 p-5" onMouseDown={() => setCloseModal(false)}>
        <div className="w-full max-w-md rounded-2xl bg-background p-8 shadow-xl" onMouseDown={(event) => event.stopPropagation()}>
          <h2 className="font-heading text-3xl font-bold">Fechar caixa</h2><p className="mt-1 text-base text-muted-foreground">Informe o valor contado na gaveta.</p>
          <form className="mt-6 grid gap-2" onSubmit={closeForm.handleSubmit(fechar)}>
            <Label className="text-lg" htmlFor="counted-value">Valor contado (R$)</Label>
            <Input
              id="counted-value"
              inputMode="decimal"
              className="h-12 text-lg"
              {...countedField}
              onChange={(event) => {
                event.target.value = formatMoneyInput(event.target.value)
                countedField.onChange(event)
              }}
            />
            {closeMutation.error && (
              <p className="flex items-center gap-2 text-sm text-red-500">
                <FiAlertCircle />
                {getApiErrorMessage(closeMutation.error, 'Não foi possível fechar o caixa.')}
              </p>
            )}
            <Button type="submit" className="mt-3 h-12 rounded-xl text-lg" disabled={closeMutation.isPending}>
              {closeMutation.isPending ? 'Fechando...' : 'Confirmar fechamento'}
            </Button>
          </form>
        </div>
      </div>}
    </>
  )
}
