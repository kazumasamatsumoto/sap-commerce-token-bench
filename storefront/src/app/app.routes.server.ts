import { RenderMode, ServerRoute } from '@angular/ssr';

/** 商品や在庫が変わるので、事前描画(Prerender)はせず、要求ごとに SSR で描く。 */
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Server,
  },
];
