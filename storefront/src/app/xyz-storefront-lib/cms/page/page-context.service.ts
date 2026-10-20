import { inject, Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map, Observable, shareReplay, startWith } from 'rxjs';
import { PageContext } from '../../model/cms.model';

/**
 * URL から、どの CMS ページを引くかを決める。Spartacus の RoutingService.getPageContext に当たる。
 * /product/:code → ProductPage、/category/:code → CategoryPage、それ以外は ContentPage(ラベル = パス)。
 */
@Injectable({ providedIn: 'root' })
export class PageContextService {
  private readonly router = inject(Router);

  readonly context$: Observable<PageContext> = this.router.events.pipe(
    filter((e): e is NavigationEnd => e instanceof NavigationEnd),
    map((e) => e.urlAfterRedirects),
    startWith(this.router.url),
    map((url) => PageContextService.fromUrl(url)),
    shareReplay(1),
  );

  static fromUrl(url: string): PageContext {
    const path = url.split(/[?#]/)[0];
    const product = path.match(/^\/product\/([^/]+)/);
    if (product) {
      return { type: 'ProductPage', id: decodeURIComponent(product[1]) };
    }
    const category = path.match(/^\/category\/([^/]+)/);
    if (category) {
      return { type: 'CategoryPage', id: decodeURIComponent(category[1]) };
    }
    if (path.startsWith('/search')) {
      return { type: 'ContentPage', id: 'search' };
    }
    return { type: 'ContentPage', id: path === '/' || path === '' ? 'homepage' : path };
  }
}
