import { inject, Injectable } from '@angular/core';
import { OccConfig } from './occ-config';

export interface BuildUrlOptions {
  urlParams?: Record<string, string | number>;
  queryParams?: Record<string, string | number | undefined>;
}

/**
 * Spartacus の OccEndpointsService と同じ役目。端点の名前から
 * baseUrl + prefix + baseSite + 端点 の URL を組み立てる。
 */
@Injectable({ providedIn: 'root' })
export class OccEndpointsService {
  private readonly config = inject(OccConfig);

  getBaseUrl(): string {
    const occ = this.config.backend?.occ ?? {};
    const baseSite = this.config.context?.baseSite?.[0] ?? '';
    return `${occ.baseUrl ?? ''}${occ.prefix ?? '/occ/v2/'}${baseSite}`;
  }

  buildUrl(endpoint: string, options: BuildUrlOptions = {}): string {
    let template = this.config.backend?.occ?.endpoints?.[endpoint];
    if (!template) {
      throw new Error(`OCC の端点 "${endpoint}" が設定にありません`);
    }
    for (const [key, value] of Object.entries(options.urlParams ?? {})) {
      template = template.replace('${' + key + '}', encodeURIComponent(String(value)));
    }
    const url = new URL(`${this.getBaseUrl()}/${template}`);
    for (const [key, value] of Object.entries(options.queryParams ?? {})) {
      if (value !== undefined) {
        url.searchParams.set(key, String(value));
      }
    }
    return url.toString();
  }
}
