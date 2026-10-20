import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { LanguageSwitcherComponent } from './language-switcher/language-switcher.component';
import { NoticeBarComponent } from './notice-bar/notice-bar.component';

/** ヘッダーに足すもの(お知らせの帯、言語の切り替え)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [NoticeBarComponent, LanguageSwitcherComponent],
  providers: [
    provideOutlet({ id: 'PreHeader', position: OutletPosition.BEFORE, component: NoticeBarComponent }),
    provideOutlet({ id: 'SiteContext', position: OutletPosition.AFTER, component: LanguageSwitcherComponent }),
  ],
})
export class HeaderExtrasModule {}
