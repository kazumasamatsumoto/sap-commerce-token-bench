import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { OccConfig, TranslationService } from '@xyz/storefront';

/**
 * ヘッダーの言語の切り替え(Outlet SiteContext / AFTER)。画面の文言だけを切り替える
 * (OCC に送る言語は変えない。疑似ライブラリにその仕組みが無いため)。
 */
@Component({
  selector: 'xyz-language-switcher',
  standalone: false,
  templateUrl: './language-switcher.component.html',
  styleUrl: './language-switcher.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageSwitcherComponent {
  private readonly translation = inject(TranslationService);
  readonly languages = inject(OccConfig).context?.language ?? ['ja'];
  readonly current = this.translation.language;

  change(lang: string): void {
    this.translation.language.set(lang);
  }
}
