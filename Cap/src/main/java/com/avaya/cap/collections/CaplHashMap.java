/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap.collections;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class CaplHashMap<S, T> extends LinkedHashMap<S, T>
{
	private int capacityField;
	
	public CaplHashMap(int capacity)
	{
		capacityField = capacity;
	}
	
	@Override
  	protected boolean removeEldestEntry(Entry<S, T> eldest)
  	{
  	  	return size() > capacityField;
  	}
}
