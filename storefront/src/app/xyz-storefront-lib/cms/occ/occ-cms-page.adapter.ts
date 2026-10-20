import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CmsPage, PageContext } from '../../model/cms.model';
import { OccEndpointsService } from '../../occ/occ-endpoints.service';
import { CmsPageAdapter } from '../connectors/cms-page.adapter';

/** OCC の /cms/pages を呼ぶ。ContentPage は pageLabelOrId、商品やカテゴリのページは code で引く。 */
@Injectable()
export class OccCmsPageAdapter implements CmsPageAdapter {
  private readonly http = inject(HttpClient);
  private readonly endpoints = inject(OccEndpointsService);

  load(context: PageContext): Observable<CmsPage> {
    const queryParams =
      context.type === 'ContentPage'
        ? { pageType: context.type, pageLabelOrId: context.id }
        : { pageType: context.type, code: context.id };
    return this.http.get<CmsPage>(this.endpoints.buildUrl('pages', { queryParams }));
  }
}
