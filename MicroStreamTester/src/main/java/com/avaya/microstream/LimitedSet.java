package com.avaya.microstream;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class LimitedSet extends LinkedHashMap<String, Boolean>
{
	private int capacityField;
	
	public LimitedSet(int capacity)
	{
		capacityField = capacity;
	}
	
	@Override
  	protected boolean removeEldestEntry(Entry<String, Boolean> eldest)
  	{
  	  	return size() > capacityField;
  	}
}

