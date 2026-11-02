import { type ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  FiBarChart2,
  FiBox,
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

const links = [
  { to: '/dashboard', label: 'Visão geral', icon: FiGrid },
  { to: '/relatorios', label: 'Relatórios', icon: FiBarChart2, badge: 'Top' },
  { to: '/produtos', label: 'Produtos e serviços', icon: FiBox },
  { to: '/pedidos', label: 'Pedidos', icon: FiShoppingBag },
  { to: '/clientes', label: 'Clientes', icon: FiUsers },
  { to: '/servicos', label: 'Serviços', icon: FiScissors },
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
  const current = links.find((link) =>
    location.pathname === '/'
      ? link.to === '/dashboard'
      : link.to === location.pathname,
  )
  const userName = user?.nome ?? 'Usuário'

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('tipoToken')
    navigate('/login', { replace: true })
  }

  return (
    <SidebarProvider>
      <Sidebar collapsible="icon" className="border-sidebar-border">
        <SidebarHeader className="px-5 py-5">
          <div className="flex items-center gap-2 group-data-[collapsible=icon]:justify-center">
            <div className="grid size-9 shrink-0 place-items-center rounded-full bg-primary text-lg font-bold text-primary-foreground">
              <FiScissors />
            </div>
            <div className="group-data-[collapsible=icon]:hidden">
              <strong className="font-heading text-base tracking-tight">
                Navalha
              </strong>
              <span className="block text-[10px] text-muted-foreground">
                Gestão da barbearia
              </span>
            </div>
          </div>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <SidebarMenu>
              {links.map(({ to, label, icon: Icon, badge }) => (
                <SidebarMenuItem key={to}>
                  <SidebarMenuButton
                    render={<NavLink to={to} />}
                    isActive={current?.to === to}
                    tooltip={label}
                    className="h-9 justify-start gap-3 rounded-lg px-3 text-xs font-medium [&_svg]:size-4 group-data-[collapsible=icon]:justify-center"
                  >
                    <span className="flex size-5 shrink-0 items-center justify-center">
                      <Icon />
                    </span>
                    <span className="group-data-[collapsible=icon]:hidden">
                      {label}
                    </span>
                    {badge && (
                      <span className="ml-auto rounded-full bg-primary/15 px-2 py-0.5 text-[9px] font-bold text-primary group-data-[collapsible=icon]:hidden">
                        {badge}
                      </span>
                    )}
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter className="p-4">
          <div className="mb-3 rounded-xl bg-sidebar-accent p-3 group-data-[collapsible=icon]:hidden">
            <span className="block text-[10px] text-muted-foreground">Status do caixa</span>
            <span className="mt-1 flex items-center gap-1.5 text-xs font-semibold">
              <span className="size-1.5 rounded-full bg-emerald-500" />
              Aberto
            </span>
          </div>
          <SidebarMenu className="gap-2">
            <SidebarMenuItem>
              <SidebarMenuButton
                size="lg"
                className="justify-start gap-3 text-xs [&_svg]:size-4 group-data-[collapsible=icon]:justify-center"
              >
                <div className="grid size-7 shrink-0 place-items-center rounded-full bg-emerald-100 text-[10px] font-bold text-primary">
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
                className="justify-start text-xs text-muted-foreground hover:bg-sidebar-accent hover:text-foreground [&_svg]:size-4 group-data-[collapsible=icon]:justify-center"
                tooltip="Sair"
              >
                <span className="flex size-5 shrink-0 items-center justify-center">
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
        <main className="min-h-svh bg-background">
          <SidebarTrigger
            className="absolute left-3 top-3 z-10 size-8 text-2xl md:hidden"
            aria-label="Abrir ou fechar menu"
          />
          <div className="mx-auto w-full max-w-[1180px] p-5 pt-14 sm:p-7 sm:pt-14 lg:p-8 lg:pt-8">
            {children}
          </div>
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
