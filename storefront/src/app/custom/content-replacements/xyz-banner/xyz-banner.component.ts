import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CmsComponent, CmsComponentData } from '@xyz/storefront';

export interface XyzBannerData extends CmsComponent {
  headline?: string;
  urlLink?: string;
  media?: { url?: string; altText?: string };
}

/** http(s):// で始まる URL は外のサイト。routerLink ではなく href で開く。 */
export function isExternalUrl(url: string | undefined): boolean {
  return !!url && /^https?:\/\//.test(url);
}

/**
 * SimpleBannerComponent の差し替え。標準と同じデータ(headline / urlLink / media)のまま、
 * 見出しを画像に重ね、「詳しく見る」のボタン風の表示を足す。
 */
@Component({
  selector: 'xyz-banner',
  standalone: false,
  templateUrl: './xyz-banner.component.html',
  styleUrl: './xyz-banner.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class XyzBannerComponent {
  readonly data$ = inject(CmsComponentData<XyzBannerData>).data$;
  readonly isExternal = isExternalUrl;
}
