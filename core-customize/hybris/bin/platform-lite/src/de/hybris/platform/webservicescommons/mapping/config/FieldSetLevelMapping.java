package de.hybris.platform.webservicescommons.mapping.config;

import java.util.HashMap;
import java.util.Map;

/**
 * WsDTO ごとに、BASIC / DEFAULT / FULL がどの項目を指すか。本物と同じく *-web-spring.xml に書く:
 * <pre>
 * &lt;bean parent="fieldSetLevelMapping" id="productWsDTOFieldSetLevelMapping"&gt;
 *     &lt;property name="dtoClass" value="de.hybris.platform.commercewebservicescommons.dto.product.ProductWsDTO"/&gt;
 *     &lt;property name="levelMapping"&gt;&lt;map&gt;
 *         &lt;entry key="BASIC" value="code,name,url"/&gt;
 *         &lt;entry key="DEFAULT" value="BASIC,price,images,stock"/&gt;
 *     &lt;/map&gt;&lt;/property&gt;
 * &lt;/bean&gt;
 * </pre>
 * 同じ dtoClass の mapping が複数あると、後に読まれたものの level が足される(拡張から項目を足せる)。
 */
public class FieldSetLevelMapping
{
	private Class<?> dtoClass;
	private Map<String, String> levelMapping = new HashMap<>();

	public Class<?> getDtoClass()
	{
		return dtoClass;
	}

	public void setDtoClass(final Class<?> dtoClass)
	{
		this.dtoClass = dtoClass;
	}

	public Map<String, String> getLevelMapping()
	{
		return levelMapping;
	}

	public void setLevelMapping(final Map<String, String> levelMapping)
	{
		this.levelMapping = levelMapping;
	}
}
