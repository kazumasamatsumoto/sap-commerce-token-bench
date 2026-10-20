import { expect, test } from '@playwright/test';
import { IN_STOCK, open } from './support';

test.describe('チェックアウト', () => {
  test('ゲストで、届け先・配送方法・支払いを入れて注文を確定できる @mock', async ({ page }) => {
    await open(page, `/product/${IN_STOCK[2]}`);
    await page.getByTestId('add-to-cart').click();
    await expect(page.getByTestId('mini-cart-count')).toHaveText('1');

    await open(page, '/cart');
    await page.getByTestId('proceed-to-checkout').click();
    await expect(page).toHaveURL(/\/checkout/);
    await page.waitForLoadState('networkidle');

    const address = page.getByTestId('address-form');
    await address.getByLabel('メールアドレス').fill('e2e@example.com');
    await address.getByLabel('姓').fill('山田');
    await address.getByLabel('名', { exact: true }).fill('太郎');
    await address.getByLabel('郵便番号').fill('100-0001');
    await address.getByLabel('市区町村').fill('千代田区');
    await address.getByLabel('番地・建物名').fill('千代田1-1');
    await address.getByRole('button', { name: '次へ' }).click();

    await page.getByTestId('delivery-modes').getByRole('radio').first().check();

    const payment = page.getByTestId('payment-form');
    await payment.getByLabel('カードの名義').fill('TARO YAMADA');
    await payment.getByLabel('カード番号').fill('4111111111111111');
    await payment.getByLabel('有効期限(月)').fill('12');
    await payment.getByLabel('有効期限(年)').fill('2030');
    await page.getByTestId('place-order').click();

    await expect(page).toHaveURL(/\/order-confirmation\?guid=/);
    await expect(page.getByTestId('order-confirmation')).toBeVisible();
    await expect(page.getByTestId('order-code')).not.toBeEmpty();
    await expect(page.getByTestId('mini-cart-count')).toHaveText('0');
  });
});
