import { Injectable } from '@angular/core';
import { Config } from '../config/config';

/** { ja: { product: { addToCart: 'カートに入れる' } } } の形。chunk(product など)ごとにファイルを分ける。 */
export interface TranslationResources {
  [lang: string]: { [chunk: string]: Record<string, unknown> };
}

@Injectable({ providedIn: 'root', useExisting: Config })
export abstract class I18nConfig {
  i18n?: {
    resources?: TranslationResources;
    fallbackLang?: string;
  };
}
