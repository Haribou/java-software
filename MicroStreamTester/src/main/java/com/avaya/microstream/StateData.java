package com.avaya.microstream;

import java.util.Collections;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import one.microstream.integrations.spring.boot.types.Storage;
import one.microstream.storage.types.StorageManager;

@Storage
public class StateData
{
	private static final int MAX_NUM_SET_ENTRIES = 5;
	
	@Autowired
	private transient StorageManager storageManager;
	
	private Set<String> stateDataField;
	
	public StateData()
	{
		/** Works correctly when uncommenting the following line: repeat startup of this Spring Boot application loads the persisted data successfully. **/
		// stateDataField = new HashSet<String>(); 
		
		/** Does not work correctly when uncommenting the following line: repeat startup of this Spring Boot application does not load persisted data - or data is not persisted in the first place **/
		stateDataField = Collections.newSetFromMap(new LimitedSet(MAX_NUM_SET_ENTRIES));
		
		System.out.println("Created StateData");
	}

	public Set<String> getStateData()
	{
		return stateDataField;
	}
	
	public void save()
	{
		storageManager.store(stateDataField);
		System.out.println("Persisted state data with " + stateDataField.size() + " entries");
	}
}
