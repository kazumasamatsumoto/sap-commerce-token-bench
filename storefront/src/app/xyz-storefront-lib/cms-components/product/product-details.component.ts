import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { filter, map, Observable, switchMap } from 'rxjs';
import { ActiveCartService } from '../../cart/facade/active-cart.service';
import { PageContextService } from '../../cms/page/page-context.service';
import { Product } from '../../model/product.model';
import { ProductService } from '../../product/facade/product.service';

/** ProductDetailsComponent(CMSFlexComponent)。商品の名前、価格、在庫、画像、説明と、カートに入れるボタン。 */
@Component({
  selector: 'cx-product-details',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (product$ | async; as product) {
      <article class="cx-product-details">
        <cx-media [src]="product.images?.[0]?.url" [alt]="product.name" [width]="400"></cx-media>
        <div class="cx-product-summary">
          <h1 data-testid="product-name">{{ product.name }}</h1>
          <p class="code">{{ product.code }}</p>
          <ng-template [cxOutlet]="'PDP.PRICE'" [cxOutletContext]="product">
            <p class="price" data-testid="product-price">{{ product.price?.formattedValue }}</p>
          </ng-template>
          <p class="stock" data-testid="product-stock">
            @switch (product.stock?.stockLevelStatus) {
              @case ('outOfStock') {
                {{ 'product.outOfStock' | cxTranslate }}
              }
              @case ('lowStock') {
                {{ 'product.lowStock' | cxTranslate: { count: product.stock?.stockLevel ?? 0 } }}
              }
              @default {
                {{ 'product.inStock' | cxTranslate }}
              }
            }
          </p>
          <p class="summary">{{ product.summary }}</p>
          <ng-template [cxOutlet]="'PDP.ADDTOCART'" [cxOutletContext]="product">
            @if (product.purchasable && product.stock?.stockLevelStatus !== 'outOfStock') {
              <!-- form にしない: SSR の HTML のまま押されたとき、ブラウザの普通の送信でページが読み直されるため -->
              <div class="cx-add-to-cart">
                <label>
                  {{ 'product.quantity' | cxTranslate }}
                  <input #qty type="number" name="quantity" min="1" max="99" value="1" />
                </label>
                <button type="button" data-testid="add-to-cart" (click)="addToCart(product, qty.value)">{{ 'product.addToCart' | cxTranslate }}</button>
              </div>
              @if (added()) {
                <p role="status" data-testid="added-to-cart">{{ 'product.addedToCart' | cxTranslate }}</p>
              }
            }
          </ng-template>
        </div>
        <section class="cx-product-description">
          <h2>{{ 'product.description' | cxTranslate }}</h2>
          <p>{{ product.description }}</p>
        </section>
      </article>
    }
  `,
})
export class ProductDetailsComponent {
  private readonly productService = inject(ProductService);
  private readonly activeCart = inject(ActiveCartService);
  readonly added = signal(false);

  readonly product$: Observable<Product> = inject(PageContextService).context$.pipe(
    filter((ctx) => ctx.type === 'ProductPage'),
    map((ctx) => ctx.id),
    switchMap((code) => this.productService.get(code)),
    filter((p): p is Product => !!p),
  );

  addToCart(product: Product, quantity: string): void {
    this.activeCart.addEntry(product.code!, Math.max(1, Number(quantity) || 1));
    this.added.set(true);
  }
}
