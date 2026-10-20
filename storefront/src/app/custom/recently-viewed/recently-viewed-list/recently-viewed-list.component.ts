import { afterNextRender, ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ProductService } from '@xyz/storefront';
import { combineLatest, map, of, switchMap } from 'rxjs';
import { CurrentProductService, XyzProduct } from '../../shared';
import { RecentlyViewedService } from '../recently-viewed.service';

/** 一覧に出す件数。 */
export const RECENTLY_VIEWED_SHOWN = 6;

/**
 * 最近見た商品の一覧(トップの Section5 / AFTER と、商品詳細の CrossSelling / AFTER)。
 * SSR とブラウザの最初の描画をそろえるため、履歴を出すのは描画のあと(afterNextRender)から。
 */
@Component({
  selector: 'xyz-recently-viewed-list',
  standalone: false,
  templateUrl: './recently-viewed-list.component.html',
  styleUrl: './recently-viewed-list.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RecentlyViewedListComponent {
  private readonly productService = inject(ProductService);
  private readonly recentlyViewed = inject(RecentlyViewedService);
  private readonly ready = signal(false);
  private readonly codes = this.recentlyViewed.getCodes();
  private readonly currentCode = toSignal(inject(CurrentProductService).getCode());

  /** 今見ている商品は除く。 */
  readonly shownCodes = computed(() =>
    this.ready()
      ? this.codes()
          .filter((c) => c !== this.currentCode())
          .slice(0, RECENTLY_VIEWED_SHOWN)
      : [],
  );

  readonly products$ = toObservable(this.shownCodes).pipe(
    switchMap((codes) => (codes.length ? combineLatest(codes.map((c) => this.productService.get(c))) : of([]))),
    map((products) => products.filter((p): p is XyzProduct => !!p)),
  );

  constructor() {
    afterNextRender(() => this.ready.set(true));
  }

  clear(): void {
    this.recentlyViewed.clear();
  }
}
