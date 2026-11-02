import { type ReactNode } from 'react'
import { NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  FiActivity,
  FiBarChart2,
  FiBox,
  FiGrid,
  FiLogOut,
  FiShoppingBag,
  FiUsers,
} from 'react-icons/fi'
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupLabel,
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
  { to: '/produtos', label: 'Produtos e serviços', icon: FiBox },
  { to: '/clientes', label: 'Clientes', icon: FiUsers },
  { to: '/pedidos', label: 'Pedidos', icon: FiShoppingBag },
  { to: '/relatorios', label: 'Relatórios', icon: FiBarChart2 },
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
  const current = links.find((link) => link.to === location.pathname)
  const userName = user?.nome ?? 'Usuário'

  const handleLogout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('tipoToken')
    navigate('/login', { replace: true })
  }

  return (
    <SidebarProvider>
      <Sidebar collapsible="icon">
        <SidebarHeader>
          <div className="flex items-center gap-2 group-data-[collapsible=icon]:justify-center">
            <div className="grid size-8 shrink-0 place-items-center rounded-lg rounded-bl-sm bg-primary font-heading text-lg font-bold text-primary-foreground">
              C
            </div>
            <div className="group-data-[collapsible=icon]:hidden">
              <strong className="font-heading text-xl">
                Corte <span className="text-amber-600">& Cia</span>
              </strong>
              <span className="block text-sm text-muted-foreground">
                gestão inteligente
              </span>
            </div>
          </div>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel className="text-sm font-semibold tracking-wide">
              MENU PRINCIPAL
            </SidebarGroupLabel>
            <SidebarMenu>
              {links.map(({ to, label, icon: Icon }) => (
                <SidebarMenuItem key={to}>
                  <SidebarMenuButton
                    render={<NavLink to={to} />}
                    isActive={current?.to === to}
                    tooltip={label}
                    className="justify-start text-base [&_svg]:size-5 group-data-[collapsible=icon]:justify-center"
                  >
                    <span className="flex size-5 shrink-0 items-center justify-center">
                      <Icon />
                    </span>
                    <span className="group-data-[collapsible=icon]:hidden">
                      {label}
                    </span>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroup>
          <SidebarGroup className="mt-auto group-data-[collapsible=icon]:hidden">
            <div className="grid gap-1 rounded-lg bg-amber-50 p-3 text-amber-950">
              <FiActivity className="size-5 text-amber-600" />
              <strong className="text-sm">Seu negócio em movimento</strong>
              <span className="text-xs leading-4 text-amber-800/70">
                Acompanhe seus resultados todos os dias.
              </span>
            </div>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter>
          <SidebarMenu className="gap-2">
            <SidebarMenuItem>
              <SidebarMenuButton
                size="lg"
                className="justify-start gap-3 text-base [&_svg]:size-5 group-data-[collapsible=icon]:justify-center"
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
                className="justify-start text-base text-red-700 hover:bg-red-50 hover:text-red-800 [&_svg]:size-5 group-data-[collapsible=icon]:justify-center"
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
        <main className="min-h-[calc(100vh-4rem)] bg-muted/30">
          <SidebarTrigger
            className="size-10 [&_svg]:size-5 m-3 text-2xl"
            aria-label="Abrir ou fechar menu"
          />
          <div className="mx-auto w-full p-4 md:px-15 md:py-5 xl:px-30">
            {children}
          </div>
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
