/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;

import one.microstream.integrations.spring.boot.types.Storage;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@Storage
public class AllAnalysesStates
{
	@Autowired
	private transient EmbeddedStorageManager storageManagerField;
	
	private final HashMap<String, AnalysisState> allAnalysisConstantsStates = new HashMap<>();
	
	private final HashMap<String, HashMap<String, AnalysisState>> allAnalysisVariablesStates = new HashMap<>();
	
	AnalysisState[] makeAnalysisState(String analysisId, String entityId, long lastAnalysisChange)
	{
		AnalysisState[] analysisStates = new AnalysisState[2];
		
		HashMap<String, AnalysisState> existingVariablesStates;
		
		analysisStates[0] = allAnalysisConstantsStates.get(analysisId);
		
		if (analysisStates[0] == null)
		{
			analysisStates[0] = new AnalysisState(Mutability.CONSTANT);
			analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		}
		else if (lastAnalysisChange > analysisStates[0].getLastAnalysisChange())
		{
			analysisStates[0].reinitialize(Mutability.CONSTANT);
			existingVariablesStates = allAnalysisVariablesStates.get(analysisId);
			if (existingVariablesStates == null)
				analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
			else if (existingVariablesStates.containsKey(entityId))
			{
				analysisStates[1] = existingVariablesStates.get(entityId);
				analysisStates[1].reinitialize(Mutability.VARIABLE);
			}
			else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		}
		else
		{
			existingVariablesStates = allAnalysisVariablesStates.get(analysisId); 
			if (existingVariablesStates == null)
				analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
			else if (existingVariablesStates.containsKey(entityId))
			{
				analysisStates[1] = existingVariablesStates.get(entityId);
				analysisStates[1].getAnalysisState().put("*numberEvents*", new CaplValue(analysisStates[1].getAnalysisState().get("*numberEvents*").getNumberValue() + 1l));
			}
			else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		}
		
		return analysisStates;
	}

	void saveAnalysisState(String analysisId, String entityId, AnalysisState analysisConstantsState, AnalysisState analysisVariablesState)
	{
		HashMap<String, AnalysisState> existingVariablesStates;
		
		if (analysisConstantsState.getIsNewState())
		{
			analysisConstantsState.markAsExistingState();
			allAnalysisConstantsStates.put(analysisId, analysisConstantsState);
			storageManagerField.store(allAnalysisConstantsStates);
		}
		else storageManagerField.store(analysisConstantsState);
		
		if (analysisVariablesState.getIsNewState())
		{
			analysisVariablesState.markAsExistingState();
			existingVariablesStates = allAnalysisVariablesStates.get(analysisId);
			if (existingVariablesStates == null)
			{
				existingVariablesStates = new HashMap<>();
				allAnalysisVariablesStates.put(analysisId, existingVariablesStates);
				existingVariablesStates.put(entityId, analysisVariablesState);
				storageManagerField.store(allAnalysisVariablesStates);
			}
			else
			{
				existingVariablesStates.put(entityId, analysisVariablesState);
				storageManagerField.store(existingVariablesStates);
			}
		}
		else storageManagerField.store(analysisVariablesState);
	}
	
	HashMap<String, AnalysisState> getAllAnalysisConstantsStates()
	{
		return allAnalysisConstantsStates;
	}
	
	HashMap<String, HashMap<String, AnalysisState>> getAllAnalysisVariablesStates()
	{
		return getAllAnalysisVariablesStates();
	}
}
	