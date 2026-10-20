import { NgModule } from '@angular/core';
import { provideServerRendering, withRoutes } from '@angular/ssr';
import { provideConfig } from '@xyz/storefront';
import { App } from './app';
import { AppModule } from './app-module';
import { serverRoutes } from './app.routes.server';

/**
 * SSR のときだけ、OCC の接続先を環境変数 OCC_BASE_URL から読む(Docker の中では http://commerce:9002)。
 * 画像の URL(backend.media.baseUrl)はブラウザから届く先のままにする。
 */
@NgModule({
  imports: [AppModule],
  providers: [
    provideServerRendering(withRoutes(serverRoutes)),
    provideConfig({ backend: { occ: { baseUrl: process.env['OCC_BASE_URL'] ?? 'http://localhost:9002' } } }),
  ],
  bootstrap: [App],
})
export class AppServerModule {}
