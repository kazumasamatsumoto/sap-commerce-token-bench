import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrentProductService } from '../../shared';

/** 商品の素材(自社拡張の material)。カートのボタンの後(PDP.ADDTOCART / AFTER)に出す。 */
@Component({
  selector: 'xyz-material-info',
  standalone: false,
  templateUrl: './material-info.component.html',
  styleUrl: './material-info.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MaterialInfoComponent {
  readonly product$ = inject(CurrentProductService).getProduct();
}
