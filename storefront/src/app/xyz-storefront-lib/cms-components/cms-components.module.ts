import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { CmsConfig } from '../cms/config/cms-config';
import { provideDefaultConfig } from '../config/config';
import { StorefrontComponentsModule } from '../storefront-components.module';
import { CartDetailsComponent } from './cart/cart-details.component';
import { CheckoutComponent } from './checkout/checkout.component';
import { OrderConfirmationComponent } from './checkout/order-confirmation.component';
import { BannerComponent, LinkComponent, ParagraphComponent, ProductCarouselComponent } from './content/content-components';
import {
  CategoryNavigationComponent,
  FooterNavigationComponent,
  MiniCartComponent,
  SearchBoxComponent,
  SiteLogoComponent,
} from './navigation/header-components';
import { ProductDetailsComponent } from './product/product-details.component';
import { ProductFacetNavigationComponent, ProductListComponent } from './product/product-list.component';
import { MediaComponent } from './shared/media.component';

const COMPONENTS = [
  MediaComponent,
  SiteLogoComponent,
  SearchBoxComponent,
  MiniCartComponent,
  CategoryNavigationComponent,
  FooterNavigationComponent,
  ParagraphComponent,
  LinkComponent,
  BannerComponent,
  ProductCarouselComponent,
  ProductListComponent,
  ProductFacetNavigationComponent,
  ProductDetailsComponent,
  CartDetailsComponent,
  CheckoutComponent,
  OrderConfirmationComponent,
];

/**
 * 標準の CMS コンポーネント(Spartacus の各機能ライブラリに当たる)。
 * キーは CMS の typeCode、CMSFlexComponent は flexType。自社の部品に差し替えるときは、同じキーで provideConfig する。
 */
export const defaultCmsComponentsConfig: CmsConfig = {
  cmsComponents: {
    SiteLogoComponent: { component: SiteLogoComponent },
    SearchBox: { component: SearchBoxComponent },
    MiniCartComponent: { component: MiniCartComponent },
    NavigationComponent: { component: CategoryNavigationComponent },
    FooterComponent: { component: FooterNavigationComponent },
    CMSParagraphComponent: { component: ParagraphComponent },
    CMSLinkComponent: { component: LinkComponent },
    SimpleBannerComponent: { component: BannerComponent },
    ProductCarouselComponent: { component: ProductCarouselComponent },
    ProductListComponent: { component: ProductListComponent },
    SearchResultsListComponent: { component: ProductListComponent },
    ProductFacetNavigationComponent: { component: ProductFacetNavigationComponent },
    ProductDetailsComponent: { component: ProductDetailsComponent },
    CartComponent: { component: CartDetailsComponent },
    CheckoutComponent: { component: CheckoutComponent },
    OrderConfirmationComponent: { component: OrderConfirmationComponent },
  },
};

@NgModule({
  imports: [CommonModule, RouterModule, ReactiveFormsModule, StorefrontComponentsModule],
  declarations: COMPONENTS,
  exports: [MediaComponent],
  providers: [provideDefaultConfig(defaultCmsComponentsConfig)],
})
export class CmsComponentsModule {}
