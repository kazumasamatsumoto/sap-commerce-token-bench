package de.hybris.platform.servicelayer.session;

/** 要求(スレッド)ごとの状態。OCC では要求の始めに作られ、終わりに消える。 */
public interface SessionService
{
	<T> T getAttribute(String name);

	void setAttribute(String name, Object value);

	void removeAttribute(String name);

	void closeCurrentSession();
}
