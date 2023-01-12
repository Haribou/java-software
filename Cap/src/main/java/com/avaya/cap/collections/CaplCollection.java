/**
 *
 * @author Reinhard Klemm, Avaya
 *
 */

package com.avaya.cap.collections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.avaya.cap.CaplValue;

public class CaplCollection
{
	private CaplCollection()
	{
		// Don't instantiate this class - all methods are static.
	}
	
	public static CaplValue getSet()
	{
		return new CaplValue(new HashSet<CaplValue>(), Integer.MAX_VALUE);
	}
	
	public static CaplValue getSet(int capacity)
	{
		final Map<CaplValue, Boolean> setMap;
		
		if (capacity == Integer.MAX_VALUE)
			return getSet();
		
		setMap = new CaplHashMap<CaplValue, Boolean>(capacity);
		
		return new CaplValue(Collections.newSetFromMap(setMap), capacity);
	}
	
	public static CaplValue getSet(CaplValue capacity)
	{
		return getSet((int) capacity.getNumberValue());
	}
	
	public static CaplValue getSet(long now, long timeWindowLength)
	{
		return new CaplValue(new HashSet<CaplValue>(), now, timeWindowLength);
	}
	
	public static CaplValue getSet(CaplValue now, CaplValue timeWindowLength)
	{
		return getSet((long) now.getNumberValue(), (long) timeWindowLength.getNumberValue());
	}
	
	public static CaplValue getCombinedSet(CaplValue set1, CaplValue set2)
	{
		final Set<CaplValue> resultSet;
		
		final CaplHashMap<CaplValue, Boolean> setMap;
		
		int combinedCapacity;
		
		if (set1.getTimeWindowStart() != set2.getTimeWindowStart() || set1.getTimeWindowLength() != set2.getTimeWindowLength())
			return null;
		if (set1.isTimeWindowedStructure())
		{
			resultSet = new HashSet<CaplValue>();
			resultSet.addAll(set1.getSetValue());
			resultSet.addAll(set2.getSetValue());
			
   	  		return new CaplValue(resultSet, set1.getTimeWindowStart(), set1.getTimeWindowLength());
		}
		
		if (set1.getCapacity() == Integer.MAX_VALUE || set2.getCapacity() == Integer.MAX_VALUE)
		{
			resultSet = new HashSet<CaplValue>();
			resultSet.addAll(set1.getSetValue());
			resultSet.addAll(set2.getSetValue());
			
   	  		return new CaplValue(resultSet, Integer.MAX_VALUE);
		}
		
		try
   	  	{
			combinedCapacity = set1.getCapacity() + set2.getCapacity();
   	  	} catch (Exception e)
   	  	{
   	  		// Number overflow exception.
   	  		combinedCapacity = Integer.MAX_VALUE;
   	  	}
		
		setMap = new CaplHashMap<CaplValue, Boolean>(combinedCapacity);
		
		resultSet = Collections.newSetFromMap(setMap);
		resultSet.addAll(set1.getSetValue());
		resultSet.addAll(set2.getSetValue());
		
		return new CaplValue(resultSet, combinedCapacity);
	}
	
	public static CaplValue getDifferenceSet(CaplValue set1, CaplValue set2, boolean intersect)
	{
		final Set<CaplValue> resultSet;
		
		final CaplHashMap<CaplValue, Boolean> setMap;
		
		if (set1.getTimeWindowStart() != set2.getTimeWindowStart() || set1.getTimeWindowLength() != set2.getTimeWindowLength())
			return null;
		if (set1.isTimeWindowedStructure())
		{
			resultSet = new HashSet<CaplValue>();
			resultSet.addAll(set1.getSetValue());
			if (intersect)
				resultSet.retainAll(set2.getSetValue());
			else resultSet.removeAll(set2.getSetValue());
			
   	  		return new CaplValue(resultSet, set1.getTimeWindowStart(), set1.getTimeWindowLength());
		}
		
		if (set1.getCapacity() == Integer.MAX_VALUE)
		{
			resultSet = new HashSet<CaplValue>();
			resultSet.addAll(set1.getSetValue());
			if (intersect)
				resultSet.retainAll(set2.getSetValue());
			else resultSet.removeAll(set2.getSetValue());
			
   	  		return new CaplValue(resultSet, Integer.MAX_VALUE);
		}
		
		setMap = new CaplHashMap<CaplValue, Boolean>(set1.getCapacity());
		
		resultSet = Collections.newSetFromMap(setMap);
		resultSet.addAll(set1.getSetValue());
		if (intersect)
			resultSet.retainAll(set2.getSetValue());
		else resultSet.removeAll(set2.getSetValue());
		
		return new CaplValue(resultSet, set1.getCapacity());
	}
	
	public static CaplValue getMap()
	{
		return new CaplValue(new HashMap<String, CaplValue>(), Integer.MAX_VALUE);
	}
	
	public static CaplValue getMap(int capacity)
	{
		if (capacity == Integer.MAX_VALUE)
			return getMap();

		return new CaplValue(new CaplHashMap<String, CaplValue>(capacity), capacity);
	}
	
	public static CaplValue getMap(CaplValue capacity)
	{
		return getMap((int) capacity.getNumberValue());
	}
	
	public static CaplValue getMap(long now, long timeWindowLength)
	{
		return new CaplValue(new HashMap<String, CaplValue>(), now, timeWindowLength);
	}
	
	public static CaplValue getMap(CaplValue now, CaplValue timeWindowLength)
	{
		return getMap((long) now.getNumberValue(), (long) timeWindowLength.getNumberValue());
	}
	
	public static CaplValue getCombinedMap(CaplValue map1, CaplValue map2)
	{
		final Map<String, CaplValue> resultMap;
		
		int combinedCapacity;
		
		if (map1.getTimeWindowStart() != map2.getTimeWindowStart() || map1.getTimeWindowLength() != map2.getTimeWindowLength())
			return null;
		if (map1.isTimeWindowedStructure())
		{
			resultMap = new HashMap<String, CaplValue>();
			resultMap.putAll(map1.getMapValue());
			resultMap.putAll(map2.getMapValue());
			
   	  		return new CaplValue(resultMap, map1.getTimeWindowStart(), map1.getTimeWindowLength());
		}
		
		if (map1.getCapacity() == Integer.MAX_VALUE || map2.getCapacity() == Integer.MAX_VALUE)
		{
			resultMap = new HashMap<String, CaplValue>();
			resultMap.putAll(map1.getMapValue());
			resultMap.putAll(map2.getMapValue());
			
   	  		return new CaplValue(resultMap, Integer.MAX_VALUE);
		}
		
		try
   	  	{
			combinedCapacity = map1.getCapacity() + map2.getCapacity();
   	  	} catch (Exception e)
   	  	{
   	  		// Number overflow exception.
   	  		combinedCapacity = Integer.MAX_VALUE;
   	  	}
		
		resultMap = new CaplHashMap<String, CaplValue>(combinedCapacity);
		
		resultMap.putAll(map1.getMapValue());
		resultMap.putAll(map2.getMapValue());
		
		return new CaplValue(resultMap, combinedCapacity);
	}
	
	public static CaplValue getList()
	{
		return new CaplValue(new ArrayList<CaplValue>(), Integer.MAX_VALUE);
	}
	
	public static CaplValue getList(int capacity)
	{
		if (capacity == Integer.MAX_VALUE)
			return getList();

		return new CaplValue(new ArrayList<CaplValue>(), capacity);
	}
	
	public static CaplValue getList(CaplValue capacity)
	{
		if (capacity.getNumberValue() == Integer.MAX_VALUE)
			return getList();

		return getList((int) capacity.getNumberValue());
	}
	
	public static CaplValue getList(long now, long timeWindowLength)
	{
		return new CaplValue(new ArrayList<CaplValue>(), now, timeWindowLength);
	}
	
	public static CaplValue getList(CaplValue now, CaplValue timeWindowLength)
	{
		return getList((long) now.getNumberValue(), (long) timeWindowLength.getNumberValue());
	}
	
	public static CaplValue getCombinedList(CaplValue list1, CaplValue list2)
	{
		List<CaplValue> resultList;
		
		if (list1.getTimeWindowStart() != list2.getTimeWindowStart() || list1.getTimeWindowLength() != list2.getTimeWindowLength())
			return null;
		
		resultList = new ArrayList<CaplValue>();
   	  	resultList.addAll(list1.getListValue());
   	  	resultList.addAll(list2.getListValue());
   	  	if (list1.isTimeWindowedStructure())
   	  		return new CaplValue(resultList, list1.getTimeWindowStart(), list1.getTimeWindowLength());
   	  	if (list1.getCapacity() == Integer.MAX_VALUE || list2.getCapacity() == Integer.MAX_VALUE)
   	  		return new CaplValue(resultList, Integer.MAX_VALUE);
   	  	try
   	  	{
   	  		return new CaplValue(resultList, list1.getCapacity() + list2.getCapacity());
   	  	} catch (Exception e)
   	  	{
   	  		// Number overflow exception.
   	  		return new CaplValue(resultList, Integer.MAX_VALUE);
   	  	}
	}
	
	public static CaplValue getDifferenceList(CaplValue list1, CaplValue list2, boolean intersect)
	{
		List<CaplValue> resultList;
		
		if (list1.getTimeWindowStart() != list2.getTimeWindowStart() || list1.getTimeWindowLength() != list2.getTimeWindowLength())
			return null;
		
		resultList = new ArrayList<CaplValue>();
   	  	resultList.addAll(list1.getListValue());
   	  	if (intersect)
   	  		resultList.retainAll(list2.getListValue());
   	  	else resultList.removeAll(list2.getListValue());
   	  	if (list1.isTimeWindowedStructure())
   	  		return new CaplValue(resultList, list1.getTimeWindowStart(), list1.getTimeWindowLength());
   	  	if (list1.getCapacity() == Integer.MAX_VALUE)
   	  		return new CaplValue(resultList, Integer.MAX_VALUE);
   	  	return new CaplValue(resultList, list1.getCapacity());
	}
}