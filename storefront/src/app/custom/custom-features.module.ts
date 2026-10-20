import { NgModule } from '@angular/core';
import { BrandsModule } from './brands/brands.module';
import { XyzOccConfigModule } from './config/xyz-occ-config.module';
import { CartExtrasModule } from './cart-extras/cart-extras.module';
import { CheckoutExtrasModule } from './checkout-extras/checkout-extras.module';
import { ContentReplacementsModule } from './content-replacements/content-replacements.module';
import { FooterExtrasModule } from './footer-extras/footer-extras.module';
import { HeaderExtrasModule } from './header-extras/header-extras.module';
import { ProductBadgesModule } from './product-badges/product-badges.module';
import { ProductInfoModule } from './product-info/product-info.module';
import { ProductReviewsModule } from './product-reviews/product-reviews.module';
import { RecentlyViewedModule } from './recently-viewed/recently-viewed.module';
import { CustomSharedModule } from './shared';
import { WishlistModule } from './wishlist/wishlist.module';

/**
 * 自社の部品の機能モジュールをまとめたもの。spartacus-features.module.ts から import する。
 * 同じ Outlet に足す部品は、ここに並べた順に出る(例: PDP.ADDTOCART の後ろは お気に入り → 素材 → 送料と返品 → URL のコピー)。
 * 部品の一覧は docs/design/custom-components.md。
 */
@NgModule({
  imports: [
    XyzOccConfigModule,
    CustomSharedModule,
    ContentReplacementsModule,
    HeaderExtrasModule,
    ProductBadgesModule,
    WishlistModule,
    ProductInfoModule,
    ProductReviewsModule,
    BrandsModule,
    RecentlyViewedModule,
    CartExtrasModule,
    CheckoutExtrasModule,
    FooterExtrasModule,
  ],
})
export class CustomFeaturesModule {}
