import { inject, Injectable, signal } from '@angular/core';
import { OccConfig } from '../occ/occ-config';
import { I18nConfig } from './i18n-config';

/**
 * 'chunk.key.sub' の形のキーを、今の言語の文言にする。{{name}} は params で埋める。
 * 見つからないときは fallbackLang、それも無ければキーをそのまま返す(画面で抜けが見えるように)。
 */
@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly config = inject(I18nConfig);
  readonly language = signal(inject(OccConfig).context?.language?.[0] ?? 'ja');

  translate(key: string, params: Record<string, unknown> = {}): string {
    const fallback = this.config.i18n?.fallbackLang ?? 'en';
    const text = this.lookup(this.language(), key) ?? this.lookup(fallback, key);
    if (typeof text !== 'string') {
      return `[${key}]`;
    }
    return text.replace(/\{\{\s*(\w+)\s*\}\}/g, (_, name: string) => String(params[name] ?? ''));
  }

  private lookup(lang: string, key: string): unknown {
    const [chunk, ...path] = key.split('.');
    let node: unknown = this.config.i18n?.resources?.[lang]?.[chunk];
    for (const p of path) {
      node = (node as Record<string, unknown> | undefined)?.[p];
    }
    return node;
  }
}
