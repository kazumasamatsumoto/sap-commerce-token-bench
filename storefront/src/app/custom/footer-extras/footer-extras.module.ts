import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { BackToTopComponent } from './back-to-top/back-to-top.component';
import { CookieConsentComponent } from './cookie-consent/cookie-consent.component';
import { PaymentMethodsComponent } from './payment-methods/payment-methods.component';
import { TrustBadgesComponent } from './trust-badges/trust-badges.component';

/** フッターに足すもの(お店の約束、お支払い方法、先頭へ戻る、Cookie の同意)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [TrustBadgesComponent, PaymentMethodsComponent, BackToTopComponent, CookieConsentComponent],
  providers: [
    provideOutlet({ id: 'Footer', position: OutletPosition.BEFORE, component: TrustBadgesComponent }),
    provideOutlet({ id: 'Footer', position: OutletPosition.AFTER, component: PaymentMethodsComponent }),
    provideOutlet({ id: 'Footer', position: OutletPosition.AFTER, component: BackToTopComponent }),
    // 'footer' はフッターの枠(cx-page-layout section="footer")全体の Outlet
    provideOutlet({ id: 'footer', position: OutletPosition.AFTER, component: CookieConsentComponent }),
  ],
})
export class FooterExtrasModule {}
