import { ChangeDetectionStrategy, Component } from '@angular/core';

export interface FooterColumn {
  /** 翻訳のキー(xyzFooter.columns.<key>) */
  key: string;
  links: { key: string; url: string }[];
}

/** フッターのリンクの列。文言は翻訳(xyzFooter.links.<key>)で引く。 */
export const FOOTER_COLUMNS: FooterColumn[] = [
  {
    key: 'guide',
    links: [
      { key: 'howToOrder', url: '/guide' },
      { key: 'shipping', url: '/shipping' },
      { key: 'returns', url: '/returns' },
    ],
  },
  {
    key: 'support',
    links: [
      { key: 'contact', url: '/contact' },
      { key: 'faq', url: '/faq' },
    ],
  },
  {
    key: 'company',
    links: [
      { key: 'about', url: '/company' },
      { key: 'careers', url: '/careers' },
    ],
  },
];

/**
 * FooterComponent(CMSFlexComponent)の差し替え。標準は著作権の 1 行だけなので、リンクの列を足す。
 * 同じスロットの CMSLinkComponent(利用規約など)は、これまでどおり後ろに並ぶ。
 */
@Component({
  selector: 'xyz-footer',
  standalone: false,
  templateUrl: './xyz-footer.component.html',
  styleUrl: './xyz-footer.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class XyzFooterComponent {
  readonly columns = FOOTER_COLUMNS;
}
