import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { PageLayoutComponent } from '@xyz/storefront';

/** Spartacus と同じく、ページの中身は CMS で決める。どの URL も PageLayoutComponent が描く。 */
const routes: Routes = [{ path: '**', component: PageLayoutComponent }];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
