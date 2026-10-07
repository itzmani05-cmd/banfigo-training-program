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
  .catch((err) => {
    console.error('Keycloak init failed', err)
    root.render(
      <div className="mx-auto mt-24 max-w-md rounded-lg border border-line bg-paper p-6 text-center shadow-sm">
        <h2>Could not connect to the login server</h2>
        <p className="mt-2 text-muted">Is Keycloak running? Check the browser console for details.</p>
      </div>,
    )
  })
