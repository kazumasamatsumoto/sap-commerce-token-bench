import { Component } from '@angular/core';

/** アプリの外枠。Spartacus の <cx-storefront> と同じく、header / main / footer の 3 つで組む。 */
@Component({
  selector: 'cx-storefront',
  standalone: false,
  template: `
    <header>
      <cx-page-layout section="header"></cx-page-layout>
    </header>
    <main>
      <router-outlet></router-outlet>
    </main>
    <footer>
      <cx-page-layout section="footer"></cx-page-layout>
    </footer>
  `,
})
export class StorefrontComponent {}
