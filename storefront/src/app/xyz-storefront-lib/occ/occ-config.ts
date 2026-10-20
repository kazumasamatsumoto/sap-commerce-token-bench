import { Injectable } from '@angular/core';
import { Config } from '../config/config';

export interface OccEndpoints {
  [key: string]: string;
}

@Injectable({ providedIn: 'root', useExisting: Config })
export abstract class OccConfig {
  backend?: {
    occ?: {
      baseUrl?: string;
      prefix?: string;
      endpoints?: OccEndpoints;
    };
    /** 画像の URL の頭(OCC の /medias/... に付ける)。SSR でもブラウザから届く URL にする。 */
    media?: {
      baseUrl?: string;
    };
  };
  context?: {
    baseSite?: string[];
    language?: string[];
    currency?: string[];
  };
}
