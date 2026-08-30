import { Outlet } from 'react-router-dom'
import { AppLayout } from './components/AppLayout'

function App() {
  return (
    <AppLayout>
      <Outlet />
    </AppLayout>
  )
}

export default App
