import { NgModule } from '@angular/core';
import { provideConfig } from '@xyz/storefront';

/**
 * 自社の OCC の設定。標準の端点の fields を、自社の部品で使う項目に合わせて上書きする。
 * 一覧と検索(productSearchList)には、商品カードで出すブランドの名前と新着の印を足している。
 * 標準の値は xyz-storefront-lib/occ/default-occ-config.ts。上書きするときは、標準の項目を落とさないこと。
 */
@NgModule({
  providers: [
    provideConfig({
      backend: {
        occ: {
          endpoints: {
            productSearchList:
              'products/search?fields=products(code,name,summary,price(FULL),images(DEFAULT),stock(FULL),averageRating,numberOfReviews,brand(BASIC),newArrival),facets,breadcrumbs,pagination(DEFAULT),sorts(DEFAULT),freeTextSearch,currentQuery',
          },
        },
      },
    }),
  ],
})
export class XyzOccConfigModule {}
