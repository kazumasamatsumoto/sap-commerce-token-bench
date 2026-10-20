import { NgModule } from '@angular/core';
import { OutletPosition, provideOutlet } from '@xyz/storefront';
import { CustomSharedModule } from '../shared';
import { RecentlyViewedListComponent } from './recently-viewed-list/recently-viewed-list.component';
import { RecentlyViewedTrackerComponent } from './recently-viewed-tracker/recently-viewed-tracker.component';

/** 最近見た商品(ブラウザだけの機能。localStorage に覚える)。 */
@NgModule({
  imports: [CustomSharedModule],
  declarations: [RecentlyViewedTrackerComponent, RecentlyViewedListComponent],
  providers: [
    provideOutlet({ id: 'Summary', position: OutletPosition.AFTER, component: RecentlyViewedTrackerComponent }),
    provideOutlet({ id: 'CrossSelling', position: OutletPosition.AFTER, component: RecentlyViewedListComponent }),
    provideOutlet({ id: 'Section5', position: OutletPosition.AFTER, component: RecentlyViewedListComponent }),
  ],
})
export class RecentlyViewedModule {}
