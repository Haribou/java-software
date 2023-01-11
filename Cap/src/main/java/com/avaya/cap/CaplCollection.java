/**
 *
 * @author Reinhard Klemm, Avaya
 *
 */

package com.avaya.cap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

class CaplCollection
{
	static CaplValue getSet()
	{
		return new CaplValue(new HashSet<CaplValue>(), Integer.MAX_VALUE);
	}
	
	static CaplValue getSet(int capacity)
	{
		if (capacity == Integer.MAX_VALUE)
			return getSet();
		
		final Map<CaplValue, Boolean> setMap = new LinkedHashMap<CaplValue, Boolean>()
		{
		  	@Override
		  	protected boolean removeEldestEntry(Entry<CaplValue, Boolean> eldest)
		  	{
		  	  	return size() > capacity;
		  	}
		};
		
		return new CaplValue(Collections.newSetFromMap(setMap), capacity);
	}
	
	static CaplValue getSet(CaplValue capacity)
	{
		return getSet((int) capacity.getNumberValue());
	}
	
	static CaplValue getSet(long now, long timeWindowLength)
	{
		return new CaplValue(new HashSet<CaplValue>(), now, timeWindowLength);
	}
	
	static CaplValue getSet(CaplValue now, CaplValue timeWindowLength)
	{
		return getSet((long) now.getNumberValue(), (long) timeWindowLength.getNumberValue());
	}
	
	static CaplValue getCombinedSet(CaplValue set1, CaplValue set2)
	{
		Set<CaplValue> resultSet;
		
		Map<CaplValue, Boolean> setMap;
		
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
		
		setMap = new LinkedHashMap<CaplValue, Boolean>()
		{
		  	@Override
		  	protected boolean removeEldestEntry(Entry<CaplValue, Boolean> eldest)
		  	{
		  	  	return size() > set1.getCapacity() + set2.getCapacity();
		  	}
		};
		
		resultSet = Collections.newSetFromMap(setMap);
		resultSet.addAll(set1.getSetValue());
		resultSet.addAll(set2.getSetValue());
		
		return new CaplValue(resultSet, combinedCapacity);
	}
	
	static CaplValue getDifferenceSet(CaplValue set1, CaplValue set2, boolean intersect)
	{
		Set<CaplValue> resultSet;
		
		Map<CaplValue, Boolean> setMap;
		
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
		
		setMap = new LinkedHashMap<CaplValue, Boolean>()
		{
		  	@Override
		  	protected boolean removeEldestEntry(Entry<CaplValue, Boolean> eldest)
		  	{
		  	  	return size() > set1.getCapacity();
		  	}
		};
		
		resultSet = Collections.newSetFromMap(setMap);
		resultSet.addAll(set1.getSetValue());
		if (intersect)
			resultSet.retainAll(set2.getSetValue());
		else resultSet.removeAll(set2.getSetValue());
		
		return new CaplValue(resultSet, set1.getCapacity());
	}
	
	static CaplValue getMap()
	{
		return new CaplValue(new HashMap<String, CaplValue>(), Integer.MAX_VALUE);
	}
	
	static CaplValue getMap(int capacity)
	{
		if (capacity == Integer.MAX_VALUE)
			return getMap();

		return new CaplValue(new LinkedHashMap<String, CaplValue>()
		{
		  	@Override
		  	protected boolean removeEldestEntry(Entry<String, CaplValue> eldest)
		  	{
		  	  	return size() > capacity;
		  	}
		}, capacity);
	}
	
	static CaplValue getMap(CaplValue capacity)
	{
		return getMap((int) capacity.getNumberValue());
	}
	
	static CaplValue getMap(long now, long timeWindowLength)
	{
		return new CaplValue(new HashMap<String, CaplValue>(), now, timeWindowLength);
	}
	
	static CaplValue getMap(CaplValue now, CaplValue timeWindowLength)
	{
		return getMap((long) now.getNumberValue(), (long) timeWindowLength.getNumberValue());
	}
	
	static CaplValue getCombinedMap(CaplValue map1, CaplValue map2)
	{
		Map<String, CaplValue> resultMap;
		
		int capacity;
		
		final int combinedCapacity;
		
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
			capacity = map1.getCapacity() + map2.getCapacity();
   	  	} catch (Exception e)
   	  	{
   	  		// Number overflow exception.
   	  		capacity = Integer.MAX_VALUE;
   	  	}
		combinedCapacity = capacity;
		
		resultMap = new LinkedHashMap<String, CaplValue>()
		{
		  	@Override
		  	protected boolean removeEldestEntry(Entry<String, CaplValue> eldest)
		  	{
		  	  	return size() > combinedCapacity;
		  	}
		};
		
		resultMap.putAll(map1.getMapValue());
		resultMap.putAll(map2.getMapValue());
		
		return new CaplValue(resultMap, combinedCapacity);
	}
	
	static CaplValue getList()
	{
		return new CaplValue(new ArrayList<CaplValue>(), Integer.MAX_VALUE);
	}
	
	static CaplValue getList(int capacity)
	{
		if (capacity == Integer.MAX_VALUE)
			return getList();

		return new CaplValue(new ArrayList<CaplValue>(), capacity);
	}
	
	static CaplValue getList(CaplValue capacity)
	{
		if (capacity.getNumberValue() == Integer.MAX_VALUE)
			return getList();

		return getList((int) capacity.getNumberValue());
	}
	
	static CaplValue getList(long now, long timeWindowLength)
	{
		return new CaplValue(new ArrayList<CaplValue>(), now, timeWindowLength);
	}
	
	static CaplValue getList(CaplValue now, CaplValue timeWindowLength)
	{
		return getList((long) now.getNumberValue(), (long) timeWindowLength.getNumberValue());
	}
	
	static CaplValue getCombinedList(CaplValue list1, CaplValue list2)
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
	
	static CaplValue getDifferenceList(CaplValue list1, CaplValue list2, boolean intersect)
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