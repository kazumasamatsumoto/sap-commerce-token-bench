import { isPlatformBrowser } from '@angular/common';
import { afterNextRender, ChangeDetectionStrategy, Component, inject, PLATFORM_ID, signal } from '@angular/core';

export const NOTICE_DISMISSED_KEY = 'xyz-notice-dismissed';

/**
 * ヘッダーの一番上(Outlet PreHeader / BEFORE)のお知らせの帯。閉じたらこのタブの間は出さない。
 * sessionStorage はブラウザにしか無いので、読むのは描画のあと(afterNextRender)にする。
 */
@Component({
  selector: 'xyz-notice-bar',
  standalone: false,
  templateUrl: './notice-bar.component.html',
  styleUrl: './notice-bar.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NoticeBarComponent {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  readonly dismissed = signal(false);

  constructor() {
    afterNextRender(() => {
      try {
        this.dismissed.set(sessionStorage.getItem(NOTICE_DISMISSED_KEY) === '1');
      } catch {
        // 保存先が使えないブラウザでは、毎回出す
      }
    });
  }

  dismiss(): void {
    this.dismissed.set(true);
    if (this.isBrowser) {
      try {
        sessionStorage.setItem(NOTICE_DISMISSED_KEY, '1');
      } catch {
        // 覚えられなくても閉じるだけはする
      }
    }
  }
}
