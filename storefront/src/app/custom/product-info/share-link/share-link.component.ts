import { DOCUMENT, isPlatformBrowser } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, PLATFORM_ID, signal } from '@angular/core';

/**
 * 商品の URL をコピーするボタン(PDP.ADDTOCART / AFTER)。
 * クリップボードはブラウザにしか無いので、SSR では何もしない。
 */
@Component({
  selector: 'xyz-share-link',
  standalone: false,
  templateUrl: './share-link.component.html',
  styleUrl: './share-link.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShareLinkComponent {
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));
  private readonly document = inject(DOCUMENT);
  readonly status = signal<'idle' | 'copied' | 'failed'>('idle');

  async copy(): Promise<void> {
    const clipboard = this.isBrowser ? this.document.defaultView?.navigator.clipboard : undefined;
    if (!clipboard) {
      this.status.set('failed');
      return;
    }
    try {
      await clipboard.writeText(this.document.location.href);
      this.status.set('copied');
    } catch {
      this.status.set('failed');
    }
  }
}
