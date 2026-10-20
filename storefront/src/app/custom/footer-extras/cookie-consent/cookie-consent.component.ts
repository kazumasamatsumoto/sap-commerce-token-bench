import { isPlatformBrowser } from '@angular/common';
import { afterNextRender, ChangeDetectionStrategy, Component, inject, PLATFORM_ID, signal } from '@angular/core';

export const COOKIE_CONSENT_KEY = 'xyz-cookie-consent';
export type CookieConsent = 'all' | 'necessary';

/**
 * Cookie の同意の帯(Outlet footer / AFTER)。選んだ結果は localStorage に覚える。
 * SSR では出さない(サーバーは同意したかを知らないため)。ブラウザで描画したあとに読んで、未回答なら出す。
 * 画面に固定すると他のボタンに重なるので、フッターの一番下に置く。
 */
@Component({
  selector: 'xyz-cookie-consent',
  standalone: false,
  templateUrl: './cookie-consent.component.html',
  styleUrl: './cookie-consent.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CookieConsentComponent {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  readonly visible = signal(false);

  constructor() {
    afterNextRender(() => this.visible.set(this.read() === null));
  }

  choose(consent: CookieConsent): void {
    if (this.isBrowser) {
      try {
        localStorage.setItem(COOKIE_CONSENT_KEY, consent);
      } catch {
        // 覚えられなくても帯は閉じる
      }
    }
    this.visible.set(false);
  }

  private read(): string | null {
    try {
      return localStorage.getItem(COOKIE_CONSENT_KEY);
    } catch {
      return null;
    }
  }
}
