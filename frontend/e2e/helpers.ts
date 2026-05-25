import { Page } from '@playwright/test';

export async function registerAndLogin(page: Page, email: string, password = 'TestPass123', name = 'E2E Tester') {
  await page.goto('/register');
  await page.getByLabel('Full name').fill(name);
  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Create account' }).click();
  await page.waitForURL('**/login');

  await page.getByLabel('Email address').fill(email);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign in' }).click();
  await page.waitForURL('**/dashboard');
}

export function uniqueEmail(): string {
  return `e2e_${Date.now()}@test.local`;
}
