package de.hybris.platform.lite.setup;

import de.hybris.platform.lite.impex.ImpExException;
import de.hybris.platform.lite.impex.ImpExImporter;
import de.hybris.platform.lite.persistence.SchemaManager;
import de.hybris.platform.lite.runtime.TypeSystemHolder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * ant initialize / ant updatesystem / ant importImpex の中身。
 * 初期データは拡張ごとに次の場所に置く(localextensions.xml の順に入れる):
 * <ul>
 * <li>resources/&lt;拡張&gt;/import/common/*.impex … 必須のデータ(essential data)。initialize と updatesystem で入れる</li>
 * <li>resources/&lt;拡張&gt;/import/sampledata/**&#47;*.impex … 案件のデータ(project data)。initialize で入れる</li>
 * </ul>
 */
public class SystemSetup
{
	private static final Logger LOG = LoggerFactory.getLogger(SystemSetup.class);

	private final SchemaManager schemaManager;
	private final ImpExImporter importer;
	private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	public SystemSetup(final SchemaManager schemaManager, final ImpExImporter importer)
	{
		this.schemaManager = schemaManager;
		this.importer = importer;
	}

	public boolean initialize() throws IOException
	{
		LOG.info("**** initializing system - all existing data will be removed ****");
		schemaManager.dropAll();
		final int created = schemaManager.createOrUpdate();
		LOG.info("created {} tables / columns", created);
		return importData("common") & importData("sampledata");
	}

	public boolean update() throws IOException
	{
		LOG.info("**** updating running system ****");
		final int changes = schemaManager.createOrUpdate();
		LOG.info(changes == 0 ? "type system is up to date" : "added {} tables / columns", changes);
		return importData("common");
	}

	public boolean importFile(final String path) throws IOException
	{
		final List<Path> candidates = List.of(Path.of(path), Path.of("..").resolve(path), Path.of("../custom").resolve(path));
		for (final Path p : candidates)
		{
			if (Files.isRegularFile(p))
			{
				return run(p.toString(), Files.readString(p, StandardCharsets.UTF_8));
			}
		}
		final Resource r = resolver.getResource("classpath:" + path.replaceFirst("^/", ""));
		if (r.exists())
		{
			return run(path, r.getContentAsString(StandardCharsets.UTF_8));
		}
		LOG.error("ImpEx file not found: {} (tried {} and the classpath)", path, candidates);
		return false;
	}

	private boolean importData(final String folder) throws IOException
	{
		boolean ok = true;
		for (final String ext : TypeSystemHolder.extensions())
		{
			// 本物と同じく、拡張のフォルダ(resources/)のファイルをディスクから読む。ImpEx を直したら ant all をしなくても効く。
			// 拡張のフォルダが見つからないとき(jar だけを配ったとき)だけ、jar の中から読む。
			final Path onDisk = extensionResources(ext).resolve(ext + "/import/" + folder);
			final List<Resource> files = Files.isDirectory(onDisk)
					? new ArrayList<>(Arrays.asList(resolver.getResources("file:" + onDisk.toAbsolutePath().toString().replace('\\', '/') + "/**/*.impex")))
					: new ArrayList<>(Arrays.asList(resolver.getResources("classpath*:" + ext + "/import/" + folder + "/**/*.impex")));
			files.sort(Comparator.comparing(r -> uriOf(r)));
			for (final Resource r : files)
			{
				final String name = "/" + ext + "/import/" + folder + uriOf(r).substring(uriOf(r).indexOf("/import/" + folder) + ("/import/" + folder).length());
				ok &= run(name, r.getContentAsString(StandardCharsets.UTF_8));
			}
		}
		return ok;
	}

	/** 拡張の resources/ のフォルダ。ant と hybrisserver は hybris/bin/platform で動くので、そこから探す。 */
	private static Path extensionResources(final String ext)
	{
		for (final Path dir : List.of(Path.of("../custom", ext), Path.of("..", ext), Path.of("../platform-lite")))
		{
			if (Files.isRegularFile(dir.resolve("extensioninfo.xml")) && Files.isDirectory(dir.resolve("resources/" + ext)))
			{
				return dir.resolve("resources");
			}
		}
		return Path.of("does-not-exist");
	}

	private static String uriOf(final Resource r)
	{
		try
		{
			return r.getURI().toString();
		}
		catch (final IOException e)
		{
			return r.getDescription();
		}
	}

	private boolean run(final String name, final String script)
	{
		LOG.info("importing {}", name);
		final long start = System.currentTimeMillis();
		try
		{
			final ImpExImporter.Result result = importer.importScript(name, script);
			result.warnings().forEach(LOG::warn);
			if (!result.isSuccessful())
			{
				LOG.error("Import has caused an error, see following unresolved lines ({}):", result.unresolved().size());
				result.unresolved().forEach(l -> LOG.error("  {}", l));
				return false;
			}
			LOG.info("imported {} lines from {} in {} ms", result.imported(), name, System.currentTimeMillis() - start);
			return true;
		}
		catch (final ImpExException e)
		{
			LOG.error("Import of {} failed: {}", name, e.getMessage());
			return false;
		}
	}
}
