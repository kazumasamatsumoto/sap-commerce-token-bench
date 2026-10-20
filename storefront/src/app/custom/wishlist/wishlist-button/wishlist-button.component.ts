import { afterNextRender, ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { CurrentProductService } from '../../shared';
import { WishlistService } from '../wishlist.service';

/**
 * 商品詳細の「お気に入りに追加」(Outlet PDP.ADDTOCART / AFTER)。押すたびに入れる / 外すを切り替える。
 * SSR とブラウザの最初の描画は「未登録」でそろえ、描画のあとで本当の状態に直す。
 */
@Component({
  selector: 'xyz-wishlist-button',
  standalone: false,
  templateUrl: './wishlist-button.component.html',
  styleUrl: './wishlist-button.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WishlistButtonComponent {
  private readonly wishlist = inject(WishlistService);
  private readonly ready = signal(false);
  private readonly codes = this.wishlist.getCodes();
  readonly code = toSignal(inject(CurrentProductService).getCode());

  readonly saved = computed(() => {
    const code = this.code();
    return this.ready() && !!code && this.codes().includes(code);
  });

  constructor() {
    afterNextRender(() => this.ready.set(true));
  }

  toggle(): void {
    const code = this.code();
    if (code) {
      this.wishlist.toggle(code);
    }
  }
}
