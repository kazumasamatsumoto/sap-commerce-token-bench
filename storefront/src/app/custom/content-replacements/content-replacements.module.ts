import { NgModule } from '@angular/core';
import { CmsConfig, provideConfig } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { XyzBannerComponent } from './xyz-banner/xyz-banner.component';
import { XyzFooterComponent } from './xyz-footer/xyz-footer.component';
import { XyzParagraphComponent } from './xyz-paragraph/xyz-paragraph.component';

/**
 * 標準の CMS 部品の差し替え(cmsComponents)。キーは標準と同じ typeCode / flexType。
 * CMS のデータは変えずに、そのまま読めるようにしてある。
 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [XyzBannerComponent, XyzParagraphComponent, XyzFooterComponent],
  providers: [
    provideConfig(<CmsConfig>{
      cmsComponents: {
        SimpleBannerComponent: { component: XyzBannerComponent },
        CMSParagraphComponent: { component: XyzParagraphComponent },
        FooterComponent: { component: XyzFooterComponent },
      },
    }),
  ],
})
export class ContentReplacementsModule {}
