import { expect, test } from '@playwright/test';
import { IN_STOCK, open } from './support';

test.describe('商品詳細', () => {
  test('名前、価格、在庫が出る(SSR の HTML にも入っている) @mock', async ({ page, request }) => {
    await open(page, `/product/${IN_STOCK[0]}`);

    await expect(page.getByTestId('product-name')).not.toBeEmpty();
    await expect(page.getByTestId('product-price')).toContainText('¥');
    await expect(page.getByTestId('product-stock')).toContainText('在庫あり');

    // モックの開発サーバーは SSR しないので、SSR の確かめは本物の環境だけで行う
    if (test.info().project.name === 'real') {
      const html = await (await request.get(`/product/${IN_STOCK[0]}`)).text();
      expect(html).toContain('data-testid="product-name"');
    }
  });

  test('一覧から商品をクリックすると、詳細が開く', async ({ page }) => {
    await open(page, '/category/cat-01');
    const first = page.getByTestId('product-item').first().locator('a').first();
    const name = (await page.getByTestId('product-item').first().locator('.name').textContent())?.trim() ?? '';
    await first.click();

    await expect(page).toHaveURL(/\/product\//);
    await expect(page.getByTestId('product-name')).toHaveText(name);
  });

  test('在庫切れの商品には「カートに入れる」が出ない', async ({ page }) => {
    await open(page, '/product/XYZ-000002');
    await expect(page.getByTestId('product-stock')).toContainText('在庫切れ');
    await expect(page.getByTestId('add-to-cart')).toHaveCount(0);
  });
});
