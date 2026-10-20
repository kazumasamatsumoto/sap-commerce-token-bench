import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { WishlistButtonComponent } from './wishlist-button/wishlist-button.component';
import { WishlistHeaderLinkComponent } from './wishlist-header-link/wishlist-header-link.component';
import { WishlistPanelComponent } from './wishlist-panel/wishlist-panel.component';

/** お気に入り(ブラウザだけの機能。localStorage に覚える。ログインは使わない)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [WishlistButtonComponent, WishlistHeaderLinkComponent, WishlistPanelComponent],
  providers: [
    provideOutlet({ id: 'PDP.ADDTOCART', position: OutletPosition.AFTER, component: WishlistButtonComponent }),
    provideOutlet({ id: 'MiniCart', position: OutletPosition.AFTER, component: WishlistHeaderLinkComponent }),
  ],
})
export class WishlistModule {}
