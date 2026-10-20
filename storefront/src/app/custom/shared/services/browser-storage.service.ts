import { isPlatformBrowser } from '@angular/common';
import { inject, Injectable, PLATFORM_ID } from '@angular/core';

/**
 * localStorage の薄い包み。SSR(サーバー)では何もせず、読むと既定値を返す。
 * 保存先が使えないブラウザ(容量切れ、プライベートモードなど)でも落ちないようにする。
 */
@Injectable({ providedIn: 'root' })
export class BrowserStorageService {
  readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  readJson<T>(key: string, fallback: T): T {
    if (!this.isBrowser) {
      return fallback;
    }
    try {
      const raw = localStorage.getItem(key);
      return raw ? (JSON.parse(raw) as T) : fallback;
    } catch {
      return fallback;
    }
  }

  writeJson(key: string, value: unknown): void {
    if (!this.isBrowser) {
      return;
    }
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch {
      // 覚えられなくても画面は動かす
    }
  }
}
