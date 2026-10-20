import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import { ActiveCartService, ProductService } from '@xyz/storefront';
import { combineLatest, map, of, switchMap } from 'rxjs';
import { XyzProduct } from '../../shared';
import { WishlistService } from '../wishlist.service';

/** お気に入りの一覧のパネル。数量を選んでカートに入れる、一覧から外す、ができる。 */
@Component({
  selector: 'xyz-wishlist-panel',
  standalone: false,
  templateUrl: './wishlist-panel.component.html',
  styleUrl: './wishlist-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WishlistPanelComponent {
  private readonly productService = inject(ProductService);
  private readonly activeCart = inject(ActiveCartService);
  private readonly wishlist = inject(WishlistService);

  readonly codes = input.required<string[]>();
  readonly closed = output<void>();

  /** 商品コードごとの数量(既定は 1)。 */
  private readonly quantities = signal<Record<string, number>>({});
  readonly lastAdded = signal<string | undefined>(undefined);

  readonly products$ = toObservable(this.codes).pipe(
    switchMap((codes) => (codes.length ? combineLatest(codes.map((c) => this.productService.get(c))) : of([]))),
    map((products) => products.filter((p): p is XyzProduct => !!p)),
  );

  quantity(code: string | undefined): number {
    return this.quantities()[code ?? ''] ?? 1;
  }

  setQuantity(code: string | undefined, value: number): void {
    this.quantities.update((q) => ({ ...q, [code ?? '']: value }));
  }

  addToCart(product: XyzProduct): void {
    if (!product.code) {
      return;
    }
    this.activeCart.addEntry(product.code, this.quantity(product.code));
    this.lastAdded.set(product.name ?? product.code);
  }

  remove(code: string | undefined): void {
    if (code) {
      this.wishlist.remove(code);
    }
  }
}
