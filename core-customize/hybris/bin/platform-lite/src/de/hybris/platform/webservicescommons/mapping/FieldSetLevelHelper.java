package de.hybris.platform.webservicescommons.mapping;

/** fields の段階の名前。Controller の @RequestParam(defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) に使う。 */
public final class FieldSetLevelHelper
{
	public static final String BASIC_LEVEL = "BASIC";
	public static final String DEFAULT_LEVEL = "DEFAULT";
	public static final String FULL_LEVEL = "FULL";

	private FieldSetLevelHelper()
	{
	}
}
