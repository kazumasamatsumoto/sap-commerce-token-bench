import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { pushRecent, RECENTLY_VIEWED_KEY, RecentlyViewedService } from './recently-viewed.service';

describe('pushRecent', () => {
  it('先頭に足し、同じものは 1 つにまとめ、max 件で切る', () => {
    expect(pushRecent(['a', 'b', 'c'], 'b')).toEqual(['b', 'a', 'c']);
    expect(pushRecent(['a', 'b', 'c'], 'd', 3)).toEqual(['d', 'a', 'b']);
  });
});

describe('RecentlyViewedService', () => {
  beforeEach(() => localStorage.clear());

  it('ブラウザでは localStorage に覚え、読み直せる', () => {
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'browser' }] });
    const service = TestBed.inject(RecentlyViewedService);
    service.add('XYZ-1');
    service.add('XYZ-2');
    expect(service.getCodes()()).toEqual(['XYZ-2', 'XYZ-1']);
    expect(JSON.parse(localStorage.getItem(RECENTLY_VIEWED_KEY)!)).toEqual(['XYZ-2', 'XYZ-1']);
  });

  it('壊れた値が入っていても落ちない', () => {
    localStorage.setItem(RECENTLY_VIEWED_KEY, '{not json');
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'browser' }] });
    expect(TestBed.inject(RecentlyViewedService).getCodes()()).toEqual([]);
  });

  it('SSR(サーバー)では何も覚えず、localStorage も触らない', () => {
    localStorage.setItem(RECENTLY_VIEWED_KEY, JSON.stringify(['OLD']));
    const getItem = vi.spyOn(Storage.prototype, 'getItem');
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'server' }] });
    const service = TestBed.inject(RecentlyViewedService);
    service.add('XYZ-1');
    expect(service.getCodes()()).toEqual([]);
    expect(getItem).not.toHaveBeenCalled();
    getItem.mockRestore();
  });
});
