import { test, expect } from '@playwright/test';
import { registerAndLogin, uniqueEmail } from './helpers';

test('create a TIME-based rule', async ({ page }) => {
  await registerAndLogin(page, uniqueEmail());

  // Setup: need a room + device for the rule action
  await page.goto('/rooms');
  await page.waitForSelector('.page-container');

  await page.getByText('Add Room').click();
  await page.getByLabel('Room name').fill('Rule Room');
  await page.getByRole('button', { name: 'Add Room' }).click();
  const roomChip = page.locator('.room-chip').filter({ hasText: 'Rule Room' });
  await expect(roomChip).toBeVisible({ timeout: 5000 });

  await roomChip.click();
  await page.getByRole('button', { name: '+ Add Device' }).click();
  await page.getByLabel('Device name').fill('Rule Switch');
  await page.getByText('Switch', { exact: true }).click();
  await page.getByRole('button', { name: 'Add Device' }).click();
  await expect(page.getByText('Rule Switch', { exact: true }).first()).toBeVisible({ timeout: 5000 });

  // Create rule
  await page.goto('/rules');
  await page.waitForSelector('.page-container');

  await page.locator('[data-testid="rule-add-fab"]').click();
  await expect(page.getByText('Create New Rule')).toBeVisible();

  // Step 1: Name
  await page.locator('[data-testid="rule-name-input"]').fill('Morning Rule');
  await page.locator('[data-testid="rule-name-next"]').click();

  // Step 2: Trigger (TIME)
  await page.locator('[data-testid="trigger-time"]').click();
  await page.locator('[data-testid="trigger-time-input"]').fill('07:00');
  await page.locator('[data-testid="day-Mon"]').click();
  await page.locator('[data-testid="trigger-next"]').click();

  // Step 3: Action
  await page.locator('[data-testid="action-room-select"]').click();
  await page.getByRole('option', { name: 'Rule Room' }).click();

  await page.locator('[data-testid="action-device-select"]').click();
  await page.getByRole('option', { name: 'Rule Switch' }).click();

  await page.locator('[data-testid="action-value-select"]').click();
  await page.getByRole('option', { name: 'Turn on' }).click();

  await page.locator('[data-testid="action-next"]').click();

  // Step 4: Save
  await page.locator('[data-testid="rule-save-btn"]').click();

  await expect(page.getByText('Morning Rule')).toBeVisible({ timeout: 5000 });
});
