/**
 *
 * @author Reinhard Klemm, Avaya
 *
 */

package com.avaya.cap;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CaplValue
{
	private static final Logger LOG = Logger.getLogger(CaplValue.class);
																	
	private ValueDataType valueDataTypeField = ValueDataType.NA;
	
	private Mutability mutabilityField = Mutability.VARIABLE;
	
	private double numberValueField;
	
	private String stringValueField;
	
	private boolean booleanValueField;
	
	private Set<CaplValue> setValueField;
	
	private Map<String, CaplValue> mapValueField;
	
	private List<CaplValue> listValueField;
	
	private int capacityField = Integer.MAX_VALUE;
	
	private long timeWindowStartField,
				 timeWindowLengthField;
	
	private boolean toObjectNode(ArrayNode resultArrayNode, boolean decorateIdentifiers)
	{
		ObjectNode nestedObjectNode;
		
	  	ArrayNode nestedArrayNode;

		switch (valueDataTypeField)
		{
			case NUMBER: resultArrayNode.add(numberValueField); break;
			case STRING: resultArrayNode.add(stringValueField); break;
			case BOOLEAN: resultArrayNode.add(booleanValueField); break;
			case MAP: nestedObjectNode = resultArrayNode.addObject();
					  for (Entry<String, CaplValue> mapMember: mapValueField.entrySet())
						  if (!mapMember.getValue().toObjectNode(nestedObjectNode, mapMember.getKey(), null, decorateIdentifiers))
					  	  {
								LOG.error("Unable to convert MAP property \"" + mapMember.getKey() + "\" into ObjectNode");
								return false;
					  	  }
					  break;
			case LIST: nestedArrayNode = resultArrayNode.addArray();
				  	   for (CaplValue listMember: listValueField)
				  		   if (!listMember.toObjectNode(nestedArrayNode, decorateIdentifiers))
				  		   {
				  			   LOG.error("Unable to convert LIST property into ObjectNode");
				  			   return false;
				  		   }
				  	   break;
			default: // Value data type = NA => do nothing. Note: it cannot be SET because CAP-L does not allow adding SETs to SETs or LISTs and embedded JSON arrays in JSON payloads from external sources are interpreted as LISTs.	   
		}
		return true;
	}

	public CaplValue()
	{
		// This is the "neutral" or "empty" CaplValue/
		valueDataTypeField = ValueDataType.NA;
	}
	
	public CaplValue(Mutability mutability, double value)
	{
		mutabilityField = mutability;
		valueDataTypeField = ValueDataType.NUMBER; 
		numberValueField = value;
	}
	
	public CaplValue(double value)
	{
		valueDataTypeField = ValueDataType.NUMBER; 
		numberValueField = value;
	}
	
	public CaplValue(Mutability mutability, String value)
	{
		mutabilityField = mutability;
		valueDataTypeField = ValueDataType.STRING; 
		stringValueField = value;
	}
	
	public CaplValue(String value)
	{
		valueDataTypeField = ValueDataType.STRING; 
		stringValueField = value;
	}
	
	public CaplValue(Mutability mutability, boolean value)
	{
		mutabilityField = mutability;
		valueDataTypeField = ValueDataType.BOOLEAN; 
		booleanValueField = value;
	}
	
	public CaplValue(boolean value)
	{
		valueDataTypeField = ValueDataType.BOOLEAN; 
		booleanValueField = value;
	}
	
	public CaplValue(Mutability mutability, Set<CaplValue> value, int capacity)
	{
		mutabilityField = mutability;
		valueDataTypeField = ValueDataType.SET; 
		setValueField = value;
		capacityField = Math.max(capacity, value.size());
	}
	
	public CaplValue(Set<CaplValue> value, int capacity)
	{
		valueDataTypeField = ValueDataType.SET; 
		setValueField = value;
		capacityField = Math.max(capacity, value.size());
	}
	
	public CaplValue(Set<CaplValue> value, long now, long timeWindowLength)
	{
		valueDataTypeField = ValueDataType.SET; 
		setValueField = value;
		timeWindowStartField = now;
		timeWindowLengthField = timeWindowLength;
	}
	
	public CaplValue(Mutability mutability, Map<String, CaplValue> value, int capacity)
	{
		mutabilityField = mutability;
		valueDataTypeField = ValueDataType.MAP; 
		mapValueField = value;
		capacityField = Math.max(capacity, value.size());
	}
	
	public CaplValue(Map<String, CaplValue> value, int capacity)
	{
		valueDataTypeField = ValueDataType.MAP; 
		mapValueField = value;
		capacityField = Math.max(capacity, value.size());
	}
	
	public CaplValue(Map<String, CaplValue> value, long now, long timeWindowLength)
	{
		valueDataTypeField = ValueDataType.MAP; 
		mapValueField = value;
		timeWindowStartField = now;
		timeWindowLengthField = timeWindowLength;
	}
	
	public CaplValue(Mutability mutability, List<CaplValue> value, int capacity)
	{
		mutabilityField = mutability;
		valueDataTypeField = ValueDataType.LIST; 
		listValueField = value;
		capacityField = Math.max(capacity, value.size());
	}
	
	public CaplValue(List<CaplValue> value, int capacity)
	{
		valueDataTypeField = ValueDataType.LIST; 
		listValueField = value;
		capacityField = Math.max(capacity, value.size());
	}
	
	public CaplValue(List<CaplValue> value, long now, long timeWindowLength)
	{
		valueDataTypeField = ValueDataType.LIST; 
		listValueField = value;
		timeWindowStartField = now;
		timeWindowLengthField = timeWindowLength;
	}
	
	public ValueDataType getValueDataType()
	{
		return valueDataTypeField;
	}
	
	boolean hasCollectionValueDataType()
	{
		return valueDataTypeField == ValueDataType.SET || valueDataTypeField == ValueDataType.MAP || valueDataTypeField == ValueDataType.LIST;
	}
	
	public void setMutability(Mutability mutability)
	{
		mutabilityField = mutability;
		switch (valueDataTypeField)
		{
			case SET: for (CaplValue setMember: setValueField)
						setMember.setMutability(mutability);
					  return;
			case MAP: for (CaplValue mapMember: mapValueField.values())
						  mapMember.setMutability(mutability);
					  return;
			case LIST: for (CaplValue listMember: listValueField)
				  		   listMember.setMutability(mutability);
					   return;
			default: return;	   
		}
	}
	
	public Mutability getMutability() 
	{
		return mutabilityField;
	}
	
	public double getNumberValue()
	{
		return numberValueField;
	}
	
	public void modifyNumberValue(String operation, double value)
	{
		if (operation.equals("set"))
			numberValueField = value;
		else if (operation.equals("add"))
			numberValueField += value;
		else if (operation.equals("subtract"))
			numberValueField -= value;
		else if (operation.equals("mult"))
			numberValueField *= value;
		else if (operation.equals("div"))
			numberValueField /= value;
		else throw new IllegalArgumentException("\"" + operation + "\" is not a valid operation");
	}
	
	public String getStringValue()
	{
		return stringValueField;
	}
	
	public boolean getBooleanValue()
	{
		return booleanValueField;
	}
	
	public Set<CaplValue> getSetValue()
	{
		return setValueField;
	}
	
	public Map<String, CaplValue> getMapValue()
	{
		return mapValueField;
	}
	
	public List<CaplValue> getListValue()
	{
		return listValueField;
	}
	
	public int getCapacity()
	{
		return capacityField;
	}
	
	public long getTimeWindowStart()
	{
		return timeWindowStartField;
	}
	
	public void setTimeWindowStart(long now)
	{
		timeWindowStartField = now;
	}
	
	public long getTimeWindowLength()
	{
		return timeWindowLengthField;
	}
	
	public boolean isTimeWindowedStructure() 
	{
		return timeWindowStartField > 0L;
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		
		long temp;
		
		int result = prime + (booleanValueField ? 1231 : 1237);
		
		result = prime * result	+ ((listValueField == null) ? 0 : listValueField.hashCode());
		result = prime * result	+ ((mapValueField == null) ? 0 : mapValueField.hashCode());
		
		temp = Double.doubleToLongBits(numberValueField);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((setValueField == null) ? 0 : setValueField.hashCode());
		result = prime * result + ((stringValueField == null) ? 0 : stringValueField.hashCode());
		
		return prime * result + ((valueDataTypeField == null) ? 0 : valueDataTypeField.hashCode());
	}

	@Override
	public boolean equals(Object otherValue)
	{
		CaplValue otherCaplValue;
		
		if (otherValue == null)
			return false;
		if (otherValue instanceof CaplValue)
		{
			otherCaplValue = (CaplValue) otherValue;
			if (valueDataTypeField == otherCaplValue.getValueDataType())
				switch (otherCaplValue.getValueDataType())
				{
					case NUMBER: return numberValueField == otherCaplValue.getNumberValue();
					case STRING: return stringValueField.equals(otherCaplValue.getStringValue());
					case BOOLEAN: return booleanValueField == otherCaplValue.getBooleanValue();
					case SET: return setValueField.equals(otherCaplValue.getSetValue());
					case MAP: return mapValueField.equals(otherCaplValue.getMapValue());
					default: return listValueField.equals(otherCaplValue.getListValue());
				}
		}
		return false;
	}

	public boolean toObjectNode(ObjectNode resultObjectNode, String caplValueName, Mutability mutabilityScope, boolean decorateIdentifiers)
	{
		ObjectNode nestedObjectNode;
		
	  	ArrayNode nestedArrayNode;

		switch (valueDataTypeField)
		{
			case NUMBER: resultObjectNode.put(caplValueName, numberValueField); break;
			case STRING: resultObjectNode.put(caplValueName, stringValueField); break;
			case BOOLEAN: resultObjectNode.put(caplValueName, booleanValueField); break;
			case SET: if (decorateIdentifiers)
							if (timeWindowStartField > 0L)
								nestedArrayNode = resultObjectNode.putArray("set%" + timeWindowStartField + "%" + timeWindowLengthField + "%" + caplValueName);
							else if (capacityField < Integer.MAX_VALUE)
								nestedArrayNode = resultObjectNode.putArray("set*" + capacityField + "*" + caplValueName);
							else nestedArrayNode = resultObjectNode.putArray("set$" + caplValueName);
					  else nestedArrayNode = resultObjectNode.putArray(caplValueName);
				  	  for (CaplValue setMember: setValueField)
				  		  if (!setMember.toObjectNode(nestedArrayNode, decorateIdentifiers))
	  		    		  {
				  			  LOG.error("Unable to convert element of SET \"" + caplValueName + "\" into ObjectNode");
				  			  return false;
	  			  		  }
				  	  break;
			case MAP: if (caplValueName == null)
					  	nestedObjectNode = resultObjectNode;
					  else if (decorateIdentifiers)
						  		if (timeWindowStartField > 0L)
						  			nestedObjectNode = resultObjectNode.putObject("%" + timeWindowStartField + "%" + timeWindowLengthField + "%" + caplValueName);
						  		else if (capacityField < Integer.MAX_VALUE)
						  			nestedObjectNode = resultObjectNode.putObject("*" + capacityField + "*" + caplValueName);
						  		else nestedObjectNode = resultObjectNode.putObject(caplValueName);
					  	   else nestedObjectNode = resultObjectNode.putObject(caplValueName);
					  for (Entry<String, CaplValue> mapMember: mapValueField.entrySet())
						  if ((caplValueName != null || (mutabilityScope == null || mapMember.getValue().getMutability() == null || mapMember.getValue().getMutability() == mutabilityScope)) &&
						  	  !mapMember.getValue().toObjectNode(nestedObjectNode, mapMember.getKey(), null, decorateIdentifiers))
						  {
							  LOG.error("Unable to convert MAP property \"" + mapMember.getKey() + "\" into ObjectNode");
							  return false;
						  }
					  break;
			case LIST: if (decorateIdentifiers) 
							if (timeWindowStartField > 0L)
								nestedArrayNode = resultObjectNode.putArray("list%" + timeWindowStartField + "%" + timeWindowLengthField + "%" + caplValueName);
							else if (capacityField < Integer.MAX_VALUE)
								nestedArrayNode = resultObjectNode.putArray("list*" + capacityField + "*" + caplValueName);
							else nestedArrayNode = resultObjectNode.putArray("list$" + caplValueName);
					   else nestedArrayNode = resultObjectNode.putArray(caplValueName);
				  	   for (CaplValue listMember: listValueField)
				  		 if (!listMember.toObjectNode(nestedArrayNode, decorateIdentifiers))
  			  		   	 {
				  			 LOG.error("Unable to convert element of LIST \"" + caplValueName + "\" into ObjectNode");
				  			 return false;
  			  		   	 }
				  	   break;
			default: // Value data type = NA => do nothing.
		}
		return true;
	}
	
	@Override
	public String toString()
	{
		String collectionValues;
		
		if (valueDataTypeField == null || valueDataTypeField == ValueDataType.NA)
			return "";
		switch (valueDataTypeField)
		{
			case NUMBER: return numberValueField + "";
			case STRING: return "\"" + stringValueField + "\"";
			case BOOLEAN: return booleanValueField + "";
			case SET: collectionValues = "[ ";
					  if (setValueField == null || setValueField.size() == 0)
					 	return collectionValues + " ]";
					  for (CaplValue oneElement: setValueField)
					  {
						 if (collectionValues.length() > 2)
							 collectionValues += ", ";
						 collectionValues += oneElement.toString();
					  }
					  return collectionValues + " ]";
			case MAP: collectionValues = "{ ";
				  	  if (mapValueField == null || mapValueField.size() == 0)
				  		 return collectionValues + " }";
					  for (Entry<String, CaplValue> oneElement: mapValueField.entrySet())
					  {
						 if (collectionValues.length() > 2)
							 collectionValues += ", ";
						 collectionValues += "\"" + oneElement.getKey() + "\"";
						 collectionValues += ": ";
						 collectionValues += oneElement.getValue().toString();
					  }
					  return collectionValues + " }";
			default: collectionValues = "[ ";
					 if (listValueField == null || listValueField.size() == 0)
					 	return collectionValues + " ]";
					 for (CaplValue oneElement: listValueField)
					 {
						 if (collectionValues.length() > 2)
							 collectionValues += ", ";
						 collectionValues += oneElement.toString();
					 }
					 return collectionValues + " ]";
		}
	}
}
