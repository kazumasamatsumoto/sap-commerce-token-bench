import { expect, test } from '@playwright/test';
import { IN_STOCK, open } from './support';

test.describe('カート', () => {
  test('カートに入れると、ミニカートとカートの画面に出て、数量を変えられる @mock', async ({ page }) => {
    await open(page, `/product/${IN_STOCK[1]}`);
    await page.getByTestId('add-to-cart').click();
    await expect(page.getByTestId('added-to-cart')).toBeVisible();
    await expect(page.getByTestId('mini-cart-count')).toHaveText('1');

    await open(page, '/cart');
    await expect(page.getByTestId('cart-entry')).toHaveCount(1);
    const totalBefore = await page.getByTestId('cart-total').textContent();

    await page.getByTestId('cart-entry-quantity').fill('3');
    await page.getByTestId('cart-entry-quantity').blur();
    await expect(page.getByTestId('mini-cart-count')).toHaveText('3');
    await expect(page.getByTestId('cart-total')).not.toHaveText(totalBefore ?? '');
  });

  test('空のカートでは「カートは空です」と出る', async ({ page }) => {
    await open(page, '/cart');
    await expect(page.getByTestId('cart-empty')).toBeVisible();
  });
});
