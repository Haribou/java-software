/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import static com.avaya.messaging.commons.utilities.StringUtils.isEmptyOrBlank;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.avaya.cap.collections.CaplCollection;
import com.avaya.messaging.commons.io.FileUtilities;
import com.avaya.messaging.commons.io.StackTraceLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

class PersistentState
{
	private class StateRetrieval
	{
		private boolean newStateField;
		
		private Map<String, CaplValue> stateField;
		
		StateRetrieval(Map<String, CaplValue> state)
		{
			stateField = state;
			newStateField = false;
		}
		
		StateRetrieval(Mutability mutability)
		{
			stateField = new HashMap<>();
			
			if (mutability == Mutability.VARIABLE)
			{
				stateField.put("*numberEvents*", new CaplValue(1d));
				stateField.put("*now*", new CaplValue(System.currentTimeMillis()));
			}
			newStateField = true;
		}
		
		boolean isNewState()
		{
			return newStateField;
		}
		
		Map<String, CaplValue> getState()
		{
			return stateField;
		}
	}
	
	private class StateDataElementDescriptor
	{
		private String elementNameField = null;
		
		private CaplValue elementCaplValueField;
		
		StateDataElementDescriptor(String elementName, CaplValue elementCaplValue)
		{
			elementNameField = elementName;
			elementCaplValueField = elementCaplValue;
		}
		
		StateDataElementDescriptor(CaplValue elementCaplValue)
		{
			elementCaplValueField = elementCaplValue;
		}

		String getElementName()
		{
			return elementNameField;
		}

		CaplValue getElementCaplValue()
		{
			return elementCaplValueField;
		}
	}
	
	private static final Logger LOG = Logger.getLogger(PersistentState.class);
	
	private String stateDataDirectoryPathNameField;
	
	private StateDataElementDescriptor parseCaplValue(String elementName, JsonElement stateDataElement, Mutability mutability, long now)
	{
		String caplInnerStateKey,
			   actualElementName;
		
		String[] collectionNameElements;
		
		JsonElement caplInnerStateValue;
		
		JsonPrimitive caplStatePrimitiveValue;
		
		JsonArray caplStateArrayValue;
		
		StateDataElementDescriptor stateDataElementDescriptor;
		
		CaplValue collectionCaplValue;
		
		Set<CaplValue> setValues;
		
		Map<String, CaplValue> mapValues;
		
		List<CaplValue> listValues;
		
		int capacity = -1;
		
		long timeWindowStart = 0L,
			 timeWindowLength = 0L;
		
		boolean withinTimeWindow = true;
		
		try
    	{
    		if (stateDataElement.isJsonPrimitive()) 
			{
    			caplStatePrimitiveValue = stateDataElement.getAsJsonPrimitive();
    			if (elementName.equals("*numberEvents*"))
    				return new StateDataElementDescriptor(new CaplValue(caplStatePrimitiveValue.getAsDouble() + 1d));
				if (caplStatePrimitiveValue.isNumber())
					return new StateDataElementDescriptor(new CaplValue(mutability, caplStatePrimitiveValue.getAsDouble()));
				if (caplStatePrimitiveValue.isString())
					return new StateDataElementDescriptor(new CaplValue(mutability, caplStatePrimitiveValue.getAsString()));
				if (caplStatePrimitiveValue.isBoolean())
					return new StateDataElementDescriptor(new CaplValue(mutability, caplStatePrimitiveValue.getAsBoolean()));
				LOG.error("Entity data JSON object contains an invalid primitive property \"" + stateDataElement + "\" - the value must be a NUMBER, STRING, or BOOLEAN");
    			return null;
			}
    		
	    	if (stateDataElement.isJsonArray())
	    	{
				if (elementName.startsWith("set$") || elementName.startsWith("list$"))
				{
					collectionNameElements = elementName.split("\\$");
					if (collectionNameElements.length == 2)
						actualElementName = collectionNameElements[1];
					else
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list$<name>\"");
						return null;
					}
				}
				else if (elementName.startsWith("set*") || elementName.startsWith("list*"))
				{
					collectionNameElements = elementName.split("\\*");
					if (collectionNameElements.length == 3)
					{
						actualElementName = collectionNameElements[2];
						try
						{
							capacity = Integer.parseInt(collectionNameElements[1]); 
						} catch (Exception e)
						{
							LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list*<capacity>*<name>\"");
							return null;
						}
						if (capacity < 0)
						{
							LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list*<capacity>*<name>\" where capacity is a positive integer");
	        				return null;
						}
					}
					else
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list*<capacity>*<name>\"");
						return null;
					}
				}
				else if (elementName.startsWith("set%") || elementName.startsWith("list%"))
				{
					if (mutability == Mutability.CONSTANT)
					{
						LOG.error("Entity data JSON object for CONSTANT mutability scope may not contain time-windowed data structures");
						return null;
					}
					
					collectionNameElements = elementName.split("\\%");
					if (collectionNameElements.length == 4)
					{
						actualElementName = collectionNameElements[3];
						try
						{
							timeWindowStart = Long.parseLong(collectionNameElements[1]); 
						} catch (Exception e)
						{
							LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list%<timeWindowStart>%<timeWindowLength>%<name>\"");
							return null;
						}
						if (timeWindowStart < 0L)
						{
							LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list%<timeWindowStart>%<timeWindowLength>%<name>\" where timeWindowStart is a positive integer");
	        				return null;
						}
						try
						{
							timeWindowLength = Long.parseLong(collectionNameElements[2]); 
						} catch (Exception e)
						{
							LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list%<timeWindowStart>%<timeWindowLength>%<name>\"");
							return null;
						}
						if (timeWindowLength <= 0L)
						{
							LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list%<timeWindowStart>%<timeWindowLength>%<name>\" where timeWindowLength is a positive integer");
	        				return null;
						}
					}
					else
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list%<timeWindowStart>,<timeWindowLength>%<name>\"");
						return null;
					}
				}
				else
				{
					LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"set/list followed by either $<name> or *<capacity>*<name> or %<timeWindowStart>,<timeWindowLength>%<name>");
					return null;
				}
				
	    		caplStateArrayValue = stateDataElement.getAsJsonArray();
	    		if (elementName.startsWith("set"))
				{
	    			if (timeWindowStart > 0L)
	    			{
	    				setValues = new HashSet<>();
	    				withinTimeWindow = now < timeWindowStart + timeWindowLength;
						if (withinTimeWindow)
							collectionCaplValue = new CaplValue(setValues, timeWindowStart, timeWindowLength);
						else collectionCaplValue = new CaplValue(setValues, now, timeWindowLength);
	    			}
	    			else if (capacity < 0)
					{
						setValues = new HashSet<>();
						collectionCaplValue = new CaplValue(mutability, setValues, Integer.MAX_VALUE);
					}
					else
					{
						collectionCaplValue = CaplCollection.getSet(capacity);
						collectionCaplValue.setMutability(mutability);
						setValues = collectionCaplValue.getSetValue();
					}
	    			if (withinTimeWindow)
						for (JsonElement oneValue: caplStateArrayValue)
		    				if (!oneValue.isJsonNull())
		    				{
		    					stateDataElementDescriptor = parseCaplValue("", oneValue, mutability, now);
		    					if (stateDataElementDescriptor == null)
		            				return null;
		    					
		    					setValues.add(stateDataElementDescriptor.getElementCaplValue());
		    				}
					return new StateDataElementDescriptor(actualElementName, collectionCaplValue);
				}
				
	    		if (timeWindowStart > 0L)
    			{
    				listValues = new ArrayList<>();
    				withinTimeWindow = now < timeWindowStart + timeWindowLength;
					if (withinTimeWindow)
						collectionCaplValue = new CaplValue(listValues, timeWindowStart, timeWindowLength);
					else collectionCaplValue = new CaplValue(listValues, now, timeWindowLength);
    			}
    			else if (capacity < 0)
				{
					listValues = new ArrayList<>();
					collectionCaplValue = new CaplValue(mutability, listValues, Integer.MAX_VALUE);
				}
				else
				{
					collectionCaplValue = CaplCollection.getList(capacity);
					collectionCaplValue.setMutability(mutability);
					listValues = collectionCaplValue.getListValue();
				}
	    		if (withinTimeWindow)
					for (JsonElement oneValue: caplStateArrayValue)
	    				if (!oneValue.isJsonNull())
	    				{
	    					stateDataElementDescriptor = parseCaplValue("", oneValue, mutability, now);
	    					if (stateDataElementDescriptor == null)
	            				return null;
	    					listValues.add(stateDataElementDescriptor.getElementCaplValue());
	    				}
				return new StateDataElementDescriptor(actualElementName, collectionCaplValue);
			}
			
	    	if (elementName.startsWith("*"))
			{
				collectionNameElements = elementName.split("\\*");
				if (collectionNameElements.length == 3)
				{
					actualElementName = collectionNameElements[2];
					try
					{
						capacity = Integer.parseInt(collectionNameElements[1]); 
					} catch (Exception e)
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"*<capacity>*<name>\"");
						return null;
					}
					if (capacity < 0)
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"*<capacity>*<name>\" where capacity is a positive integer");
        				return null;
					}
				}
				else
				{
					LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"*<capacity>*<name>\"");
					return null;
				}
			}
			else if (elementName.startsWith("%"))
			{
				if (mutability == Mutability.CONSTANT)
				{
					LOG.error("Entity data JSON object for CONSTANT mutability scope may not contain time-windowed data structures");
					return null;
				}
				
				collectionNameElements = elementName.split("\\%");
				if (collectionNameElements.length == 4)
				{
					actualElementName = collectionNameElements[3];
					try
					{
						timeWindowStart = Long.parseLong(collectionNameElements[1]); 
					} catch (Exception e)
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"%<timeWindowStart>%<timeWindowLength>%<name>\"");
						return null;
					}
					if (timeWindowStart < 0L)
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"%<timeWindowStart>%<timeWindowLength>%<name>\" where timeWindowStart is a positive integer");
        				return null;
					}
					try
					{
						timeWindowLength = Long.parseLong(collectionNameElements[2]); 
					} catch (Exception e)
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"%<timeWindowStart>%<timeWindowLength>%<name>\"");
						return null;
					}
					if (timeWindowLength <= 0L)
					{
						LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"%<timeWindowStart>%<timeWindowLength>%<name>\" where timeWindowLength is a positive integer");
        				return null;
					}
				}
				else
				{
					LOG.error("Entity data JSON object contains an invalid property \"" + elementName + "\" - the property key must be \"%<timeWindowStart>%<timeWindowLength>%<name>\"");
					return null;
				}
			}
			else actualElementName = elementName;
			
	    	if (timeWindowStart > 0L)
	    	{
	    		mapValues = new HashMap<>();
				withinTimeWindow = now < timeWindowStart + timeWindowLength;
				if (withinTimeWindow)
					collectionCaplValue = new CaplValue(mapValues, timeWindowStart, timeWindowLength);
				else collectionCaplValue = new CaplValue(mapValues, now, timeWindowLength);
	    	}
	    	else if (capacity < 0)
			{
				mapValues = new HashMap<>();
				collectionCaplValue = new CaplValue(mutability, mapValues, Integer.MAX_VALUE);
			}
			else
			{
				collectionCaplValue = CaplCollection.getMap(capacity);
				collectionCaplValue.setMutability(mutability);
				mapValues = collectionCaplValue.getMapValue();
			}
	    	if (withinTimeWindow)
				for (Entry<String, JsonElement> oneValue: stateDataElement.getAsJsonObject().entrySet())
				{
					caplInnerStateKey = oneValue.getKey();
					caplInnerStateValue = oneValue.getValue();
					if (caplInnerStateValue.isJsonNull())
	    			{
						LOG.error("Entity data JSON object contains an invalid value for property \"" + caplInnerStateKey + "\" - value must not be empty");
	    				return null;
	    			}
					stateDataElementDescriptor = parseCaplValue(caplInnerStateKey, caplInnerStateValue, mutability, now);
					if (stateDataElementDescriptor == null)
						return null;
					if (stateDataElementDescriptor.getElementName() == null)
						mapValues.put(caplInnerStateKey, stateDataElementDescriptor.getElementCaplValue());
					else mapValues.put(stateDataElementDescriptor.getElementName(), stateDataElementDescriptor.getElementCaplValue());
				}
    		return new StateDataElementDescriptor(actualElementName, collectionCaplValue);
    	} catch (Exception e)
    	{
    		StackTraceLogger.log("Unable to parse entity data JSON object", Level.ERROR, e, LOG);
    		return null;
    	}
	}
	
	private StateRetrieval readStoredCaplValues(String fileName, Mutability mutability, long reinitializeIfOlderThan)
	{
		final String caplStateDataPathAndFileName = stateDataDirectoryPathNameField + fileName + ".json";
		
		String jsonStateDataFileContent;
		
    	Map<String, CaplValue> stateCaplValues;
		
		JsonElement stateDataElement;
		
		JsonObject stateDataObject;
		
		StateDataElementDescriptor stateDataElementDescriptor;
		
		File caplStateDataFile;
		
		final long now;
		
		try
    	{
			if (reinitializeIfOlderThan < 0L)
    		{
    			LOG.info("Re-initializing state data JSON file \"" + caplStateDataPathAndFileName + "\" because it was written before the identified analysis was modified or because the analysis request was made with \"resetState\" = true");
    			return new StateRetrieval(mutability);
    		}
			caplStateDataFile = new File(caplStateDataPathAndFileName);
			if (!caplStateDataFile.exists() || caplStateDataFile.lastModified() < reinitializeIfOlderThan)
    		{
    			LOG.info("Re-initializing state data JSON file \"" + caplStateDataPathAndFileName + "\" because it was written before the identified analysis was modified or because the analysis request was made with \"resetState\" = true");
    			return new StateRetrieval(mutability);
    		}
			jsonStateDataFileContent = FileUtilities.getFileContent(caplStateDataPathAndFileName);
    		if (isEmptyOrBlank(jsonStateDataFileContent))
    			return new StateRetrieval(mutability);
    		
    		stateDataElement = JsonParser.parseString(jsonStateDataFileContent);
    		stateDataObject = stateDataElement.getAsJsonObject();
    		if (stateDataObject.isJsonNull() && mutability == Mutability.VARIABLE)
    		{
    			stateCaplValues = new HashMap<>();
    			stateCaplValues.put("*numberEvents*", new CaplValue(Mutability.CONSTANT, 1d));
    			stateCaplValues.put("*now*", new CaplValue(System.currentTimeMillis()));
        		return new StateRetrieval(stateCaplValues);
    		}
    		
    		now = System.currentTimeMillis();
    		stateDataElementDescriptor = parseCaplValue("", stateDataObject, mutability, now);
    		if (stateDataElementDescriptor == null)
    			return null;
    		if (stateDataElementDescriptor.getElementCaplValue().getValueDataType() == ValueDataType.MAP)
    		{
    			stateCaplValues = stateDataElementDescriptor.getElementCaplValue().getMapValue();
    			if (mutability == Mutability.VARIABLE)
    				stateCaplValues.put("*now*", new CaplValue(now));
    			return new StateRetrieval(stateCaplValues);
    		}
    		LOG.error("Entity data file \"" +  caplStateDataPathAndFileName + "\" does not contain a valid JSON object");
			return null;
    	} catch (Exception e)
    	{
    		StackTraceLogger.log("Unable to parse entity data JSON object \"" +  caplStateDataPathAndFileName + "\"", Level.ERROR, e, LOG);
    		return null;
    	}
	}
	
	PersistentState(String stateDataDirectoryPathName)
	{
		stateDataDirectoryPathNameField = stateDataDirectoryPathName;
	}
	
	List<Map<String, CaplValue>> retrieveState(String caplAnalysisPathAndFileName, String analysisId, String entityId, boolean resetState)
	{
		StateRetrieval analysisStateRetrieval,
					   entityStateRetrieval;
		
		final long lastCaplAnalysisUpdate = resetState ? -1L : new File(caplAnalysisPathAndFileName).lastModified();
		LOG.debug("{{{"+lastCaplAnalysisUpdate);
		List<Map<String, CaplValue>> result;
		
		analysisStateRetrieval = readStoredCaplValues(analysisId + "-analysisState", Mutability.CONSTANT, lastCaplAnalysisUpdate);
      	if (analysisStateRetrieval == null)
        	return null;
      	if (analysisStateRetrieval.isNewState())
      		// Force recreation of the entity state as well.
      		entityStateRetrieval = readStoredCaplValues(analysisId + "-" + entityId + "-entityState", Mutability.VARIABLE, -1L);
      	else entityStateRetrieval = readStoredCaplValues(analysisId + "-" + entityId + "-entityState", Mutability.VARIABLE, lastCaplAnalysisUpdate);
        if (entityStateRetrieval == null)
	    	return null;
        result = new ArrayList<Map<String, CaplValue>>(2);
        
        result.add(analysisStateRetrieval.getState());
        result.add(entityStateRetrieval.getState());
        
        return result;
	}
	
	boolean saveState(String analysisId, String entityId, ObjectNode caplValues)
	{
		final JsonNode analysisState = caplValues.get("analysisState"),
					   entityState = caplValues.get("entityState");
		
		final String caplAnalysisStatePathAndFileName = stateDataDirectoryPathNameField + analysisId + "-analysisState.json",
					 caplEntityStatePathAndFileName = stateDataDirectoryPathNameField + analysisId + "-" + entityId + "-entityState.json";
	
		String caplValuesAsString;
		
		if (analysisState != null)
		{
			caplValuesAsString = JsonManager.jsonNodeToString(analysisState);
			if (caplValuesAsString != null)
				if (!FileUtilities.saveToFile(caplAnalysisStatePathAndFileName, false, caplValuesAsString))
				{
					LOG.error("Unable to save analysis state \"" + analysisState + "\" for CAP analysis ID \"" + analysisId + "\"");
					return false;
				}
		}
		if (entityState != null)
		{
			caplValuesAsString = JsonManager.jsonNodeToString(entityState);
			if (caplValuesAsString != null)
				if (!FileUtilities.saveToFile(caplEntityStatePathAndFileName, false, caplValuesAsString))
				{
					LOG.error("Unable to save entity state \"" + analysisState + "\" for CAP analysis ID \"" + analysisId + "\" and entity ID \"" + entityId + "\"");
					return false;
				}
		}
		return true;
	}
	
	static JsonObject parseJsonFile(String jsonFileName) 
	{
		String jsonFileContent = FileUtilities.getFileContent(jsonFileName);
		
		JsonElement jsonFileContentElement;
		
		if (isEmptyOrBlank(jsonFileContent))
			return null;
		
		try
		{
			jsonFileContentElement = JsonParser.parseString(jsonFileContent);
		} catch (Exception e)
		{
			LOG.warn("Unable to parse file \"" + jsonFileName + "\" into JSON object");
			return null;
		}
		
		if (jsonFileContentElement.isJsonObject())
			return jsonFileContentElement.getAsJsonObject();
		
		LOG.warn("File \"" + jsonFileName + "\" does not contain a valid JSON object");
		return null;
	}
}
