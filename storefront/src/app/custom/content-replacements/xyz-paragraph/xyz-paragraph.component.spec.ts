import { TestBed } from '@angular/core/testing';
import { CmsComponentData } from '@xyz/storefront';
import { of } from 'rxjs';
import { provideCustomTesting } from '../../testing/custom-testing';
import { ContentReplacementsModule } from '../content-replacements.module';
import { isNoticeParagraph, XyzParagraphComponent, XyzParagraphData } from './xyz-paragraph.component';

describe('isNoticeParagraph', () => {
  it('uid に Notice を含むか、本文に「お知らせ」があればお知らせ', () => {
    expect(isNoticeParagraph({ uid: 'HomeNoticeParagraph', content: '年末年始' })).toBe(true);
    expect(isNoticeParagraph({ uid: 'X', content: '配送についてのお知らせ' })).toBe(true);
    expect(isNoticeParagraph({ uid: 'HomeAboutParagraph', content: 'XYZ電機は…' })).toBe(false);
  });
});

describe('XyzParagraphComponent', () => {
  function render(data: XyzParagraphData): HTMLElement {
    TestBed.configureTestingModule({
      imports: [ContentReplacementsModule],
      providers: [provideCustomTesting(), { provide: CmsComponentData, useValue: { uid: data.uid, data$: of(data) } }],
    });
    const fixture = TestBed.createComponent(XyzParagraphComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('標準と同じく content を HTML として出し、cx-paragraph の class を残す', () => {
    const el = render({ uid: 'About', content: '<b>XYZ</b>電機' });
    const p = el.querySelector('p.cx-paragraph')!;
    expect(p.querySelector('b')?.textContent).toBe('XYZ');
    expect(p.getAttribute('role')).toBeNull();
  });

  it('お知らせの段落には印と role="note" を付ける', () => {
    const el = render({ uid: 'HomeNoticeParagraph', content: '年末年始の配送についてのお知らせ' });
    const p = el.querySelector('p')!;
    expect(p.classList).toContain('xyz-paragraph--notice');
    expect(p.getAttribute('role')).toBe('note');
  });
});
