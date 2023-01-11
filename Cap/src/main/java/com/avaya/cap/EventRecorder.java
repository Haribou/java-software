/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.avaya.messaging.commons.io.FileUtilities;
import com.avaya.messaging.commons.io.StackTraceLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class EventRecorder
{
	private static final Logger LOG = Logger.getLogger(EventRecorder.class);
	
	private static final int MAX_NUMBER_VERSIONS = 1000;
	
	private static final Gson JSON_PRETTY_PRINTER = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
	
	private Hashtable<String, ArrayNode> objectBuffers = new Hashtable<>();
	
	private String directoryPathAndNameField;
	
	private long deleteAfterDaysField;
	
	private int eventBufferLengthField;
	
	private boolean prettyPrintField;
	
	private int inventoryDirectory(String analysisId)
	{
		final long deleteOlderThan = System.currentTimeMillis() - deleteAfterDaysField * 24L * 60L * 60000L;
	    
		Iterator<Path> filesInDirectory;
		
		File file;
		
		String versionNumberString;
		
		int versionNumber,
    		largestVersionNumber = 0;
		
		if (LOG.isTraceEnabled())
			LOG.trace("Inventoring content of directory \"" + directoryPathAndNameField + "\": purging old event files for CAP analysis with ID \"" + analysisId + "\" and determining the largest version number of event files for this analysis");
    
		try
		{
			filesInDirectory = Files.newDirectoryStream(Paths.get(directoryPathAndNameField), analysisId + "*.json").iterator();
			while (filesInDirectory.hasNext())
			{
				file = filesInDirectory.next().toFile();
				versionNumberString = file.getName();
				versionNumberString = versionNumberString.substring(versionNumberString.lastIndexOf('-') + 1, versionNumberString.length() - 5);
				try
            	{
            		versionNumber = Integer.parseInt(versionNumberString);
            		if (versionNumber > largestVersionNumber)
            			largestVersionNumber = versionNumber;
            		if (file.lastModified() < deleteOlderThan && !file.delete())
            			LOG.warn("Unable to delete old file \"" + file.getName() + "\" in directory \"" + directoryPathAndNameField + "\"");
            	} catch (NumberFormatException e)
            	{}
			}
			if (largestVersionNumber > MAX_NUMBER_VERSIONS)
			{
				file = new File(analysisId + "-1.json");
				if (file.exists() && !file.delete())
				{
	        		LOG.warn("Unable to delete old file \"" + file.getName() + "\" in directory \"" + directoryPathAndNameField + "\"");
	        		return -1;
				}
				return 0;
			}
			return largestVersionNumber % MAX_NUMBER_VERSIONS;
		} catch (Exception e)
		{
			StackTraceLogger.log("Unable to retrieve files in directory \"" + directoryPathAndNameField + "\"", Level.WARN, e, LOG);
			return -1;
		}
	}
	
	public EventRecorder(String directoryPathAndName, int deleteAfterDays, int eventBufferLength, boolean prettyPrint)
	{
		directoryPathAndNameField = directoryPathAndName;
		deleteAfterDaysField = deleteAfterDays;
		eventBufferLengthField = eventBufferLength;
		prettyPrintField = prettyPrint;
	}
	
	public boolean recordJsonObject(String analysisId, JsonNode payload)
	{
		ArrayNode objectBuffer;
		
		String newJsonFilePathAndName,
			   objectStringRepresentation;
		
		int largestVersionNumber;
		
		if (payload != null && !payload.isNull())
		{
			objectBuffer = objectBuffers.get(analysisId);
			if (objectBuffer == null)
			{
				objectBuffer = JsonManager.createArrayNode();
				objectBuffers.put(analysisId, objectBuffer);
			}
			objectBuffer.add(payload);
	
			if (LOG.isTraceEnabled())
				LOG.trace("Added new JSON object to existing JSON object buffer with current length " + objectBuffer.size());
			
			if (objectBuffer.size() > eventBufferLengthField)
			{
				if (LOG.isTraceEnabled())
					LOG.trace("Persisting JSON object buffer to file system...");
				largestVersionNumber = inventoryDirectory(analysisId);
				if (largestVersionNumber < 0)
				{
					LOG.warn("Unable to persist JSON object buffer to file system");
					return false;
				}
				newJsonFilePathAndName = directoryPathAndNameField + analysisId + "-" + (largestVersionNumber + 1) + ".json";
				if (prettyPrintField)
					objectStringRepresentation = JSON_PRETTY_PRINTER.toJson(objectBuffer);
				else objectStringRepresentation = objectBuffer.toString();
				if (FileUtilities.saveToFile(newJsonFilePathAndName, false, objectStringRepresentation))
				{
					if (LOG.isTraceEnabled())
						LOG.trace("Persisted JSON object buffer to file system as file \"" + newJsonFilePathAndName + "\"");
					objectBuffers.remove(analysisId);
					return true;
				}
				
				objectBuffers.remove(analysisId);
				LOG.warn("Unable to persist JSON object buffer to file system");
				return false;
			}
		}
		return true;
	}
}
