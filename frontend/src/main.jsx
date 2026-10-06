import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'
import App from './App.jsx'
import { initAuth } from './auth.js'

const root = createRoot(document.getElementById('root'))

initAuth()
  .then(() => {
    root.render(
      <StrictMode>
        <App />
      </StrictMode>,
    )
  })
  .catch(() => {
    root.render(<p className="error">Could not connect to the login server. Is Keycloak running?</p>)
  })
