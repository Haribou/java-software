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
class AllAnalysesStates
{
	@Autowired
	private transient EmbeddedStorageManager storageManagerField;
	
	private final HashMap<String, AnalysisState> allAnalysisConstantsStates = new HashMap<>();
	
	private final HashMap<String, HashMap<String, AnalysisState>> allAnalysisVariablesStates = new HashMap<>();
	
	AnalysisState[] makeAnalysisState(String analysisId, String entityId, long lastAnalysisChange)
	{
		AnalysisState[] analysisStates = new AnalysisState[2];
		
		analysisStates[0] = allAnalysisConstantsStates.get(analysisId);
		
		if (analysisStates[0] == null)
		{
			analysisStates[0] = new AnalysisState(Mutability.CONSTANT);
			analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		}
		else if (lastAnalysisChange > analysisStates[0].getLastAnalysisChange())
		{
			analysisStates[0].reinitialize(Mutability.CONSTANT);
			if (allAnalysisVariablesStates.containsKey(analysisId))
				if (allAnalysisVariablesStates.get(analysisId).containsKey(entityId))
				{
					analysisStates[1] = allAnalysisVariablesStates.get(analysisId).get(entityId);
					analysisStates[1].reinitialize(Mutability.VARIABLE);
				}
				else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
			else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		}
		else if (allAnalysisVariablesStates.containsKey(analysisId))
			if (allAnalysisVariablesStates.get(analysisId).containsKey(entityId))
				analysisStates[1] = allAnalysisVariablesStates.get(analysisId).get(entityId);
			else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		
		return analysisStates;
	}

	void saveAnalysisState(String analysisId, String entityId, AnalysisState analysisConstantsState, AnalysisState analysisVariablesState)
	{
		HashMap<String, AnalysisState> entityVariablesState;
		
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
			if (allAnalysisVariablesStates.containsKey(analysisId))
			{
				allAnalysisVariablesStates.get(analysisId).put(entityId, analysisVariablesState);
				storageManagerField.store(allAnalysisVariablesStates.get(analysisId));
			}
			else
			{
				entityVariablesState = new HashMap<>();
				allAnalysisVariablesStates.put(analysisId, entityVariablesState);
				entityVariablesState.put(entityId, analysisVariablesState);
				storageManagerField.store(allAnalysisVariablesStates);
			}
		}
		else storageManagerField.store(analysisVariablesState);
	}
}
