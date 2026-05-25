import { test, expect } from '@playwright/test';

test('register and login', async ({ page }) => {
  const email = `e2e_${Date.now()}@test.local`;

  await page.goto('/register');
  await page.getByLabel('Full name').fill('E2E User');
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill('TestPass123');
  await page.getByRole('button', { name: 'Create account' }).click();
  await page.waitForURL('**/login');

  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill('TestPass123');
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('**/dashboard');

  await expect(page.getByText('Total Devices')).toBeVisible();
});
