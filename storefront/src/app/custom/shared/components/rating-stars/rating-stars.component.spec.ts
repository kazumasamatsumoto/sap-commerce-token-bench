import { TestBed } from '@angular/core/testing';
import { provideCustomTesting } from '../../../testing/custom-testing';
import { CustomSharedModule } from '../../custom-shared.module';
import { RatingStarsComponent, toStars } from './rating-stars.component';

describe('toStars', () => {
  it('0.5 きざみに丸めて、満・半・空 の星にする', () => {
    expect(toStars(3.7)).toEqual(['full', 'full', 'full', 'half', 'empty']);
    expect(toStars(4.8)).toEqual(['full', 'full', 'full', 'full', 'full']);
    expect(toStars(undefined)).toEqual(['empty', 'empty', 'empty', 'empty', 'empty']);
  });
});

describe('RatingStarsComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CustomSharedModule], providers: provideCustomTesting() });
  });

  it('星を 5 つ出し、読み上げ用の文言を付ける', async () => {
    const fixture = TestBed.createComponent(RatingStarsComponent);
    fixture.componentRef.setInput('rating', 4);
    fixture.detectChanges();
    await fixture.whenStable();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('.star').length).toBe(5);
    expect(el.querySelectorAll('.star[data-state="full"]').length).toBe(4);
    expect(el.querySelector('[role="img"]')?.getAttribute('aria-label')).toBe('5 点中 4.0 点');
  });
});
