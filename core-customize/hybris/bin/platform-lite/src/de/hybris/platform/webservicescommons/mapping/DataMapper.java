package de.hybris.platform.webservicescommons.mapping;

/** Data を WsDTO に写す。fields で、返す項目を絞る(BASIC / DEFAULT / FULL や code,name,price(FULL))。 */
public interface DataMapper
{
	<T> T map(Object source, Class<T> destinationClass, String fields);

	<T> T map(Object source, Class<T> destinationClass);

	/** WsDTO(受け取った値)を Data に写す。fields は使わない。 */
	<T> T mapToData(Object source, Class<T> destinationClass);
}
