import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, PLATFORM_ID } from '@angular/core';

/**
 * 「ページの先頭へ」のボタン(Outlet Footer / AFTER)。
 * 画面に固定すると他のボタンに重なるので、フッターの中に置く。window はブラウザでだけ触る。
 */
@Component({
  selector: 'xyz-back-to-top',
  standalone: false,
  templateUrl: './back-to-top.component.html',
  styleUrl: './back-to-top.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BackToTopComponent {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly document = inject(DOCUMENT);

  scrollToTop(): void {
    if (!this.isBrowser) {
      return;
    }
    this.document.defaultView?.scrollTo({ top: 0, behavior: 'smooth' });
    // 読み上げの位置も先頭に戻す
    this.document.querySelector<HTMLElement>('header a, header button')?.focus({ preventScroll: true });
  }
}
