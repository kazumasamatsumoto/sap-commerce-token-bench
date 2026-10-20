import { inject, Injectable, Signal, signal } from '@angular/core';
import { BrowserStorageService } from '../shared';

export const RECENTLY_VIEWED_KEY = 'xyz-recently-viewed';
export const RECENTLY_VIEWED_MAX = 8;

/** 先頭に足して、同じ商品は 1 つにまとめ、max 件で切る。 */
export function pushRecent(list: string[], code: string, max = RECENTLY_VIEWED_MAX): string[] {
  return [code, ...list.filter((c) => c !== code)].slice(0, max);
}

/**
 * 最近見た商品の商品コード(新しい順)。ブラウザの localStorage に覚える。
 * SSR では常に空(サーバーは利用者の履歴を知らない)。
 */
@Injectable({ providedIn: 'root' })
export class RecentlyViewedService {
  private readonly storage = inject(BrowserStorageService);
  private readonly codes = signal<string[]>([]);
  private loaded = false;

  /** 部品のフィールドの初期化で呼ぶ(テンプレートや computed の中では呼ばない)。 */
  getCodes(): Signal<string[]> {
    this.load();
    return this.codes.asReadonly();
  }

  add(code: string): void {
    this.load();
    if (!this.storage.isBrowser) {
      return;
    }
    const next = pushRecent(this.codes(), code);
    this.codes.set(next);
    this.storage.writeJson(RECENTLY_VIEWED_KEY, next);
  }

  clear(): void {
    this.codes.set([]);
    this.storage.writeJson(RECENTLY_VIEWED_KEY, []);
  }

  private load(): void {
    if (this.loaded || !this.storage.isBrowser) {
      return;
    }
    this.loaded = true;
    const saved = this.storage.readJson<unknown>(RECENTLY_VIEWED_KEY, []);
    this.codes.set(Array.isArray(saved) ? saved.filter((c): c is string => typeof c === 'string').slice(0, RECENTLY_VIEWED_MAX) : []);
  }
}
