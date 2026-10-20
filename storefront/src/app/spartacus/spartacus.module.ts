import { NgModule } from '@angular/core';
import { StorefrontModule } from '@xyz/storefront';
import { SpartacusConfigurationModule } from './spartacus-configuration.module';
import { SpartacusFeaturesModule } from './spartacus-features.module';

@NgModule({
  imports: [StorefrontModule.forRoot(), SpartacusFeaturesModule, SpartacusConfigurationModule],
  exports: [StorefrontModule],
})
export class SpartacusModule {}
