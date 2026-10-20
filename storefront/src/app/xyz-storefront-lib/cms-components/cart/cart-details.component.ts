import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActiveCartService } from '../../cart/facade/active-cart.service';

/** CartComponent(CMSFlexComponent)。カートの明細(数量の変更と削除)、合計、購入手続きへのボタン。 */
@Component({
  selector: 'cx-cart-details',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (cart$ | async; as cart) {
      @if (cart.entries?.length) {
        <section class="cx-cart-details">
          <h1>{{ 'cart.title' | cxTranslate }} ({{ 'cart.items' | cxTranslate: { count: cart.totalUnitCount ?? 0 } }})</h1>
          <ul>
            @for (entry of cart.entries; track entry.entryNumber) {
              <li class="cx-cart-item" data-testid="cart-entry">
                <a [routerLink]="['/product', entry.product?.code]">{{ entry.product?.name }}</a>
                <span class="base-price">{{ entry.basePrice?.formattedValue }}</span>
                <label>
                  {{ 'product.quantity' | cxTranslate }}
                  <input
                    type="number"
                    min="0"
                    max="99"
                    [value]="entry.quantity"
                    (change)="update(entry.entryNumber!, $any($event.target).value)"
                    data-testid="cart-entry-quantity"
                  />
                </label>
                <span class="total">{{ entry.totalPrice?.formattedValue }}</span>
                <button type="button" (click)="update(entry.entryNumber!, 0)">{{ 'cart.remove' | cxTranslate }}</button>
              </li>
            }
          </ul>
          <dl class="cx-cart-totals">
            <dt>{{ 'cart.subtotal' | cxTranslate }}</dt>
            <dd>{{ cart.subTotal?.formattedValue }}</dd>
            <dt>{{ 'cart.tax' | cxTranslate }}</dt>
            <dd>{{ cart.totalTax?.formattedValue }}</dd>
            <dt>{{ 'cart.total' | cxTranslate }}</dt>
            <dd data-testid="cart-total">{{ cart.totalPrice?.formattedValue }}</dd>
          </dl>
          <a routerLink="/checkout" class="button" data-testid="proceed-to-checkout">{{ 'cart.checkout' | cxTranslate }}</a>
        </section>
      } @else {
        <p data-testid="cart-empty">{{ 'cart.empty' | cxTranslate }}</p>
      }
    } @else {
      <p data-testid="cart-empty">{{ 'cart.empty' | cxTranslate }}</p>
    }
  `,
})
export class CartDetailsComponent {
  private readonly activeCart = inject(ActiveCartService);
  readonly cart$ = this.activeCart.getActive();

  update(entryNumber: number, quantity: string | number): void {
    this.activeCart.updateEntry(entryNumber, Math.max(0, Number(quantity) || 0));
  }
}
