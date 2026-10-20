import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { filter } from 'rxjs';
import { BrowserStorageService, CurrentProductService } from '../../shared';
import { RecentlyViewedService } from '../recently-viewed.service';

/**
 * 商品詳細を開いたら、その商品を「最近見た商品」に覚える(Outlet Summary / AFTER)。画面には何も出さない。
 * 覚えるのはブラウザだけ(SSR では何もしない)。
 */
@Component({
  selector: 'xyz-recently-viewed-tracker',
  standalone: false,
  templateUrl: './recently-viewed-tracker.component.html',
  styleUrl: './recently-viewed-tracker.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecentlyViewedTrackerComponent {
  constructor() {
    if (!inject(BrowserStorageService).isBrowser) {
      return;
    }
    const recentlyViewed = inject(RecentlyViewedService);
    inject(CurrentProductService)
      .getCode()
      .pipe(
        filter((code): code is string => !!code),
        takeUntilDestroyed(),
      )
      .subscribe((code) => recentlyViewed.add(code));
  }
}
