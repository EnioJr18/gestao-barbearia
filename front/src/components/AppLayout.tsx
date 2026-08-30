import { type ReactNode } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import {
  FiActivity,
  FiBarChart2,
  FiBox,
  FiGrid,
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
import { Separator } from '@/components/ui/separator'

const links = [
  { to: '/dashboard', label: 'Visão geral', icon: FiGrid },
  { to: '/produtos', label: 'Produtos e serviços', icon: FiBox },
  { to: '/clientes', label: 'Clientes', icon: FiUsers },
  { to: '/pedidos', label: 'Pedidos', icon: FiShoppingBag },
  { to: '/relatorios', label: 'Relatórios', icon: FiBarChart2 },
]

export function AppLayout({ children }: { children: ReactNode }) {
  const location = useLocation()
  const current = links.find((link) => link.to === location.pathname)
  return (
    <SidebarProvider>
      <Sidebar collapsible="icon">
        <SidebarHeader>
          <div className="flex items-center gap-2 px-2 py-3">
            <div className="grid size-8 shrink-0 place-items-center rounded-lg rounded-bl-sm bg-primary font-heading text-lg font-bold text-primary-foreground">
              C
            </div>
            <div className="group-data-[collapsible=icon]:hidden">
              <strong className="font-heading text-base">
                Corte <span className="text-amber-600">& Cia</span>
              </strong>
              <span className="block text-[10px] text-muted-foreground">
                gestão inteligente
              </span>
            </div>
          </div>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupLabel className="text-xs font-semibold tracking-wide">
              MENU PRINCIPAL
            </SidebarGroupLabel>
            <SidebarMenu>
              {links.map(({ to, label, icon: Icon }) => (
                <SidebarMenuItem key={to}>
                  <SidebarMenuButton
                    render={<NavLink to={to} />}
                    isActive={current?.to === to}
                    tooltip={label}
                    className="text-sm [&_svg]:size-5 group-data-[collapsible=icon]:justify-center group-data-[collapsible=icon]:[&>svg]:size-4"
                  >
                    <>
                      <Icon />
                      <span>{label}</span>
                      {to === '/produtos' && (
                        <span className="ml-auto text-[10px]">4</span>
                      )}
                    </>
                  </SidebarMenuButton>
                </SidebarMenuItem>
              ))}
            </SidebarMenu>
          </SidebarGroup>
          <SidebarGroup className="mt-auto group-data-[collapsible=icon]:hidden">
            <div className="grid gap-1 rounded-lg bg-amber-50 p-3 text-amber-950">
              <FiActivity className="text-amber-600" />
              <strong className="text-xs">Seu negócio em movimento</strong>
              <span className="text-[10px] leading-4 text-amber-800/70">
                Acompanhe seus resultados todos os dias.
              </span>
            </div>
          </SidebarGroup>
        </SidebarContent>
        <SidebarFooter>
          <SidebarMenu>
            <SidebarMenuItem>
              <SidebarMenuButton className="text-sm [&_svg]:size-5">
                <div className="grid size-7 shrink-0 place-items-center rounded-full bg-emerald-100 text-[10px] font-bold text-primary">
                  MS
                </div>
                <span>
                  <strong className="block text-xs">Marcos Silva</strong>
                  <small className="text-[10px] text-muted-foreground">
                    Administrador
                  </small>
                </span>
              </SidebarMenuButton>
            </SidebarMenuItem>
          </SidebarMenu>
        </SidebarFooter>
        <SidebarRail />
      </Sidebar>
      <SidebarInset>
        <header className="flex h-16 shrink-0 items-center gap-3 border-b bg-background px-4 md:px-8">
          <SidebarTrigger
            className="size-9 [&_svg]:size-5"
            aria-label="Abrir ou fechar menu"
          />
          <Separator orientation="vertical" className="h-5" />
          <div className="flex items-center gap-2 text-xs text-muted-foreground">
            <span>Workspace</span>
            <span>/</span>
            <strong className="text-foreground">
              {current?.label ?? 'Visão geral'}
            </strong>
          </div>
          <div className="ml-auto flex items-center gap-2 text-xs text-muted-foreground">
            <span className="size-1.5 rounded-full bg-emerald-500" />
            <span className="hidden sm:inline">Operação aberta</span>
            <div className="grid size-7 place-items-center rounded-full bg-emerald-100 text-[10px] font-bold text-primary">
              MS
            </div>
          </div>
        </header>
        <main className="min-h-[calc(100vh-4rem)] bg-muted/30">
          <div className="mx-auto w-full max-w-[1600px] p-4 md:px-10 md:py-8 xl:px-14">
            {children}
          </div>
        </main>
      </SidebarInset>
    </SidebarProvider>
  )
}
