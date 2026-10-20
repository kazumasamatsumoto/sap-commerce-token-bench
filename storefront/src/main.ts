import { platformBrowser } from '@angular/platform-browser';
import { AppModule } from './app/app-module';
import { environment } from './environments/environment';

async function bootstrap(): Promise<void> {
  if (environment.useMocks) {
    // モックのときだけ、OCC への要求を MSW で横取りしてから起動する
    const { startMocks } = await import('../mocks/browser');
    await startMocks();
  }
  await platformBrowser().bootstrapModule(AppModule);
}

bootstrap().catch((err) => console.error(err));
