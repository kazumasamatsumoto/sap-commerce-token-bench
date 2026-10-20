import { inject, InjectionToken, Provider } from '@angular/core';

/**
 * Spartacus の Config をまねたもの。provideConfig で渡した設定の断片(chunk)を
 * 順に深くマージして、1 つの設定として Config から引けるようにする。
 * OccConfig や CmsConfig などの型付きの設定は、すべて Config と同じものを指す。
 */
export abstract class Config {}

export const ConfigChunk = new InjectionToken<object[]>('ConfigChunk');
export const DefaultConfigChunk = new InjectionToken<object[]>('DefaultConfigChunk');

export function provideConfig(config: object): Provider {
  return { provide: ConfigChunk, useValue: config, multi: true };
}

export function provideDefaultConfig(config: object): Provider {
  return { provide: DefaultConfigChunk, useValue: config, multi: true };
}

function isObject(item: unknown): item is Record<string, unknown> {
  return !!item && typeof item === 'object' && !Array.isArray(item);
}

export function deepMerge<T extends object>(target: T, ...sources: object[]): T {
  const out = { ...target } as Record<string, unknown>;
  for (const source of sources) {
    for (const [key, value] of Object.entries(source ?? {})) {
      out[key] = isObject(value) && isObject(out[key]) ? deepMerge(out[key] as object, value) : value;
    }
  }
  return out as T;
}

export function configFactory(): Config {
  const defaults = inject(DefaultConfigChunk, { optional: true }) ?? [];
  const chunks = inject(ConfigChunk, { optional: true }) ?? [];
  return deepMerge({}, ...defaults, ...chunks);
}

export const configProviders: Provider[] = [{ provide: Config, useFactory: configFactory }];
