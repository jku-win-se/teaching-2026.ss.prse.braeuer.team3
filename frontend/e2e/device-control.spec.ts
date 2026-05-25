import { test, expect } from '@playwright/test';
import { registerAndLogin, uniqueEmail } from './helpers';

test('create room, add switch device, toggle on', async ({ page }) => {
  await registerAndLogin(page, uniqueEmail());

  await page.goto('/rooms');
  await page.waitForSelector('.page-container');

  // Add room
  await page.getByText('Add Room').click();
  await page.getByLabel('Room name').fill('E2E Room');
  await page.getByRole('button', { name: 'Add Room' }).click();
  const roomChip = page.locator('.room-chip').filter({ hasText: 'E2E Room' });
  await expect(roomChip).toBeVisible({ timeout: 5000 });

  // Select room and add device
  await roomChip.click();
  await page.getByRole('button', { name: '+ Add Device' }).click();
  await page.getByLabel('Device name').fill('E2E Switch');
  await page.getByText('Switch', { exact: true }).click();
  await page.getByRole('button', { name: 'Add Device' }).click();
  await expect(page.getByText('E2E Switch', { exact: true }).first()).toBeVisible({ timeout: 5000 });

  // Toggle the switch on
  await page.locator('mat-slide-toggle').first().click();
  await expect(page.getByText(/turned on/)).toBeVisible({ timeout: 5000 });
});
