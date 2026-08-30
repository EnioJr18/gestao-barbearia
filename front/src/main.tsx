import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import './App.css'
import App from './App.tsx'
import { createBrowserRouter, RouterProvider } from 'react-router-dom'
// import {Home} from './pages/Home.tsx';
import { Dashboard } from './pages/Dashboard.tsx'
import { Produtos } from './pages/Produtos.tsx'
import { Clientes } from './pages/Clientes.tsx'
import { Pedidos } from './pages/Pedidos.tsx'
import { Relatorios } from './pages/Relatorios.tsx'

const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: 'dashboard', element: <Dashboard /> },
      { path: 'produtos', element: <Produtos /> },
      { path: 'clientes', element: <Clientes /> },
      { path: 'pedidos', element: <Pedidos /> },
      { path: 'relatorios', element: <Relatorios /> },
    ],
  },
  // {
  //   path: "/pagamento/sucesso",
  //   element: <Sucesso />,
  // },
  // { path: "/login", element: <Login /> },
  // { path: "/register", element: <Register /> },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <RouterProvider router={router} />
  </StrictMode>,
)
