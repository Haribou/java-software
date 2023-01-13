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
	private int combinedCapacityField;
	
	public CaplHashMap(int combinedCapacity)
	{
		combinedCapacityField = combinedCapacity;
	}
	
	@Override
  	protected boolean removeEldestEntry(Entry<S, T> eldest)
  	{
  	  	return size() > combinedCapacityField;
  	}
}
