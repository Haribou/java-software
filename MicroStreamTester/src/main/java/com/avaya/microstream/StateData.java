package com.avaya.microstream;

import java.util.HashMap;

import org.apache.log4j.Logger;

import one.microstream.integrations.spring.boot.types.Storage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@Storage
class StateData
{
	private static final Logger LOG = Logger.getLogger(StateData.class);
	
	private final HashMap<String, String> stateDataField = new HashMap<>();
	
	StateData()
	{
		LOG.debug("Created StateData");
	}

	public HashMap<String, String> getStateData()
	{
		return stateDataField;
	}
	
	public void save(EmbeddedStorageManager storageManager)
	{
		storageManager.store(stateDataField);
	}
}
