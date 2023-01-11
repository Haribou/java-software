/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import java.util.HashMap;

class AnalysisState
{
	private final HashMap<String, CaplValue> analysisStateField = new HashMap<>();
	
	private long lastAnalysisStateChangeField = System.currentTimeMillis();
	
	private boolean isNewState = true;
	
	AnalysisState()
	{}
	
	AnalysisState(Mutability mutability)
	{
		reinitialize(mutability);
	}
	
	void reinitialize(Mutability mutability)
	{
		lastAnalysisStateChangeField = System.currentTimeMillis();
		analysisStateField.clear();
		if (mutability == Mutability.VARIABLE)
		{
			analysisStateField.put("*numberEvents*", new CaplValue(1d));
			analysisStateField.put("*now*", new CaplValue(lastAnalysisStateChangeField));
		}
	}
	
	HashMap<String, CaplValue> getAnalysisState()
	{
		return analysisStateField;
	}

	long getLastAnalysisChange()
	{
		return lastAnalysisStateChangeField;
	}
	
	boolean getIsNewState() 
	{
		return isNewState;
	}
	
	void markAsExistingState() 
	{
		isNewState = false;
	}
}
