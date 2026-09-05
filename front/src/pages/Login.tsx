import { useState, type ReactNode } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { AxiosError } from 'axios'
import { FiArrowRight, FiCheck, FiEye, FiEyeOff, FiLock, FiMail, FiScissors, FiUser } from 'react-icons/fi'
import { useNavigate } from 'react-router-dom'
import { useCreateUser } from '@/hooks/user/useCreateUser'
import { useLogin } from '@/hooks/user/useLogin'

const loginSchema = z.object({
	nome: z.string().optional(),
	email: z.string().email('Digite um e-mail válido'),
	senha: z.string().min(6, 'A senha deve ter pelo menos 6 caracteres'),
})
type FormValues = z.infer<typeof loginSchema>

export function Login() {
	const [registering, setRegistering] = useState(false)
	const [showPassword, setShowPassword] = useState(false)
	const loginMutation = useLogin()
	const createUserMutation = useCreateUser()
	const navigate = useNavigate()
	const form = useForm<FormValues>({
		resolver: zodResolver(loginSchema),
		defaultValues: { nome: '', email: '', senha: '' },
	})
	const mutation = registering ? createUserMutation : loginMutation
	const errorMessage = mutation.error instanceof AxiosError
		? mutation.error.response?.data?.message ?? 'Não foi possível concluir a operação.'
		: mutation.error?.message

	const onSubmit = form.handleSubmit(async (values) => {
		if (registering) {
			if (!values.nome || values.nome.trim().length < 2) {
				form.setError('nome', { message: 'Digite seu nome' })
				return
			}
			await createUserMutation.mutateAsync({ nome: values.nome, email: values.email, senha: values.senha })
			setRegistering(false)
			form.reset({ nome: '', email: values.email, senha: '' })
			return
		}
		await loginMutation.mutateAsync({ email: values.email, senha: values.senha })
		navigate('/dashboard')
	})

	const switchMode = () => {
		setRegistering((current) => !current)
		form.clearErrors()
		loginMutation.reset()
		createUserMutation.reset()
	}

	return (
		<main className="relative flex min-h-screen overflow-hidden bg-[#f4f7f4] text-[#172624]">
			<section className="relative hidden w-[46%] flex-col justify-between overflow-hidden bg-[#123d39] p-10 text-[#f7f8f1] lg:flex xl:p-14">
				<div className="absolute -right-32 -top-32 size-[28rem] rounded-full border border-[#d9a441]/30" />
				<div className="absolute -bottom-44 -left-28 size-[30rem] rounded-full border border-[#9bc7b7]/20" />
				<div className="relative flex items-center gap-3"><div className="grid size-10 place-items-center rounded-xl bg-[#d9a441] text-[#123d39]"><FiScissors size={20} /></div><div><strong className="block text-lg tracking-tight">Corte & Cia</strong><span className="text-xs text-[#b8d2c8]">gestão inteligente</span></div></div>
				<div className="relative max-w-md"><p className="mb-5 text-xs font-bold uppercase tracking-[0.24em] text-[#d9a441]">Seu negócio, no ritmo certo</p><h1 className="font-heading text-5xl font-bold leading-[1.05] tracking-tight xl:text-6xl">Mais tempo para cuidar do que importa.</h1><p className="mt-6 max-w-sm text-base leading-7 text-[#c4d9d1]">Organize sua barbearia, acompanhe seus números e faça cada atendimento contar.</p><div className="mt-10 flex items-center gap-3 text-sm text-[#dcebe4]"><span className="grid size-7 place-items-center rounded-full bg-[#d9a441] text-[#123d39]"><FiCheck /></span> Tudo em um só lugar</div></div>
				<p className="relative text-xs text-[#8eb5aa]">© 2026 Corte & Cia</p>
			</section>
			<section className="flex w-full items-center justify-center px-5 py-10 sm:px-10 lg:w-[54%]"><div className="w-full max-w-md">
				<div className="mb-8 lg:hidden"><div className="mb-5 grid size-11 place-items-center rounded-xl bg-[#123d39] text-[#d9a441]"><FiScissors size={21} /></div><p className="font-heading text-xl font-bold">Corte & Cia</p></div>
				<div className="mb-9"><p className="mb-3 text-xs font-bold uppercase tracking-[0.2em] text-[#2c8279]">{registering ? 'Comece agora' : 'Bem-vindo de volta'}</p><h2 className="font-heading text-3xl font-bold tracking-tight sm:text-4xl">{registering ? 'Crie sua conta' : 'Acesse seu workspace'}</h2><p className="mt-3 text-sm leading-6 text-[#657773]">{registering ? 'Tenha uma visão mais clara da sua operação.' : 'Entre para acompanhar sua barbearia de onde estiver.'}</p></div>
				<form onSubmit={onSubmit} className="space-y-5">
					{registering && <Field label="Seu nome" icon={<FiUser />} error={form.formState.errors.nome?.message}><input {...form.register('nome')} placeholder="Marcos Silva" className="h-12 w-full rounded-lg border border-[#d9e2de] bg-white pl-10 pr-4 text-sm font-normal text-[#172624] outline-none transition placeholder:text-[#9aaba6] focus:border-[#2c8279] focus:ring-2 focus:ring-[#2c8279]/15" /></Field>}
					<Field label="E-mail" icon={<FiMail />} error={form.formState.errors.email?.message}><input {...form.register('email')} type="email" placeholder="voce@exemplo.com" className="h-12 w-full rounded-lg border border-[#d9e2de] bg-white pl-10 pr-4 text-sm font-normal text-[#172624] outline-none transition placeholder:text-[#9aaba6] focus:border-[#2c8279] focus:ring-2 focus:ring-[#2c8279]/15" /></Field>
					<Field label="Senha" icon={<FiLock />} error={form.formState.errors.senha?.message}><div className="relative"><input {...form.register('senha')} type={showPassword ? 'text' : 'password'} placeholder="••••••••" className="h-12 w-full rounded-lg border border-[#d9e2de] bg-white pl-10 pr-11 text-sm font-normal text-[#172624] outline-none transition placeholder:text-[#9aaba6] focus:border-[#2c8279] focus:ring-2 focus:ring-[#2c8279]/15" /><button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 top-1/2 -translate-y-1/2 text-[#78908a]" aria-label={showPassword ? 'Ocultar senha' : 'Mostrar senha'}>{showPassword ? <FiEyeOff /> : <FiEye />}</button></div></Field>
					{errorMessage && <p className="rounded-lg border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{errorMessage}</p>}
					<button type="submit" disabled={mutation.isPending} className="group flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-[#123d39] text-sm font-bold text-white transition hover:bg-[#1c5a53] disabled:cursor-not-allowed disabled:opacity-60">{mutation.isPending ? 'Aguarde...' : registering ? 'Criar minha conta' : 'Entrar no painel'}{!mutation.isPending && <FiArrowRight className="transition-transform group-hover:translate-x-1" />}</button>
				</form>
				<p className="mt-8 text-center text-sm text-[#657773]">{registering ? 'Já possui uma conta?' : 'Ainda não possui uma conta?'} <button type="button" onClick={switchMode} className="font-bold text-[#2c8279] hover:underline">{registering ? 'Entrar' : 'Criar conta'}</button></p>
			</div></section>
		</main>
	)
}

function Field({ label, icon, error, children }: { label: string; icon: ReactNode; error?: string; children: ReactNode }) {
	return <label className="block text-sm font-semibold text-[#29423d]"><span className="mb-2 block">{label}</span><span className="relative block"><span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-[#78908a]">{icon}</span>{children}</span>{error && <small className="mt-1 block font-normal text-red-600">{error}</small>}</label>
}
