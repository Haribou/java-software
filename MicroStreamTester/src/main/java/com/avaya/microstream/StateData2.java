package com.avaya.microstream;

import org.springframework.beans.factory.annotation.Autowired;

import one.microstream.integrations.spring.boot.types.Storage;
import one.microstream.storage.types.StorageManager;

@Storage
public class StateData2
{
	@Autowired
	private transient StorageManager storageManager;
	
	private LimitedSet2 stateDataField;
	
	public StateData2()
	{
		stateDataField = new LimitedSet2(); 
		
		System.out.println("Created StateData");
	}

	public LimitedSet2 getLimitedSet()
	{
		return stateDataField;
	}
	
	public void save()
	{
		storageManager.store(stateDataField);
		System.out.println("Persisted state data with " + stateDataField.size() + " entries");
	}
}
