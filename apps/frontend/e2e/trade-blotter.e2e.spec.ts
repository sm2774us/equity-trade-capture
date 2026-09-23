import { test, expect } from '@playwright/test';

test.describe('Trade Blotter', () => {
  test('loads and shows the blotter table with the default book filter', async ({ page }) => {
    await page.goto('/blotter');
    await expect(page.getByRole('heading', { name: 'Trade Blotter' })).toBeVisible();
    await expect(page.getByLabel('Book ID')).toHaveValue('SYSMACRO-EQ-01');
  });

  test('navigates to positions dashboard', async ({ page }) => {
    await page.goto('/blotter');
    await page.getByRole('link', { name: 'Positions & P&L' }).click();
    await expect(page).toHaveURL(/positions/);
    await expect(page.getByRole('heading', { name: 'Positions & P&L' })).toBeVisible();
  });

  test('navigates to manual trade entry and validates required fields', async ({ page }) => {
    await page.goto('/new-trade');
    await page.getByRole('button', { name: 'Capture Trade' }).click();
    await expect(page.getByLabel('External Order ID')).toHaveAttribute('aria-invalid', 'true');
  });
});
