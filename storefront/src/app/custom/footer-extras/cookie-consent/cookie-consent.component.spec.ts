import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideCustomTesting } from '../../testing/custom-testing';
import { FooterExtrasModule } from '../footer-extras.module';
import { COOKIE_CONSENT_KEY, CookieConsentComponent } from './cookie-consent.component';

describe('CookieConsentComponent', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [FooterExtrasModule],
      providers: [provideCustomTesting(), { provide: PLATFORM_ID, useValue: 'browser' }],
    });
  });

  async function render() {
    const fixture = TestBed.createComponent(CookieConsentComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('まだ答えていなければ、描画のあとに帯を出す', async () => {
    const fixture = await render();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="xyz-cookie-consent"]')).not.toBeNull();
  });

  it('選ぶと覚えて、帯を閉じる', async () => {
    const fixture = await render();
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll('button');
    buttons[1].click();
    fixture.detectChanges();
    expect(localStorage.getItem(COOKIE_CONSENT_KEY)).toBe('necessary');
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="xyz-cookie-consent"]')).toBeNull();
  });

  it('答え済みなら出さない', async () => {
    localStorage.setItem(COOKIE_CONSENT_KEY, 'all');
    const fixture = await render();
    expect((fixture.nativeElement as HTMLElement).querySelector('[data-testid="xyz-cookie-consent"]')).toBeNull();
  });
});
