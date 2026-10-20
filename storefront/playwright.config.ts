import { defineConfig, devices } from '@playwright/test';

/**
 * E2E の設定。2 通りで回す:
 * - real … Docker Compose で起動した環境(SSR のストアフロント :4000 と疑似 Commerce :9002)。npm run e2e -- --project=real
 * - mock … MSW のモックで動かす開発サーバー(:4200)。バックエンドは要らない。npm run e2e -- --project=mock
 */
const realBaseUrl = process.env['E2E_BASE_URL'] ?? 'http://localhost:4000';

export default defineConfig({
  testDir: './e2e',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    locale: 'ja-JP',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'real',
      use: { ...devices['Desktop Chrome'], baseURL: realBaseUrl },
    },
    {
      name: 'mock',
      grep: /@mock/,
      use: { ...devices['Desktop Chrome'], baseURL: 'http://localhost:4200' },
    },
  ],
  webServer: process.env['E2E_MOCK']
    ? { command: 'npm run start:mock -- --port 4200', url: 'http://localhost:4200', reuseExistingServer: true, timeout: 180_000 }
    : undefined,
});
