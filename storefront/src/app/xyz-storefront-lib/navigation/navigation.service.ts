import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, shareReplay } from 'rxjs';
import { OccEndpointsService } from '../occ/occ-endpoints.service';

export interface NavigationNode {
  id: string;
  name: string;
  url: string;
  children: NavigationNode[];
}

interface CategoryHierarchy {
  id?: string;
  name?: string;
  subcategories?: CategoryHierarchy[];
}

/** カテゴリのメニュー。OCC の /catalogs のカテゴリの木から作る(1 回だけ読む)。 */
@Injectable({ providedIn: 'root' })
export class NavigationService {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  private readonly nodes$: Observable<NavigationNode[]> = this.http
    .get<{ catalogs?: { catalogVersions?: { categories?: CategoryHierarchy[] }[] }[] }>(this.endpoints.buildUrl('catalogs'))
    .pipe(
      map((r) => r.catalogs?.[0]?.catalogVersions?.[0]?.categories ?? []),
      map((categories) => categories.map((c) => NavigationService.toNode(c))),
      catchError(() => of([])),
      shareReplay(1),
    );

  getCategoryNodes(): Observable<NavigationNode[]> {
    return this.nodes$;
  }

  private static toNode(c: CategoryHierarchy): NavigationNode {
    return {
      id: c.id ?? '',
      name: c.name ?? c.id ?? '',
      url: `/category/${c.id}`,
      children: (c.subcategories ?? []).map((s) => NavigationService.toNode(s)),
    };
  }
}
