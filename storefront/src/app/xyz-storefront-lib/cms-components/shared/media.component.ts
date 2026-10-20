import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { OccConfig } from '../../occ/occ-config';

/**
 * OCC の画像を出す。/medias/... の URL には backend.media.baseUrl を頭に付ける
 * (SSR のときもブラウザから届く URL にするため、occ.baseUrl とは別に持つ)。
 */
@Component({
  selector: 'cx-media',
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `@if (url()) {
    <img [src]="url()" [alt]="alt() ?? ''" loading="lazy" [attr.width]="width()" [attr.height]="width()" />
  }`,
})
export class MediaComponent {
  readonly src = input<string | undefined>();
  readonly alt = input<string | undefined>();
  readonly width = input<number>(300);

  private readonly baseUrl = inject(OccConfig).backend?.media?.baseUrl ?? '';

  readonly url = computed(() => {
    const src = this.src();
    if (!src) {
      return undefined;
    }
    return src.startsWith('/') ? `${this.baseUrl}${src}` : src;
  });
}
