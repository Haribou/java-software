/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap.comparators;

import java.util.Comparator;
import java.util.Map.Entry;

import com.avaya.cap.CaplValue;

public class CaplMapValueComparator implements Comparator<Entry<String, CaplValue>>
{
	private CaplValueComparator caplValueComparator;
	
	public CaplMapValueComparator(boolean isAscending)
	{
		caplValueComparator = new CaplValueComparator(isAscending);
	}
	
	@Override
	public int compare(Entry<String, CaplValue> value1, Entry<String, CaplValue> value2) throws ClassCastException
	{
		return caplValueComparator.compare(value1.getValue(), value2.getValue());
	}
}
