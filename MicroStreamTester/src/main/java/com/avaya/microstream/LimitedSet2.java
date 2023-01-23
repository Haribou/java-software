package com.avaya.microstream;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class LimitedSet2
{
	private final static int CAPACITY = 5;
	
	private final Set<String> storage = new LinkedHashSet<>();
	
	public void add(String setElement)
	{
		Iterator<String> storageIterator;
		
		if (storage.size() == CAPACITY)
		{
			storageIterator = storage.iterator();
			storageIterator.next();
			storageIterator.remove();
		}
		storage.add(setElement);
	}
	
	public boolean contains(String setEelement) 
	{
		return storage.contains(setEelement);
	}
	
	public int size()
	{
		return storage.size();
	}
	
	public String getContent()
	{
		return String.join("\n", storage);
	}
}
