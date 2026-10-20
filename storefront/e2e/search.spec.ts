import { expect, test } from '@playwright/test';
import { open } from './support';

test.describe('商品検索', () => {
  test('キーワードで検索すると、件数と商品が出る @mock', async ({ page }) => {
    await open(page, '/');
    await page.getByRole('searchbox').fill('ゲーム');
    await page.getByRole('button', { name: '検索' }).click();

    await expect(page).toHaveURL(/\/search\?query=/);
    await expect(page.getByTestId('result-count')).toContainText('件');
    await expect(page.getByTestId('product-item').first()).toBeVisible();
  });

  test('カテゴリのメニューから一覧を開き、並べ替えられる', async ({ page }) => {
    await open(page, '/');
    await page.getByRole('navigation', { name: 'カテゴリ' }).getByRole('link').first().click();

    await expect(page).toHaveURL(/\/category\//);
    await expect(page.getByTestId('product-item')).toHaveCount(12);
    await page.waitForLoadState('networkidle');

    const sorted = page.waitForResponse((r) => r.url().includes('/products/search') && r.url().includes('sort=price-asc'));
    await page.getByLabel('並べ替え').selectOption('price-asc');
    await expect(page).toHaveURL(/sort=price-asc/);
    await sorted;
    await expect(page.getByTestId('product-item')).toHaveCount(12);
    const prices = await page.getByTestId('product-item').locator('.price').allTextContents();
    const values = prices.map((p) => Number(p.replace(/[^\d]/g, '')));
    expect(values).toEqual([...values].sort((a, b) => a - b));
  });
});
