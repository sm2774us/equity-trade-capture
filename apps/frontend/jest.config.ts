export default {
  displayName: 'frontend',
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  coverageDirectory: '../../coverage/apps/frontend',
  // Playwright e2e specs live under apps/frontend/e2e/ and must never be
  // picked up by Jest — they use @playwright/test's own test runner and
  // fail with a confusing error ("needs to be invoked via 'npx playwright
  // test'") if Jest tries to execute them as unit tests.
  testPathIgnorePatterns: ['<rootDir>/e2e/'],
  coverageThreshold: {
    global: { branches: 75, functions: 80, lines: 80, statements: 80 }
  }
};
