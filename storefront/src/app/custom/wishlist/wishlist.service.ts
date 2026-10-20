import { inject, Injectable, Signal, signal } from '@angular/core';
import { BrowserStorageService } from '../shared';

export const WISHLIST_KEY = 'xyz-wishlist';
export const WISHLIST_MAX = 50;

/** 入っていれば外し、無ければ先頭に足す。 */
export function toggleCode(list: string[], code: string, max = WISHLIST_MAX): string[] {
  return list.includes(code) ? list.filter((c) => c !== code) : [code, ...list].slice(0, max);
}

/**
 * お気に入り(ログイン無しで使う簡易版)。商品コードをブラウザの localStorage に覚える。
 * SSR では常に空。
 */
@Injectable({ providedIn: 'root' })
export class WishlistService {
  private readonly storage = inject(BrowserStorageService);
  private readonly codes = signal<string[]>([]);
  private loaded = false;

  /** 部品のフィールドの初期化で呼ぶ(テンプレートや computed の中では呼ばない)。 */
  getCodes(): Signal<string[]> {
    this.load();
    return this.codes.asReadonly();
  }

  toggle(code: string): void {
    this.save(toggleCode(this.current(), code));
  }

  remove(code: string): void {
    this.save(this.current().filter((c) => c !== code));
  }

  private current(): string[] {
    this.load();
    return this.codes();
  }

  private save(next: string[]): void {
    this.codes.set(next);
    this.storage.writeJson(WISHLIST_KEY, next);
  }

  private load(): void {
    if (this.loaded || !this.storage.isBrowser) {
      return;
    }
    this.loaded = true;
    const saved = this.storage.readJson<unknown>(WISHLIST_KEY, []);
    this.codes.set(Array.isArray(saved) ? saved.filter((c): c is string => typeof c === 'string').slice(0, WISHLIST_MAX) : []);
  }
}
