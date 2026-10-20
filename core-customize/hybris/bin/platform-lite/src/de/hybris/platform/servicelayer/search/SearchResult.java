package de.hybris.platform.servicelayer.search;

import java.util.List;

public interface SearchResult<E>
{
	List<E> getResult();

	int getTotalCount();

	int getCount();

	int getRequestedStart();

	int getRequestedCount();
}
