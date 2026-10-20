import { setupWorker } from 'msw/browser';
import { handlers } from './handlers';

/**
 * ブラウザで OCC への要求を横取りする。モックに無い要求(画像など)は、そのまま外へ流す。
 * MSW 3 では、2 系の onUnhandledRequest が onUnhandledFrame に変わった。
 */
export const worker = setupWorker(...handlers);

export function startMocks(): Promise<unknown> {
  return worker.start({ onUnhandledFrame: 'bypass', quiet: true });
}
