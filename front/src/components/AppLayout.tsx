import { type ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  FiBarChart2,
  FiBox,
  FiDollarSign,
  FiGrid,
  FiLogOut,
  FiScissors,
  FiShoppingBag,
  FiUsers,
} from 'react-icons/fi'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarRail,
  SidebarTrigger,
} from '@/components/ui/sidebar'
import { useMeUser } from '@/hooks/user/useMeUser'
import { useCurrentCaixa } from '@/hooks/caixa/useCurrentCaixa'

const links = [
  { to: '/dashboard', label: 'Visão geral', icon: FiGrid },
  { to: '/relatorios', label: 'Relatórios', icon: FiBarChart2 },
  { to: '/caixa', label: 'Caixa', icon: FiDollarSign },
  { to: '/produtos', label: 'Produtos', icon: FiBox },
  { to: '/servicos', label: 'Serviços', icon: FiScissors },
  { to: '/pedidos', label: 'Pedidos', icon: FiShoppingBag },
  { to: '/clientes', label: 'Clientes', icon: FiUsers },
]

function getInitials(name: string) {
  const initials = name
    .trim()
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')

  return initials.toUpperCase() || 'U'
}

export function AppLayout({ children }: { children: ReactNode }) {
  const location = useLocation()
  const navigate = useNavigate()
  const { data: user } = useMeUser()
  const { data: caixa, isLoading: caixaLoading, error: caixaError } =
    useCurrentCaixa()
  const current = links.find((link) =>
    location.pathname === '/'
      ? link.to === '/dashboard'
      : link.to === location.pathname,
  )
  const userName = user?.nome ?? 'Usuário'
  const caixaStatusLabel = caixaLoading
    ? 'Carregando...'
    : caixaError
      ? 'Indisponível'
      : caixa?.status === 'ABERTO'
        ? 'Aberto'
        : 'Fechado'
  const caixaStatusColor =
    caixaLoading
      ? 'bg-muted-foreground'
      : caixa?.status === 'ABERTO'
        ? 'bg-emerald-500'
        : 'bg-red-500'

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('tipoToken')
    navigate('/login', { replace: true })
  }

  return (
    <SidebarProvider className='bg-[#0c0806]'>
      <Sidebar collapsible="icon" className="border-sidebar-border">
        <SidebarHeader className="px-5 py-5 group-data-[collapsible=icon]:px-0">
          <div className="flex w-full items-center gap-2 group-data-[collapsible=icon]:justify-center">
            <div className="grid size-10 shrink-0 place-items-center rounded-full bg-primary text-xl font-bold text-primary-foreground">
              <FiScissors />
            </div>
            <div className="group-data-[collapsible=icon]:hidden">
              <strong className="font-heading text-base tracking-tight">
                Barbearia Neves
              </strong>
              <span className="block text-[14px] text-muted-foreground">
                Gestão da barbearia
              </span>
            </div>
          </div>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup className="group-data-[collapsible=icon]:items-center">
            <SidebarMenu className="group-data-[collapsible=icon]:items-center">
              {links.map(({ to, label, icon: Icon}) => (
                <SidebarMenuItem key={to}>
                  <SidebarMenuButton
                    render={<NavLink to={to} />}
                    isActive={current?.to === to}
                    tooltip={label}
                    className="h-11 justify-start gap-3 rounded-lg px-3 text-sm font-medium hover:bg-primary/20 hover:text-primary data-active:bg-primary data-active:font-semibold data-active:text-primary-foreground data-active:shadow-[0_0_0_1px_var(--primary),0_4px_14px_color-mix(in_oklab,var(--primary)_30%,transparent)] [&_svg]:size-5 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0"
                  >
                    <span className="flex size-6 shrink-0 items-center justify-center">
                      <Icon />
                    </span>
                    <span className="group-data-[collapsible=icon]:hidden text-[18px] font-medium">
                      {label}
                    </span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter className="p-4">
          <div className="mb-3 rounded-xl bg-sidebar-accent p-3 group-data-[collapsible=icon]:hidden">
            <span className="block text-[14px] text-muted-foreground">Status do caixa</span>
            <span className="mt-1 flex items-center gap-1.5 text-[14px] font-semibold">
              <span className={`size-3 rounded-full ${caixaStatusColor}`} />
              {caixaStatusLabel}
            </span>
          </div>
          <SidebarMenu className="gap-2 group-data-[collapsible=icon]:items-center">
            <SidebarMenuItem>
              <SidebarMenuButton
                size="lg"
                className="justify-start gap-3 text-sm [&_svg]:size-5 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0"
              >
                <div className="grid size-8 shrink-0 place-items-center rounded-full bg-emerald-100 text-xs font-bold text-primary">
                  {getInitials(userName)}
                </div>
                <span className="min-w-0 group-data-[collapsible=icon]:hidden">
                  <strong className="block whitespace-nowrap text-sm">
                    {userName}
                  </strong>
                  <small className="text-xs text-muted-foreground">
                    Administrador
                  </small>
                </span>
              </SidebarMenuButton>
            </SidebarMenuItem>
            <SidebarMenuItem>
              <SidebarMenuButton
                type="button"
                onClick={handleLogout}
                className="justify-start text-sm text-muted-foreground hover:bg-sidebar-accent hover:text-foreground [&_svg]:size-5 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:px-0"
                tooltip="Sair"
              >
                <span className="flex size-6 shrink-0 items-center justify-center">
                  <FiLogOut />
                </span>
                <span className="group-data-[collapsible=icon]:hidden">
                  Sair
                </span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <main className="min-h-svh bg-[#110e0b]">
          <SidebarTrigger
            className="m-3 size-10 text-2xl [&_svg]:size-5"
            aria-label="Abrir ou fechar menu"
          />
          <div className="mx-auto w-full px-10 py-6 bg-[#110e0b]">
            {children}
          </div>
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
