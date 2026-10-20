package de.hybris.platform.lite;

import de.hybris.platform.lite.impex.ImpExImporter;
import de.hybris.platform.lite.persistence.SchemaManager;
import de.hybris.platform.lite.setup.SystemSetup;
import java.util.Arrays;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportResource;

/**
 * 疑似 Commerce の入口。--hybris.mode で動きを変える:
 * server(OCC を 9002 で出す)、initialize、update、import(--hybris.impex=ファイル)。
 * 拡張の Spring の設定(*-spring.xml と *-web-spring.xml)は、クラスパスの一番上から全部読む。
 */
@SpringBootApplication(scanBasePackages = "de.hybris.platform.lite.config")
@ImportResource({ "classpath*:*-spring.xml" })
public class HybrisLiteApplication
{
	public static void main(final String[] args) throws Exception
	{
		final String mode = Arrays.stream(args).filter(a -> a.startsWith("--hybris.mode=")).map(a -> a.substring("--hybris.mode=".length()))
				.findFirst().orElse("server");
		final SpringApplication app = new SpringApplication(HybrisLiteApplication.class);
		if (!"server".equals(mode))
		{
			app.setWebApplicationType(WebApplicationType.NONE);
		}
		final ConfigurableApplicationContext ctx = app.run(args);
		if ("server".equals(mode))
		{
			return;
		}
		final SystemSetup setup = new SystemSetup(ctx.getBean(SchemaManager.class), ctx.getBean(ImpExImporter.class));
		final boolean ok = switch (mode)
		{
			case "initialize" -> setup.initialize();
			case "update" -> setup.update();
			case "import" -> setup.importFile(Arrays.stream(args).filter(a -> a.startsWith("--hybris.impex=")).map(a -> a.substring(15)).findFirst()
					.orElseThrow(() -> new IllegalArgumentException("--hybris.impex=<file> is required")));
			default -> throw new IllegalArgumentException("unknown --hybris.mode=" + mode);
		};
		ctx.close();
		System.exit(ok ? 0 : 1);
	}
}
