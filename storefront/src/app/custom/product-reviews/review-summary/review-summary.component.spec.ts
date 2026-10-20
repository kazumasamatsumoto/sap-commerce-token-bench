import { TestBed } from '@angular/core/testing';
import { Review } from '../../shared';
import { provideCustomTesting } from '../../testing/custom-testing';
import { sortReviews } from '../product-reviews/product-reviews.component';
import { ProductReviewsModule } from '../product-reviews.module';
import { ReviewSummaryComponent, summarizeReviews } from './review-summary.component';

const reviews: Review[] = [
  { id: 'a', rating: 5, date: '2026-10-01T00:00:00+0000' },
  { id: 'b', rating: 1, date: '2026-10-03T00:00:00+0000' },
  { id: 'c', rating: 5, date: '2026-10-02T00:00:00+0000' },
  { id: 'd', rating: 3, date: '2026-09-30T00:00:00+0000' },
];

describe('summarizeReviews', () => {
  it('平均と、星ごとの件数・割合を出す', () => {
    const s = summarizeReviews(reviews);
    expect(s.average).toBe(3.5);
    expect(s.count).toBe(4);
    expect(s.rows[0]).toEqual({ stars: 5, count: 2, percent: 50 });
    expect(s.rows[4]).toEqual({ stars: 1, count: 1, percent: 25 });
  });

  it('レビューが無ければ 0', () => {
    expect(summarizeReviews([]).average).toBe(0);
  });
});

describe('sortReviews', () => {
  it('新しい順・評価の高い順・低い順に並べる', () => {
    expect(sortReviews(reviews, 'newest').map((r) => r.id)).toEqual(['b', 'c', 'a', 'd']);
    expect(sortReviews(reviews, 'highest')[0].rating).toBe(5);
    expect(sortReviews(reviews, 'lowest')[0].id).toBe('b');
  });
});

describe('ReviewSummaryComponent', () => {
  it('星ごとの行に読み上げ用の文言を付ける', async () => {
    TestBed.configureTestingModule({ imports: [ProductReviewsModule], providers: provideCustomTesting() });
    const fixture = TestBed.createComponent(ReviewSummaryComponent);
    fixture.componentRef.setInput('reviews', reviews);
    fixture.detectChanges();
    await fixture.whenStable();
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('li');
    expect(rows.length).toBe(5);
    expect(rows[0].getAttribute('aria-label')).toBe('星 5: 2 件');
  });
});
