/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap.comparators;

import java.util.Comparator;

import com.avaya.cap.CaplValue;

public class FrequencyMapComparator implements Comparator<CaplValue>
{
	private boolean isAscendingField;
	
	public FrequencyMapComparator(boolean isAscending)
	{
		isAscendingField = isAscending;
	}
	
	@Override
	public int compare(CaplValue value1, CaplValue value2)
	{
		final double frequency1 = value1.getMapValue().get("f").getNumberValue(),
					 frequency2 = value2.getMapValue().get("f").getNumberValue();
		
		if (frequency1 == frequency2)
			return 0;
		if (isAscendingField)
			if (frequency1 < frequency2)
				return -1;
			else return 1;
		if (frequency1 < frequency2)
			return 1;
		return -1;
	}
}
