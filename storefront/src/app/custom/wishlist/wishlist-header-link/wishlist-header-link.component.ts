import { afterNextRender, ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { WishlistService } from '../wishlist.service';

/**
 * ヘッダーのお気に入りのボタン(Outlet MiniCart / AFTER)。件数を出し、押すと一覧のパネルを開く。
 * 件数はブラウザで描画したあとに出す(SSR では 0)。
 */
@Component({
  selector: 'xyz-wishlist-header-link',
  standalone: false,
  templateUrl: './wishlist-header-link.component.html',
  styleUrl: './wishlist-header-link.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WishlistHeaderLinkComponent {
  private readonly ready = signal(false);
  readonly codes = inject(WishlistService).getCodes();
  readonly count = computed(() => (this.ready() ? this.codes().length : 0));
  readonly open = signal(false);

  constructor() {
    afterNextRender(() => this.ready.set(true));
  }

  toggle(): void {
    this.open.update((v) => !v);
  }
}
