package de.hybris.platform.store.services;

import de.hybris.platform.store.model.BaseStoreModel;

public interface BaseStoreService
{
	BaseStoreModel getCurrentBaseStore();

	BaseStoreModel getBaseStoreForUid(String uid);
}
