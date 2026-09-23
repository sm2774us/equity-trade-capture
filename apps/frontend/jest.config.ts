export default {
  displayName: 'frontend',
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  coverageDirectory: '../../coverage/apps/frontend',
  transform: { '^.+\\.(ts|mjs|js|html)$': 'jest-preset-angular' },
  coverageThreshold: {
    global: { branches: 75, functions: 80, lines: 80, statements: 80 }
  }
};
