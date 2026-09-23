import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  retries: process.env['CI'] ? 2 : 0,
  reporter: [['html', { open: 'never' }]],
  use: { baseURL: 'http://localhost:4200', trace: 'on-first-retry' },
  webServer: {
    command: 'npx nx serve frontend',
    url: 'http://localhost:4200',
    reuseExistingServer: !process.env['CI']
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }]
});
