import { inject, Injectable } from '@angular/core';
import { catchError, map, Observable, of, shareReplay } from 'rxjs';
import { CmsComponent, CmsPage, ContentSlot, PageContext } from '../../model/cms.model';
import { CmsPageConnector } from '../connectors/cms-page.connector';

/**
 * ページとスロットの窓口。取得に失敗したときは空のページを返し、画面全体は止めない
 * (SSR でバックエンドが落ちていても HTML は返すため)。
 */
@Injectable({ providedIn: 'root' })
export class CmsService {
  private readonly connector = inject(CmsPageConnector);
  private readonly cache = new Map<string, Observable<CmsPage>>();

  getPage(context: PageContext): Observable<CmsPage> {
    const key = `${context.type}:${context.id}`;
    if (!this.cache.has(key)) {
      this.cache.set(
        key,
        this.connector.get(context).pipe(
          catchError(() => of<CmsPage>({ uid: 'not-found', template: 'ContentPage1Template', contentSlots: { contentSlot: [] } })),
          shareReplay(1),
        ),
      );
    }
    return this.cache.get(key)!;
  }

  getSlot(context: PageContext, position: string): Observable<ContentSlot | undefined> {
    return this.getPage(context).pipe(map((page) => page.contentSlots?.contentSlot?.find((s) => s.position === position)));
  }

  getComponents(context: PageContext, position: string): Observable<CmsComponent[]> {
    return this.getSlot(context, position).pipe(map((slot) => slot?.components?.component ?? []));
  }
}
