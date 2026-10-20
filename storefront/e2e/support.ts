import { Page } from '@playwright/test';

/** 在庫のある商品(テストデータの種を変えたら確かめ直す)。 */
export const IN_STOCK = ['XYZ-000001', 'XYZ-000003', 'XYZ-000004', 'XYZ-000005'];

/**
 * ページを開き、通信が落ち着く(= SSR の HTML にブラウザ側の Angular がつながった)まで待つ。
 * つながる前に押した操作は、取りこぼされたり、古い値で再生されたりすることがあるため。
 */
export async function open(page: Page, url: string): Promise<void> {
  await page.goto(url);
  await page.waitForLoadState('networkidle');
}
