import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import './index.css'
import './App.css'
import App from './App.tsx'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
// import {Home} from './pages/Home.tsx';
import { Dashboard } from './pages/Dashboard.tsx'
import { Produtos } from './pages/Produtos.tsx'
import { Servicos } from './pages/Servicos.tsx'
import { Caixa } from './pages/Caixa.tsx'
import { Clientes } from './pages/Clientes.tsx'
import { Pedidos } from './pages/Pedidos.tsx'
import { Relatorios } from './pages/Relatorios.tsx'
import { Login } from './pages/Login.tsx'
import { ProtectedRoute } from './components/ProtectedRoute.tsx'

const queryClient = new QueryClient()

const router = createBrowserRouter([
  {
    path: '/',
    element: (
      <ProtectedRoute>
        <App />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'produtos', element: <Produtos /> },
      { path: 'servicos', element: <Servicos /> },
      { path: 'caixa', element: <Caixa /> },
      { path: 'clientes', element: <Clientes /> },
      { path: 'pedidos', element: <Pedidos /> },
      { path: 'relatorios', element: <Relatorios /> },
    ],
  },
  { path: '/login', element: <Login /> },
  // {
  //   path: "/pagamento/sucesso",
  //   element: <Sucesso />,
  // },
  // { path: "/login", element: <Login /> },
  // { path: "/register", element: <Register /> },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  </StrictMode>,
)
