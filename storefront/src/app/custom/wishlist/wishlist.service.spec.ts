import { PLATFORM_ID } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { toggleCode, WISHLIST_KEY, WishlistService } from './wishlist.service';

describe('toggleCode', () => {
  it('無ければ先頭に足し、あれば外す', () => {
    expect(toggleCode(['a'], 'b')).toEqual(['b', 'a']);
    expect(toggleCode(['a', 'b'], 'a')).toEqual(['b']);
  });
});

describe('WishlistService', () => {
  beforeEach(() => localStorage.clear());

  it('ブラウザでは切り替えと削除を localStorage に覚える', () => {
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'browser' }] });
    const service = TestBed.inject(WishlistService);
    const codes = service.getCodes();
    service.toggle('XYZ-1');
    service.toggle('XYZ-2');
    service.toggle('XYZ-1');
    expect(codes()).toEqual(['XYZ-2']);
    service.remove('XYZ-2');
    expect(codes()).toEqual([]);
    expect(localStorage.getItem(WISHLIST_KEY)).toBe('[]');
  });

  it('前に覚えたものを読み直す', () => {
    localStorage.setItem(WISHLIST_KEY, JSON.stringify(['XYZ-9', 42]));
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'browser' }] });
    expect(TestBed.inject(WishlistService).getCodes()()).toEqual(['XYZ-9']);
  });

  it('SSR(サーバー)では localStorage に書かない', () => {
    const setItem = vi.spyOn(Storage.prototype, 'setItem');
    TestBed.configureTestingModule({ providers: [{ provide: PLATFORM_ID, useValue: 'server' }] });
    TestBed.inject(WishlistService).toggle('XYZ-1');
    expect(setItem).not.toHaveBeenCalled();
    setItem.mockRestore();
  });
});
