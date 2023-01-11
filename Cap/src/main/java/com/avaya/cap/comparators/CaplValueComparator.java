/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap.comparators;

import java.util.Comparator;

import com.avaya.cap.CaplValue;
import com.avaya.cap.ValueDataType;

public class CaplValueComparator implements Comparator<CaplValue>
{
	private boolean isAscendingField;
	
	public CaplValueComparator(boolean isAscending)
	{
		isAscendingField = isAscending;
	}
	
	@Override
	public int compare(CaplValue value1, CaplValue value2) throws ClassCastException
	{
	  	if (value1.getValueDataType() == ValueDataType.NUMBER && value2.getValueDataType() == ValueDataType.NUMBER)
	  	{
	  		if (isAscendingField)
	  		{
				if (value1.getNumberValue() < value2.getNumberValue())
					return -1;
				if (value1.getNumberValue() > value2.getNumberValue())
					return 1;
	  		}
	  		else
	  		{
	  			if (value1.getNumberValue() < value2.getNumberValue())
					return 1;
				if (value1.getNumberValue() > value2.getNumberValue())
					return -1;
	  		}
			return 0;
		}
		else if (value1.getValueDataType() == ValueDataType.STRING && value2.getValueDataType() == ValueDataType.STRING)
			if (isAscendingField)
				return value1.getStringValue().compareTo(value2.getStringValue());
			else return value2.getStringValue().compareTo(value1.getStringValue());
		else if (value1.getValueDataType() == ValueDataType.BOOLEAN && value2.getValueDataType() == ValueDataType.BOOLEAN)
			if (value1.getBooleanValue() == value2.getBooleanValue())
				return 0;
			else if (isAscendingField)
				if (value1.getBooleanValue())
					return 1;
				else return -1;
			else if (value1.getBooleanValue())
				return -1;
			else return 1;

		throw new ClassCastException();
	}
}
