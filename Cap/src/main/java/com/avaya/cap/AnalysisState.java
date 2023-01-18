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
	
	private boolean isNewState;
	
	public AnalysisState()
	{}
	
	AnalysisState(Mutability mutability)
	{
		isNewState = true;
		lastAnalysisStateChangeField = System.currentTimeMillis();
		if (mutability == Mutability.VARIABLE)
		{
			analysisStateField.put("*numberEvents*", new CaplValue(1d));
			analysisStateField.put("*now*", new CaplValue(lastAnalysisStateChangeField));
		}
	}
	
	void reinitialize(Mutability mutability)
	{
		isNewState = true;
		analysisStateField.clear();
		lastAnalysisStateChangeField = System.currentTimeMillis();
		if (mutability == Mutability.VARIABLE)
		{
			analysisStateField.put("*numberEvents*", new CaplValue(1d));
			analysisStateField.put("*now*", new CaplValue(lastAnalysisStateChangeField));
		}
	}
	
	void update(Mutability mutability)
	{
		lastAnalysisStateChangeField = System.currentTimeMillis();
		if (mutability == Mutability.VARIABLE)
		{
			analysisStateField.get("*numberEvents*").modifyNumberValue("add", 1l);
			analysisStateField.get("*now*").modifyNumberValue("set", lastAnalysisStateChangeField);
		}
	}
	
	HashMap<String, CaplValue> getAnalysisState()
	{
		return analysisStateField;
	}
	
	void setLastAnalysisChange(long timestamp)
	{
		lastAnalysisStateChangeField = timestamp;
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
