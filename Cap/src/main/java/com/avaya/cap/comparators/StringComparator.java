/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap.comparators;

import java.util.Comparator;

public class StringComparator implements Comparator<String>
{
	private boolean isAscendingField;
	
	public StringComparator(boolean isAscending)
	{
		isAscendingField = isAscending;
	}
	
  	@Override
	public int compare(String value1, String value2) throws ClassCastException
	{
  		if (isAscendingField)
  			return value1.compareTo(value2);
  		return value2.compareTo(value1);
	}
}