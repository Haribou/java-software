/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import java.util.HashMap;

public class AnalysisState
{
	private final HashMap<String, CaplValue> analysisStateField = new HashMap<>();
	
	private long lastAnalysisStateChangeField;
	
	private boolean isNewState = true;
	
	public AnalysisState()
	{}
	
	AnalysisState(Mutability mutability)
	{
		lastAnalysisStateChangeField = System.currentTimeMillis();
		if (mutability == Mutability.VARIABLE)
		{
			analysisStateField.put("*numberEvents*", new CaplValue(1d));
			analysisStateField.put("*now*", new CaplValue(lastAnalysisStateChangeField));
		}
	}
	
	void reinitialize(Mutability mutability)
	{
		analysisStateField.clear();
		lastAnalysisStateChangeField = System.currentTimeMillis();
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
