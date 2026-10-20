package de.hybris.platform.core;

import java.io.Serializable;

/** 行の主キー。本物と同じく long の値を包む。 */
public final class PK implements Serializable, Comparable<PK>
{
	private final long value;

	private PK(final long value)
	{
		this.value = value;
	}

	public static PK fromLong(final long value)
	{
		return new PK(value);
	}

	public static PK parse(final String value)
	{
		return new PK(Long.parseLong(value));
	}

	public long getLongValue()
	{
		return value;
	}

	public String getLongValueAsString()
	{
		return Long.toString(value);
	}

	@Override
	public boolean equals(final Object o)
	{
		return o instanceof PK pk && pk.value == value;
	}

	@Override
	public int hashCode()
	{
		return Long.hashCode(value);
	}

	@Override
	public int compareTo(final PK o)
	{
		return Long.compare(value, o.value);
	}

	@Override
	public String toString()
	{
		return Long.toString(value);
	}
}
