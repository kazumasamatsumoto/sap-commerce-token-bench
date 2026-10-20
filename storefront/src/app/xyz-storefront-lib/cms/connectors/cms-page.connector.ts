import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { CmsPage, PageContext } from '../../model/cms.model';
import { CmsPageAdapter } from './cms-page.adapter';

@Injectable({ providedIn: 'root' })
export class CmsPageConnector {
  private readonly adapter = inject(CmsPageAdapter);

  get(context: PageContext): Observable<CmsPage> {
    return this.adapter.load(context);
  }
}
