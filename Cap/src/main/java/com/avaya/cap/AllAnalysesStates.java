/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import java.util.HashMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import one.microstream.integrations.spring.boot.types.Storage;
import one.microstream.storage.types.StorageManager;

@Storage
public class AllAnalysesStates
{
	@Autowired
	private transient StorageManager storageManagerField;
	
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
				analysisStates[1].update(Mutability.VARIABLE);
			}
			else analysisStates[1] = new AnalysisState(Mutability.VARIABLE);
		}
		
		Logger LOG=Logger.getLogger(AllAnalysesStates.class);
		LOG.debug("RETRIEVE");
		LOG.debug("ANALYSIS: ");
		if(allAnalysisConstantsStates.containsKey(analysisId))
    	for(Entry<String, CaplValue> en:allAnalysisConstantsStates.get(analysisId).getAnalysisState().entrySet()) {LOG.debug(en.getKey()+"=>");LOG.debug(en.getValue().toString());}
    	LOG.debug("ENTITIES: ");
    	if (allAnalysisVariablesStates.containsKey(analysisId)&&allAnalysisVariablesStates.get(analysisId).containsKey(entityId))
    	for(Entry<String, CaplValue> en:allAnalysisVariablesStates.get(analysisId).get(entityId).getAnalysisState().entrySet()) {LOG.debug(en.getKey()+"=>");LOG.debug(en.getValue().toString());}
		
		return analysisStates;
	}

	void saveAnalysisState(String analysisId, String entityId, AnalysisState analysisConstantsState, AnalysisState analysisVariablesState)
	{
		Logger LOG=Logger.getLogger(AllAnalysesStates.class);
		HashMap<String, AnalysisState> existingVariablesStates;
		LOG.debug("SAVE");
		LOG.debug("ANALYSIS: ");
    	for(Entry<String, CaplValue> en:analysisConstantsState.getAnalysisState().entrySet()) {LOG.debug(en.getKey()+"=>");LOG.debug(en.getValue().toString());}
    	LOG.debug("ENTITIES: ");
    	for(Entry<String, CaplValue> en:analysisVariablesState.getAnalysisState().entrySet()) {LOG.debug(en.getKey()+"=>");LOG.debug(en.getValue().toString());}
    	
		if (analysisConstantsState.getIsNewState())
		{LOG.debug("STORING NEW ANALYSIS");
			analysisConstantsState.markAsExistingState();
			allAnalysisConstantsStates.put(analysisId, analysisConstantsState);
			//storageManagerField.store(allAnalysisConstantsStates);
		}
		
		if (analysisVariablesState.getIsNewState())
		{
			analysisVariablesState.markAsExistingState();
			existingVariablesStates = allAnalysisVariablesStates.get(analysisId);
			if (existingVariablesStates == null)
			{
				existingVariablesStates = new HashMap<>();
				allAnalysisVariablesStates.put(analysisId, existingVariablesStates);
				existingVariablesStates.put(entityId, analysisVariablesState);
				//storageManagerField.store(allAnalysisVariablesStates);LOG.debug("STORING ALL NEW ENTITIES");
			}
			else
			{
				existingVariablesStates.put(entityId, analysisVariablesState);
				//storageManagerField.store(existingVariablesStates);LOG.debug("STORING ONE NEW ENTITY");
			}
		}
		else { /*storageManagerField.store(analysisVariablesState);LOG.debug("STORING ONE EXISTING ENTITY");*/}
		
		/** REMOVE **/
    	storageManagerField.store(allAnalysisConstantsStates);
    	storageManagerField.store(allAnalysisVariablesStates);
    	/** **/
	}
	
	HashMap<String, AnalysisState> getAllAnalysisConstantsStates()
	{
		return allAnalysisConstantsStates;
	}
	
	HashMap<String, HashMap<String, AnalysisState>> getAllAnalysisVariablesStates()
	{
		return allAnalysisVariablesStates;
	}
}
	