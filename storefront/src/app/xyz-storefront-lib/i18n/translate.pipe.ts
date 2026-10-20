import { inject, Pipe, PipeTransform } from '@angular/core';
import { TranslationService } from './translation.service';

/** `{{ 'product.addToCart' | cxTranslate }}` / `{{ 'cart.items' | cxTranslate: { count: 3 } }}` */
@Pipe({ name: 'cxTranslate', pure: false, standalone: false })
export class TranslatePipe implements PipeTransform {
  private readonly translation = inject(TranslationService);

  transform(key: string, params?: Record<string, unknown>): string {
    return this.translation.translate(key, params);
  }
}
