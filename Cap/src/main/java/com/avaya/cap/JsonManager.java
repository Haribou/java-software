/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import static com.avaya.messaging.commons.utilities.StringUtils.isEmptyOrBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.avaya.messaging.commons.io.StackTraceLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class JsonManager
{
	private final static Logger LOG = Logger.getLogger(JsonManager.class);
	
	private final static CaplValue EMPTY_CAPL_VALUE = new CaplValue();
	
	public static ObjectNode createObjectNode() 
	{
		return JSON_GENERATOR.createObjectNode();
	}
	
	private final static ObjectMapper JSON_GENERATOR = LOG.isTraceEnabled() ? new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT, SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS) : new ObjectMapper();
	
	public static ArrayNode createArrayNode() 
	{
		return JSON_GENERATOR.createArrayNode();
	}
	
	public static ObjectNode generateResponseObject(String errorMessage, String infoMessage, String[] propertyNames, ObjectNode[] propertyValues)
	{
		final ObjectNode responseObject = JSON_GENERATOR.createObjectNode();
		
		int index;
		
		if (isEmptyOrBlank(errorMessage))
		{
			responseObject.put("success", true);
			if (!isEmptyOrBlank(infoMessage))
				responseObject.put("info", infoMessage);
			
			if (propertyNames == null && propertyValues == null)
				return responseObject;
			
			if (propertyNames == null || propertyValues == null)
			{
				LOG.error("generateResponseObject called with invalid arguments: propertyNames or propertyValues argument is null");
				return null;
			}
			if (propertyNames.length == 0 || propertyValues.length == 0)
			{
				LOG.error("generateResponseObject called with invalid arguments: propertyNames or propertyValues argument is empty");
				return null;
			}
			if (propertyNames.length != propertyValues.length)
			{
				LOG.error("generateResponseObject called with invalid arguments: propertyNames has a different length than propertyValues");
				return null;
			}
			
			for (index = 0; index < propertyNames.length; index++)
			{
				if (propertyValues[index].isTextual())
					responseObject.put(propertyNames[index], propertyValues[index].asText());
				else if (propertyValues[index].isBoolean())
					responseObject.put(propertyNames[index], propertyValues[index].asBoolean());
				else if (propertyValues[index].isInt())
					responseObject.put(propertyNames[index], propertyValues[index].asInt());
				else if (propertyValues[index].isDouble())
					responseObject.put(propertyNames[index], propertyValues[index].asDouble());
				else if (propertyValues[index].isObject())
					responseObject.set(propertyNames[index], propertyValues[index]);
				else if (propertyValues[index].isArray())
					responseObject.set(propertyNames[index], propertyValues[index]);
				else if (propertyValues[index] != null)
					responseObject.put(propertyNames[index], propertyValues[index].toString());
			}
		}
		else
		{
			responseObject.put("success", false);
			responseObject.put("error", errorMessage);
		}
		return responseObject;
	}
	
	public static CaplValue convertJsonToCaplValue(JsonElement jsonValueElement, Mutability mutability)
	{
	  	JsonPrimitive jsonPrimitive;

	  	JsonArray jsonArray;

	  	Map<String, CaplValue> mapValues;

	  	List<CaplValue> listValues;
	  	
	  	CaplValue caplValue;

	  	if (jsonValueElement == null || jsonValueElement.isJsonNull())
	  		return EMPTY_CAPL_VALUE;
	  	
	  	if (jsonValueElement.isJsonPrimitive())
	  	{
	  	  	jsonPrimitive = jsonValueElement.getAsJsonPrimitive(); 
			if (jsonPrimitive.isNumber())
				return new CaplValue(jsonPrimitive.getAsDouble());
			else if (jsonPrimitive.isString())
				return new CaplValue(jsonPrimitive.getAsString());
			else if (jsonPrimitive.isBoolean())
				return new CaplValue(jsonPrimitive.getAsBoolean());
			else
			{ 
	  			LOG.error("Invalid JSON value type (must be a NUMBER, STRING, or BOOLEAN) for JSON element " + jsonValueElement);
				return null;
			}
		}

		if (jsonValueElement.isJsonArray())
		{
			jsonArray = jsonValueElement.getAsJsonArray();
			listValues = new ArrayList<>(jsonArray.size());
	  	  	for (JsonElement oneArrayElement: jsonArray)
	  	  	{
	  	  		caplValue = convertJsonToCaplValue(oneArrayElement, mutability);
	  	  		if (caplValue == null)
	  	  			return null;
	  	  		if (caplValue.getValueDataType() != ValueDataType.NA)
	  	  			listValues.add(caplValue);
	  	  	}
			return new CaplValue(mutability, listValues, Integer.MAX_VALUE);
		}			

	  	mapValues = new HashMap<String, CaplValue>();
	    for (Entry<String, JsonElement> oneMapElement: jsonValueElement.getAsJsonObject().entrySet())
	    {
	    	caplValue = convertJsonToCaplValue(oneMapElement.getValue(), mutability);
	    	if (caplValue == null)
	    		return null;
	    	mapValues.put(oneMapElement.getKey(), caplValue);
	    }
		return new CaplValue(mutability, mapValues, Integer.MAX_VALUE);
	}
	
	public static CaplValue convertJsonToCaplValue(JsonNode jsonValueElement, Mutability mutability)
	{
	  	Map<String, CaplValue> mapValues;

	  	List<CaplValue> listValues;
	  	
	  	Iterator<Entry<String, JsonNode>> objectFields;
	  	
	  	Entry<String, JsonNode> objectField;
	  	
	  	CaplValue caplValue;
	  	
	  	int index;

	  	if (jsonValueElement == null || jsonValueElement.isNull())
	  		return EMPTY_CAPL_VALUE;
	  	
	  	if (jsonValueElement.isValueNode())
			if (jsonValueElement.isNumber())
				return new CaplValue(jsonValueElement.asDouble());
			else if (jsonValueElement.isTextual())
				return new CaplValue(jsonValueElement.textValue());
			else if (jsonValueElement.isBoolean())
				return new CaplValue(jsonValueElement.asBoolean());
			else
			{ 
	  			LOG.error("Invalid JSON value type (must be a NUMBER, STRING, or BOOLEAN) for JSON element " + jsonValueElement);
				return null;
			}

		if (jsonValueElement.isArray())
		{
			listValues = new ArrayList<>(jsonValueElement.size());
	  	  	for (index = 0; index < jsonValueElement.size(); index++)
	  	  	{
	  	  		caplValue = convertJsonToCaplValue(jsonValueElement.get(index), mutability);
	  	  		if (caplValue == null)
	  	  			return null;
	  	  		if (caplValue.getValueDataType() != ValueDataType.NA)
	  	  			listValues.add(caplValue);
	  	  	}
			return new CaplValue(mutability, listValues, Integer.MAX_VALUE);
		}			

	  	mapValues = new HashMap<String, CaplValue>();
	  	objectFields = jsonValueElement.fields();
	    while (objectFields.hasNext())
	    {
	    	objectField = objectFields.next();
	    	caplValue = convertJsonToCaplValue(objectField.getValue(), mutability);
	    	if (caplValue == null)
	    		return null;
	    	mapValues.put(objectField.getKey(), caplValue);
	    }
		return new CaplValue(mutability, mapValues, Integer.MAX_VALUE);
	}
	
	public static ObjectNode generateResponseObject(String infoMessage, String[] propertyNames, ObjectNode[] propertyValues)
	{
		return generateResponseObject(null, infoMessage, propertyNames, propertyValues);
	}
	
	public static ObjectNode generateResponseObject(String infoMessage, String propertyName, ObjectNode propertyValue)
	{
		return generateResponseObject(null, infoMessage, new String[] { propertyName }, new ObjectNode[] { propertyValue } );
	}
	
	public static ObjectNode generateResponseObject(String errorMessage)
	{
		return generateResponseObject(errorMessage, null, null, null);
	}
	
	public static ObjectNode caplValuesMapToObjectNode(Map<String, CaplValue> caplValues, Mutability mutabilityScope, boolean decorateIdentifiers)
	{
		ObjectNode resultNode;
		
		if (caplValues == null)
			return null;
		resultNode = JSON_GENERATOR.createObjectNode();
		if (new CaplValue(mutabilityScope, caplValues, Integer.MAX_VALUE).toObjectNode(resultNode, null, mutabilityScope, decorateIdentifiers))
			return resultNode;
		return null;
	}
	
	public static ObjectNode caplValuesMapToObjectNode(Map<String, CaplValue> caplValues, boolean decorateIdentifiers)
	{
		return caplValuesMapToObjectNode(caplValues, null, decorateIdentifiers);
	}
	
	public static ObjectNode caplValueToObjectNode(CaplValue caplValue, Mutability mutabilityScope, boolean decorateIdentifiers)
	{
		final ObjectNode resultNode;
		
		if (caplValue == null)
			return null;
		resultNode = JSON_GENERATOR.createObjectNode();
		if (caplValue.toObjectNode(resultNode, null, mutabilityScope, decorateIdentifiers))
			return resultNode;
		return null;
	}
	
	public static String jsonNodeToString(JsonNode jsonNode, boolean prettyPrint)
	{
		if (prettyPrint)
			try
			{
				return JSON_GENERATOR.writeValueAsString(JSON_GENERATOR.treeToValue(jsonNode, Object.class));
			} catch (Exception e)
			{
				StackTraceLogger.log("Unable to pretty-print JSON object", Level.ERROR, e, LOG);
				return null;
			}
		return jsonNode.toString();
	}
	
	public static String jsonNodeToString(JsonNode jsonNode)
	{
		return jsonNodeToString(jsonNode, LOG.isTraceEnabled());
	}
	
	public static JsonNode parseJson(String jsonValue)
	{
		try
		{
			return JSON_GENERATOR.readTree(jsonValue);
		} catch (Exception e)
		{
			StackTraceLogger.log("Unable to parse JSON object", Level.ERROR, e, LOG);
			return null;
		}
	}
}
