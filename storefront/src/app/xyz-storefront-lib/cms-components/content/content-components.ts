import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { combineLatest, map, Observable, of, switchMap } from 'rxjs';
import { CmsComponentData } from '../../cms/model/cms-component-data';
import { CmsComponent } from '../../model/cms.model';
import { Product } from '../../model/product.model';
import { ProductService } from '../../product/facade/product.service';

interface ParagraphData extends CmsComponent {
  content?: string;
}

interface LinkData extends CmsComponent {
  linkName?: string;
  url?: string;
}

interface BannerData extends CmsComponent {
  headline?: string;
  urlLink?: string;
  media?: { url?: string; altText?: string };
}

interface CarouselData extends CmsComponent {
  title?: string;
  productCodes?: string;
}

/** CMSParagraphComponent。 */
@Component({
  selector: 'cx-paragraph',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (data$ | async; as data) {
    <p class="cx-paragraph" [innerHTML]="data.content"></p>
  }`,
})
export class ParagraphComponent {
  readonly data$ = inject(CmsComponentData<ParagraphData>).data$;
}

/** CMSLinkComponent。 */
@Component({
  selector: 'cx-link',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (data$ | async; as data) {
    <a class="cx-link" [routerLink]="data.url ?? '/'">{{ data.linkName ?? data.name }}</a>
  }`,
})
export class LinkComponent {
  readonly data$ = inject(CmsComponentData<LinkData>).data$;
}

/** SimpleBannerComponent。 */
@Component({
  selector: 'cx-banner',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (data$ | async; as data) {
    <a class="cx-banner" [routerLink]="data.urlLink ?? '/'">
      <cx-media [src]="data.media?.url" [alt]="data.media?.altText ?? data.headline" [width]="600"></cx-media>
      @if (data.headline) {
        <h2>{{ data.headline }}</h2>
      }
    </a>
  }`,
})
export class BannerComponent {
  readonly data$ = inject(CmsComponentData<BannerData>).data$;
}

/** ProductCarouselComponent。productCodes(空白区切り)の商品を並べる。 */
@Component({
  selector: 'cx-product-carousel',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (data$ | async; as data) {
    <section class="cx-product-carousel">
      @if (data.title) {
        <h2>{{ data.title }}</h2>
      }
      <ul>
        @for (product of products$ | async; track product.code) {
          <li>
            <a [routerLink]="['/product', product.code]">
              <cx-media [src]="product.images?.[0]?.url" [alt]="product.name" [width]="150"></cx-media>
              <span class="name">{{ product.name }}</span>
              <span class="price">{{ product.price?.formattedValue }}</span>
            </a>
          </li>
        }
      </ul>
    </section>
  }`,
})
export class ProductCarouselComponent {
  private readonly productService = inject(ProductService);
  readonly data$ = inject(CmsComponentData<CarouselData>).data$;

  readonly products$: Observable<Product[]> = this.data$.pipe(
    map((d) => (d.productCodes ?? '').split(/\s+/).filter(Boolean).slice(0, 8)),
    switchMap((codes: string[]) =>
      codes.length ? combineLatest(codes.map((c: string) => this.productService.get(c))) : of([] as (Product | undefined)[]),
    ),
    map((products: (Product | undefined)[]) => products.filter((p): p is Product => !!p)),
  );
}
