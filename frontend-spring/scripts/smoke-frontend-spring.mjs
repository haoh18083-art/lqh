const baseUrl = process.env.SMOKE_BASE_URL || 'http://127.0.0.1:3000'
const routes = ['/', '/login', '/student/dashboard', '/admin/dashboard']

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function waitForServer() {
  for (let attempt = 1; attempt <= 20; attempt += 1) {
    try {
      const response = await fetch(`${baseUrl}/`)
      if (response.ok) {
        return
      }
    } catch {
      // Ignore and retry until the Vite dev server is ready.
    }
    await sleep(1000)
  }

  throw new Error(`frontend-spring did not become ready at ${baseUrl}`)
}

async function assertRoute(route) {
  const response = await fetch(`${baseUrl}${route}`)
  if (!response.ok) {
    throw new Error(`Route ${route} returned ${response.status}`)
  }

  const html = await response.text()
  if (!html.includes('<div id="app"></div>')) {
    throw new Error(`Route ${route} did not return the SPA shell`)
  }
}

async function assertProxyHealth() {
  const apiResponse = await fetch(`${baseUrl}/api/v1/public/departments?page=1&page_size=1`)
  if (!apiResponse.ok) {
    throw new Error(`API proxy returned ${apiResponse.status}`)
  }

  const apiPayload = await apiResponse.json()
  if (!apiPayload?.success) {
    throw new Error('API proxy did not return a successful payload')
  }

  const wsResponse = await fetch(`${baseUrl}/ws/alerts/info?t=${Date.now()}`)
  if (!wsResponse.ok) {
    throw new Error(`WebSocket proxy returned ${wsResponse.status}`)
  }

  const wsPayload = await wsResponse.json()
  if (typeof wsPayload?.websocket !== 'boolean') {
    throw new Error('WebSocket proxy info payload is invalid')
  }
}

await waitForServer()

for (const route of routes) {
  await assertRoute(route)
}

await assertProxyHealth()

console.log(
  JSON.stringify(
    {
      baseUrl,
      routesChecked: routes,
      proxyChecks: ['api:/api/v1/public/departments', 'ws:/ws/alerts/info'],
      success: true
    },
    null,
    2
  )
)
