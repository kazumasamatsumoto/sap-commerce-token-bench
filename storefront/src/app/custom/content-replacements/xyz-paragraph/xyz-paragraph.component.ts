import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CmsComponent, CmsComponentData } from '@xyz/storefront';
import { map } from 'rxjs';

export interface XyzParagraphData extends CmsComponent {
  content?: string;
}

/**
 * お知らせとして目立たせる段落か。uid に Notice を含むか、本文が「お知らせ」を含むもの。
 * (CMS に項目を足さずに、今のデータのまま見分ける)
 */
export function isNoticeParagraph(data: XyzParagraphData): boolean {
  return /notice/i.test(data.uid ?? '') || (data.content ?? '').includes('お知らせ');
}

/**
 * CMSParagraphComponent の差し替え。標準と同じく content を HTML として出す。
 * お知らせの段落は、枠と印を付けて目立たせる(role="note")。
 */
@Component({
  selector: 'xyz-paragraph',
  standalone: false,
  templateUrl: './xyz-paragraph.component.html',
  styleUrl: './xyz-paragraph.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class XyzParagraphComponent {
  readonly view$ = inject(CmsComponentData<XyzParagraphData>).data$.pipe(
    map((data) => ({ content: data.content ?? '', notice: isNoticeParagraph(data) })),
  );
}
