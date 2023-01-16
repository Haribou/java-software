/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */

package com.avaya.cap;

import static com.avaya.messaging.commons.utilities.StringUtils.isEmpty;
import static com.avaya.messaging.commons.utilities.StringUtils.isEmptyOrBlank;

import java.io.File;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.avaya.messaging.commons.io.ConsoleOutputter;
import com.avaya.messaging.commons.io.FileUtilities;
import com.avaya.messaging.commons.io.MultiLogger;
import com.avaya.messaging.commons.io.StackTraceLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;

import one.microstream.concurrency.XThreads;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@SpringBootApplication
@RestController
@Configuration
public class CapApiServer implements CommandLineRunner 
{
	private static final Logger LOG = Logger.getLogger(CapApiServer.class),
								SECURITY_LOG = Logger.getLogger("com.avaya.cap.Security");

	private final static MultiLogger MULTI_LOGGER = new MultiLogger(LOG, SECURITY_LOG);
	
	private final static Map<String, Object> ENTITIES_SEMAPHORE = new HashMap<>();
	
	private static AllAnalysesStates allAnalysesStatesField;
	
	private EmbeddedStorageManager storageManagerField; 
	
	@Value("${consoleOutput:true}")
	private boolean consoleOutput;
	
	@Value("${authentication.clientId}")
	private String accessClientId;
	
	@Value("${authentication.password}")
	private String accessPassword;
	
	@Value("${analysisDirectory:./analyses}")
	private String analysisDirectory;
	
	@Value("${stateDataDirectory:./stateData}")
	private String stateDataDirectory;

	@Value("${eventFileDirectory:./eventData}")
	private String eventFileDirectory;
	
	@Value("${eventBufferLength:100}")
	private int eventBufferLength;
	
	@Value("${deleteEventFilesAfterDays:90}")
	private int deleteEventFilesAfterDays;
	
	@Value("${prettyPrintLoggedJsonEvents:false}")
	private boolean prettyPrintLoggedJsonEvents;
	
	@Value("${trustStorePathAndName:}")
	private String trustStorePathAndName;
	
	@Value("${trustStorePassword:}")
	private String trustStorePassword;

	@Value("${proxyOn:false}")
	private boolean proxyOn;
	
	@Value("${proxyHostName:}")
	private String proxyHostName;
	
	@Value("${proxyPort:-1}")
	private int proxyPort;
	
	@Value("${stateDataPurgeIntervalMins:720}")
	private int stateDataPurgeIntervalMinutes;
	
	@Value("${one.microstream.use:true}")
	private boolean useMicroStream;
	
	private String cachedAnalysisId,
				   cachedEntityId,
				   cachedAnalysisPathAndFileName,
				   cachedCapAnalysis;
	
	private boolean terminated = false;
	
	private class AnalysisExecutor implements Supplier<ObjectNode>
	{
		private String entityIdField,
					   analysisIdField,
					   variableSubstitutionsField;
		
		private JsonNode eventField;
		
		private boolean useCachedAnalysisStateField, 
						resetStateField;
		
		AnalysisExecutor(String entityId, String analysisId, JsonNode event, String variableSubstitutions, boolean useCachedAnalysisState, boolean resetState)
		{
			entityIdField = entityId;
			analysisIdField = analysisId;
			eventField = event;
			variableSubstitutionsField = variableSubstitutions;
			useCachedAnalysisStateField = useCachedAnalysisState;
			resetStateField = resetState;
		}

		@Override
		public ObjectNode get()
		{
			return executeCapAnalysis(entityIdField, analysisIdField, eventField, variableSubstitutionsField, useCachedAnalysisStateField, resetStateField);
		}
	}
	
	private class StatePurger extends Thread
	{
		private class Purger implements Runnable
		{
			@Override
			public void run()
			{
				final long now = System.currentTimeMillis();
				
				double entitiesExpireAt;
				
				int removedAnalyses = 0,
					removedEntities = 0;
				
				Entry<String, AnalysisState> oneAnalysisState;
				
				Iterator<Entry<String, AnalysisState>> analysisStates;
				
				Iterator<AnalysisState> entityStates;
				
				HashMap<String, AnalysisState> entityStatesForOneAnalysis;
				
				CaplValue expirationCaplValue;
				
				analysisStates = allAnalysesStatesField.getAllAnalysisConstantsStates().entrySet().iterator();
				while (analysisStates.hasNext())
				{
					oneAnalysisState = analysisStates.next();
					expirationCaplValue = oneAnalysisState.getValue().getAnalysisState().get("*expires*");
					if (expirationCaplValue != null)
					{
						entitiesExpireAt = now - expirationCaplValue.getNumberValue();
						entityStatesForOneAnalysis = allAnalysesStatesField.getAllAnalysisVariablesStates().get(oneAnalysisState.getKey());
						entityStates = entityStatesForOneAnalysis.values().iterator();
						while (entityStates.hasNext())
							if (entityStates.next().getLastAnalysisChange() < entitiesExpireAt)
							{
								entityStates.remove();
								removedEntities++;
							}
						if (entityStatesForOneAnalysis.size() == 0)
						{
							analysisStates.remove();
							removedAnalyses++;
						}
					}
				}
				if (removedAnalyses + removedEntities > 0)
					storageManagerField.store(allAnalysesStatesField);
				
				LOG.info(removedAnalyses + " analysis state(s) removed, " + removedEntities + " entity state(s) removed");
			}
			
			void purge()
			{
				final Path stateDataDirectoryPath = FileSystems.getDefault().getPath(stateDataDirectory);
				
				Path analysisStateFilePath,
					 entityStateFilePath;
				
				Iterator<Path> analysisStateFiles,
							   entityStateFiles;
				
				int removedAnalyses = 0,
					removedEntities = 0;
				
				final long now = System.currentTimeMillis();
				
				long expires;
				
				String analysisStateFileName = null;
				
				File analysisStateFile,
					 entityStateFile;
				
				JsonObject analysisStateFileObject;
				
				boolean hasEntityStateFiles;
				
			    try
			    {
			    	analysisStateFiles = Files.newDirectoryStream(stateDataDirectoryPath, "*-analysisState.json").iterator();
			    	while (analysisStateFiles.hasNext())
			    		try
				    	{
			    			analysisStateFilePath = analysisStateFiles.next();
			    			analysisStateFileName = analysisStateFilePath.getFileName().toString();
				    		analysisStateFileObject = PersistentState.parseJsonFile(stateDataDirectory + analysisStateFileName);
				    		if (analysisStateFileObject.has("*expires*"))
				    		{
				    			expires = ((long) analysisStateFileObject.get("*expires*").getAsDouble());
				    			hasEntityStateFiles = false;
				    			if (expires > 0L)
				    			{
				    				expires = now - expires;
					    			entityStateFiles = Files.newDirectoryStream(stateDataDirectoryPath, analysisStateFileName.substring(0, analysisStateFileName.length() - "-analysisState.json".length()) + "-*-entityState.json").iterator();
				    				while (entityStateFiles.hasNext())
				    					try
					    				{
				    						entityStateFilePath = entityStateFiles.next();
				    						entityStateFile = entityStateFilePath.toFile();
				    						if (expires > entityStateFile.lastModified())
							    			{
				    							if (entityStateFile.delete())
						    					{
						    						if (LOG.isTraceEnabled())
						    							LOG.trace("Deleted expired entity state file \"" + entityStateFilePath.getFileName().toString() + "\"");
						    						removedEntities++;
						    					}
						    					else LOG.warn("Unable to delete expired entity state file \"" + entityStateFilePath.getFileName().toString() + "\"");
							    			}
				    						else hasEntityStateFiles = true;
					    				} catch (Exception e)
				    					{
					    					StackTraceLogger.log("Unable to delete expired entity state file", Level.WARN, e, LOG);
				    					}
					    			
				    				analysisStateFile = analysisStateFilePath.toFile();
					    			if (!hasEntityStateFiles && expires > analysisStateFile.lastModified())
					    			{
					    				if (analysisStateFile.delete())
					    				{
					    					if (LOG.isTraceEnabled())
					    						LOG.trace("Deleted expired analysis state file \"" + analysisStateFilePath.getFileName().toString() + "\"");
					    					removedAnalyses++;
					    				}
					    				else LOG.trace("Unable to delete expired analysis state file \"" + analysisStateFilePath.getFileName().toString() + "\"");
					    			}
				    			}
				    		}
				    	} catch (Exception e)
				    	{
				    		StackTraceLogger.log("Analysis state file \"" + analysisStateFileName + "\" does not contain a JSON object", Level.WARN, e, LOG);
				    	}

					LOG.info(removedAnalyses + " analysis state(s) removed, " + removedEntities + " entity state(s) removed");
			    } catch (Exception e)
			    {
			    	StackTraceLogger.log("Unable to read analysis state files in directory \"" + stateDataDirectory + "\"", Level.ERROR, e, LOG);
			    }
			}
		}
		
		@Override
		public void run()
		{
			final long sleepTime = stateDataPurgeIntervalMinutes * 60000L;
			
			final Purger purger = new Purger();
			
			LOG.info("Starting state purger thread...");
			
			while (true)
				try
				{
					synchronized(ENTITIES_SEMAPHORE)
					{
						ENTITIES_SEMAPHORE.wait(sleepTime);
						if (terminated)
						{
							LOG.info("Shutting down state purger thread");
							return;
						}
						if (LOG.isTraceEnabled())
							LOG.trace("Running state purger thread");
						if (useMicroStream)
							XThreads.executeSynchronized(purger);
						else purger.purge();
					}
				} catch (InterruptedException e)
				{}
		}
	}
	
	private List<Map<String, CaplValue>> cachedState;
	
	private void logConfigurationParameter(String parameterName, Object parameterValue, Object defaultValue)
	{
		LOG.info("Discovered property name \"" + parameterName + "\", value is \"" + parameterValue + "\", default value is \"" + defaultValue + "\"");
	}
	
	private String instantiateAnalysis(String capAnalysis, String variableSubstitutionsPairs)
	{
		final Set<String> variableNames;
		
		String[] variableSubstitutionsArray,
				 onevariableSubstitutionPair;
		
		if (isEmptyOrBlank(variableSubstitutionsPairs))
			return capAnalysis;
		
		variableNames = new HashSet<>();
		
		variableSubstitutionsArray = variableSubstitutionsPairs.split(",");
		if (variableSubstitutionsArray == null || variableSubstitutionsArray.length == 0)
		{
			LOG.error("The \"variableSubstitutions\" = \"" + variableSubstitutionsPairs + "\" parameter is invalid - it must be a comma-separated list of <variable name>:<variable value> tokens");
			return null;
		}
		for (String variableSubstitutionsPair: variableSubstitutionsArray)
		{
			onevariableSubstitutionPair = variableSubstitutionsPair.split(":");
			if (onevariableSubstitutionPair == null || onevariableSubstitutionPair.length < 2)
			{
				LOG.error("The \"variableSubstitutions\" = \"" + variableSubstitutionsPairs + "\" parameter is invalid - it must be a comma-separated list of <variable name>:<variable value> tokens");
				return null;
			}
			if (variableNames.contains(onevariableSubstitutionPair[0]))
			{
				LOG.error("The \"variableSubstitutions\" = \"" + variableSubstitutionsPairs + "\" parameter is invalid - duplicate variable name \"" + onevariableSubstitutionPair[0] + "\"");
				return null;
			}
			variableNames.add(onevariableSubstitutionPair[0]);
			capAnalysis = capAnalysis.replace("_@"+ onevariableSubstitutionPair[0] + "@_", onevariableSubstitutionPair[1]);
		}
		return capAnalysis;
	}
	
	private ObjectNode checkCredentials(String authentication)
	{
		String[] authenticationComponents;
		
		String clientId,
			   password;
		
		if (isEmpty(authentication))
		{
			MULTI_LOGGER.warn("Unauthorized request with empty authentication header");
			return JsonManager.generateResponseObject("Please provide an authentication header");
		}
		
		authenticationComponents = authentication.split("\\+");
		if (authenticationComponents == null || authenticationComponents.length < 2)
		{
			MULTI_LOGGER.warn("Unauthorized request with authentication header that does not contain a clientId and password in the format <clientId>+<password>");
			return JsonManager.generateResponseObject("Please provide a valid authentication header");
		}
		clientId = authenticationComponents[0];
		password = authenticationComponents[1];
		
		if (!accessClientId.equals(clientId) || !accessPassword.equals(password))
		{
			MULTI_LOGGER.warn("Unauthorized request with clientId = \"" + clientId + "\" and password \"" + password + "\" - for request details, please see the Spring log");
			return JsonManager.generateResponseObject("You are not authorized to access CAP");
		}	
		
		return null;
	}
	
	private ObjectNode runCaplAnalysis(String entityId, String analysisId, JsonNode event, String variableSubstitutions, boolean useCachedAnalysisState, boolean resetState)
	{
		String capAnalysisPathAndFileName,
			   capAnalysis;
		
		File capAnalysisFile;
		
		StringReader capAnalysisReader;
		
		CaplInterpreter caplInterpreter;
		
		ObjectNode analysisResults;
		
		PersistentState persistentState = null;
		
		AnalysisState[] analysisStateData = null;
		
		CaplValue numberEvents;
		
		List<Map<String, CaplValue>> retrievedState = null;
	
		if (resetState)
		{
			cachedEntityId = null;
			cachedAnalysisId = null;
			cachedCapAnalysis = null;
			cachedAnalysisPathAndFileName = null;
			cachedState = null;
			
			if (LOG.isTraceEnabled())
				LOG.trace("Resetting cached and persistent state");
		}
		
		if (useCachedAnalysisState && cachedEntityId != null && cachedAnalysisId != null && cachedCapAnalysis != null && cachedState != null)
			if (cachedEntityId.equals(entityId) && cachedAnalysisId.equals(analysisId))
			{
				capAnalysis = cachedCapAnalysis;
				retrievedState = cachedState;
				numberEvents = retrievedState.get(1).get("*numberEvents*");
				if (numberEvents == null)
					return JsonManager.generateResponseObject("The cached analysis state does not contain the required *numberEvents* CAPL value");
				retrievedState.get(1).put("*numberEvents*", new CaplValue(numberEvents.getNumberValue() + 1d));
				capAnalysisPathAndFileName = cachedAnalysisPathAndFileName;
				
				if (LOG.isTraceEnabled())
					LOG.trace("Using cached analysis state");
			}
			else return JsonManager.generateResponseObject("The cached analysis was generated for a different entity / analysis ID combination and is thus invalid: " +
													  	   "cached entity ID = \"" + cachedEntityId + "\", new entity ID = \"" + entityId + "\", " +
													  	   "cached analysis ID = \"" + cachedAnalysisId + "\", new analysis ID = \"" + analysisId + "\"");
		else
		{
    		if (isEmpty(analysisId))
	    	{
	    		LOG.error("The \"analysisId\" must not be empty");
	    		return JsonManager.generateResponseObject("The analysisId must not be empty");
	    	}
	    	capAnalysisPathAndFileName = analysisDirectory + analysisId + ".capl";
	    	
	    	capAnalysisFile = new File(capAnalysisPathAndFileName);
			if (!capAnalysisFile.exists())
	    	{
	    		LOG.error("\"" + capAnalysisPathAndFileName + "\" does not match any CAP analysis in directory \"" + analysisDirectory + "\"");
	    		return JsonManager.generateResponseObject("The \"analysisId\" (\"" + analysisId + "\") is invalid");
	    	}
	    	capAnalysis = FileUtilities.getFileContent(capAnalysisPathAndFileName);
	    	if (isEmpty(capAnalysis))
	    	{
	    		LOG.error("Specified CAP analysis script \"" + capAnalysisPathAndFileName + "\" is empty");
	    		return JsonManager.generateResponseObject("The CAP analysis with \"analysisId\" \"" + analysisId + "\" is invalid");
	    	}
	    	capAnalysis = instantiateAnalysis(capAnalysis, variableSubstitutions);
	    	if (capAnalysis == null)
				return JsonManager.generateResponseObject("Unable to instantiate the CAP analysis with the given \"variableSubstitutions\" parameters");
	    	
	    	if (LOG.isTraceEnabled())
	    		LOG.trace("Retrieving state for CAP analysis \"" + capAnalysisPathAndFileName + "\" for entity \"" + entityId + "\"");
	    	
	    	if (useMicroStream)
	    		analysisStateData = allAnalysesStatesField.makeAnalysisState(analysisId, entityId, capAnalysisFile.lastModified());
	    	else
	    	{
	    		persistentState = new PersistentState(stateDataDirectory);
		    	retrievedState = persistentState.retrieveState(capAnalysisPathAndFileName, analysisId, entityId, resetState);
	    		if (retrievedState == null)
	    			return JsonManager.generateResponseObject("Unable to read stored entity state of CAP analysis with \"analysisId\" \"" + analysisId + "\" for entity \"" + entityId + "\"");
	    	}
		}
		
		if (LOG.isTraceEnabled())
    		LOG.trace("Parsing and interpreting CAP analysis \"" + capAnalysisPathAndFileName + "\" for entity \"" + entityId + "\"");
    	try
    	{
    		capAnalysisReader = new StringReader(capAnalysis);
    	} catch (Exception e)
    	{
    	  	StackTraceLogger.log("Unable to process specified CAP analysis", Level.ERROR, e, LOG);
    	  	return JsonManager.generateResponseObject("Unable to read the CAP analysis with \"analysisId\" \"" + analysisId + "\"");
    	}
    	
    	caplInterpreter = new CaplInterpreter(capAnalysisReader);
    	
    	if (useMicroStream)
    	{
    		analysisResults = caplInterpreter.analyzeEvent(analysisId, analysisStateData[0].getAnalysisState(), analysisStateData[1].getAnalysisState(), event);
    		allAnalysesStatesField.saveAnalysisState(analysisId, entityId, analysisStateData[0], analysisStateData[1]);
    	}
    	else analysisResults = caplInterpreter.analyzeEvent(analysisId, retrievedState.get(0), retrievedState.get(1), event);
		if (!analysisResults.get("success").asBoolean())
			return JsonManager.generateResponseObject("Evaluation of CAP analysis with \"analysisId\" \"" + analysisId + "\" for entity \"" + entityId + "\" failed");
		
		if (!useMicroStream)
		{
			if (useCachedAnalysisState)
			{
				cachedEntityId = entityId;
				cachedAnalysisId = analysisId;
				cachedCapAnalysis = capAnalysis;
				cachedAnalysisPathAndFileName = capAnalysisPathAndFileName;
				cachedState = retrievedState;
			}
			else if (!persistentState.saveState(analysisId, entityId, analysisResults))
				return JsonManager.generateResponseObject("Unable to save state of CAP analysis with \"analysisId\" \"" + analysisId + "\" for entity \"" + entityId + "\"");
		}
			
		analysisResults.remove("analysisState");
		analysisResults.remove("entityState");
		
		return analysisResults;
	}
	
	private ObjectNode executeCapAnalysis(String entityId, String analysisId, JsonNode event, String variableSubstitutions, boolean useCachedAnalysisState, boolean resetState) 
	{
    	Object synchronizerForEntity;

    	if (useMicroStream)
    		return runCaplAnalysis(entityId, analysisId, event, variableSubstitutions, useCachedAnalysisState, resetState);
    	
		synchronized (ENTITIES_SEMAPHORE)
    	{
    		synchronizerForEntity = ENTITIES_SEMAPHORE.get(entityId);
    		if (synchronizerForEntity == null)
    		{
    			synchronizerForEntity = new Object();
    			ENTITIES_SEMAPHORE.put(entityId, synchronizerForEntity);
    		}
    	}
    	
    	synchronized (synchronizerForEntity)
    	{
    		return runCaplAnalysis(entityId, analysisId, event, variableSubstitutions, useCachedAnalysisState, resetState);
    	}
	}
	
	private ObjectNode analyzeEventObject(String analysisPipelines,
							   	   		  boolean useCachedAnalysisState,
							   	   		  boolean resetState,
							   	   		  String variableSubstitutions,
							   	   		  String event,
							   	   		  String authentication)
	{
		long then = System.currentTimeMillis();
		
		ObjectNode resultObject,
				   returnObject;
		
		JsonNode eventObject,
				 outputObject;
		
		ArrayNode returnArray;
		
		final ObjectNode credentialsCheck = checkCredentials(authentication);
		
		StringReader analysisPipelineReader;
		
		AnalysisPipelineInterpreter analysisPipelineInterpreter;
		
		List<AnalysisPipeline> analysisPipelinesList;
		
		List<String> entityIds,
					 analysisIds;
		
		int index = 0;
		
		if (credentialsCheck != null)
			return credentialsCheck;
		
		if (consoleOutput)
			ConsoleOutputter.print(true, "Received /analysis request");

		try
		{
			try
	    	{
				analysisPipelineReader = new StringReader(analysisPipelines);
	    	} catch (Exception e)
	    	{
	    	  	StackTraceLogger.log("Unable to process specified CAP analysis pipelines \"" + analysisPipelines + "\"", Level.ERROR, e, LOG);
	    	  	return JsonManager.generateResponseObject("Unable to process specified CAP analysis pipelines \"" + analysisPipelines + "\"");
	    	}
	    	
			analysisPipelineInterpreter = new AnalysisPipelineInterpreter(analysisPipelineReader);
			
			analysisPipelinesList = analysisPipelineInterpreter.getAnalysisPipelines();
			if (analysisPipelinesList == null || analysisPipelinesList.size() == 0)
				return JsonManager.generateResponseObject("Unable to parse specified CAP analysis pipelines \"" + analysisPipelines + "\"");
			
			eventObject = JsonManager.parseJson(event);
			returnObject = JsonManager.createObjectNode();
			returnArray = returnObject.putArray("results");
			
			for (AnalysisPipeline analysisPipeline: analysisPipelinesList)
			{
				entityIds = analysisPipeline.getEntityIds();
				analysisIds = analysisPipeline.getAnalysisIds();
				
				if (LOG.isTraceEnabled())
					LOG.trace("Analyzing the following event for entity ID \"" + entityIds.get(0) + "\" and analysis ID \"" + analysisIds.get(0) + "\":\n" + eventObject);
				
				try
		    	{
					if (useMicroStream)
						resultObject = XThreads.executeSynchronized(new AnalysisExecutor(entityIds.get(0), analysisIds.get(0), eventObject, variableSubstitutions, useCachedAnalysisState, resetState));
					else resultObject = executeCapAnalysis(entityIds.get(0), analysisIds.get(0), eventObject, variableSubstitutions, useCachedAnalysisState, resetState);
		    	} catch (Exception e)
		    	{
		    		StackTraceLogger.log("Unable to execute CAP analysis \"" + analysisIds.get(0) + "\"", Level.ERROR, e, LOG);
		    		return JsonManager.generateResponseObject("Unable to execute CAP analysis \"" + analysisIds.get(0) + "\"");
		    	}
				if (resultObject.get("success").asBoolean() && entityIds.size() > 1)
					for (index = 1; index < entityIds.size(); index++)
					{
						outputObject = resultObject.get("output");
						if (LOG.isTraceEnabled())
							LOG.trace("Analyzing the following event for entity ID \"" + entityIds.get(index) + "\" and analysis ID \"" + analysisIds.get(index) + "\":\n" + outputObject);
						if (outputObject.isEmpty())
							break;
						try
						{
							if (useMicroStream)
								resultObject = XThreads.executeSynchronized(new AnalysisExecutor(entityIds.get(index), analysisIds.get(index), outputObject, variableSubstitutions, useCachedAnalysisState, resetState));
							else resultObject = executeCapAnalysis(entityIds.get(index), analysisIds.get(index), outputObject, variableSubstitutions, useCachedAnalysisState, resetState);
							if (!resultObject.get("success").asBoolean())
								return resultObject;
						} catch (Exception e)
						{
				    		StackTraceLogger.log("Unable to execute CAP analysis \"" + analysisIds.get(index) + "\"", Level.ERROR, e, LOG);
				    		return JsonManager.generateResponseObject("Unable to execute CAP analysis \"" + analysisIds.get(index) + "\"");
				    	}
					}
				returnArray.add(resultObject);
			}
	
			if (consoleOutput)
				ConsoleOutputter.print(true, "Completed /analysis request");
		} finally
		{
  			if (LOG.isTraceEnabled())
  				LOG.trace("Execution time: "+ (System.currentTimeMillis() - then) + "ms");
		}
		
		return returnObject;
	}
	
	public CapApiServer(EmbeddedStorageManager storageManager, AllAnalysesStates allAnalysesStateData)
	{
		storageManagerField = storageManager;
		allAnalysesStatesField = allAnalysesStateData;
	}
	
	@Override
	public void run(String... args) throws Exception
	{
    	final CapHttpClient capHttpClient = new CapHttpClient();
    	
    	if (isEmpty(accessClientId))
    	{
    		LOG.error("The mandatory \"accessClientId\" is not configured");
			return;
    	}
    	if (isEmpty(accessPassword))
    	{
    		LOG.error("The mandatory \"accessPassword\" is not configured");
			return;
    	}
    	
    	logConfigurationParameter("consoleOutput", consoleOutput, true);
		logConfigurationParameter("analysisDirectory", analysisDirectory, "./analyses");
		logConfigurationParameter("stateDataDirectory", stateDataDirectory, "./stateData");
		logConfigurationParameter("eventFileDirectory", eventFileDirectory, "./eventData");
		logConfigurationParameter("eventBufferLength", eventBufferLength, 100);
		logConfigurationParameter("deleteEventFilesAfterDays", deleteEventFilesAfterDays, 90);
		logConfigurationParameter("prettyPrintLoggedJsonEvents", prettyPrintLoggedJsonEvents, false);
		logConfigurationParameter("proxyOn", proxyOn, false);
		logConfigurationParameter("proxyHostName", proxyHostName, "");
		logConfigurationParameter("proxyPort", proxyPort, -1);
		logConfigurationParameter("stateDataPurgeIntervalMinutes", stateDataPurgeIntervalMinutes, 720);
		logConfigurationParameter("one.microstream.use", useMicroStream, true);
    	
    	if (!analysisDirectory.endsWith(File.separator))
			analysisDirectory += File.separator;
		if (!FileUtilities.checkDirectory("analysisDirectory", analysisDirectory, true))
			return;
		if (!stateDataDirectory.endsWith(File.separator))
			stateDataDirectory += File.separator;
		if (!FileUtilities.checkDirectory("stateDataDirectory", stateDataDirectory, true))
			return;
		if (!eventFileDirectory.endsWith(File.separator))
			eventFileDirectory += File.separator;
		if (!FileUtilities.checkDirectory("eventFileDirectory", eventFileDirectory, true))
			return;
		if (eventBufferLength < 10 || eventBufferLength > 100000)
		{
			LOG.error("The configured \"eventBufferLength\" must be in the range [10, 100000]; it determines the maximum number of JSON objects in the CAP event JSON buffer before CAP writes the event JSON buffer to directory \"" + eventFileDirectory + "\"");
			return;
		}
		if (deleteEventFilesAfterDays < 1)
		{
			LOG.error("The configured \"deleteEventFilesAfterDays\" must be at least 1; it determines the maximum duration of JSON event files in the directory \"" + eventFileDirectory + "\"");
			return;
		}
		
		if (!capHttpClient.initialize(trustStorePathAndName, trustStorePassword, proxyOn, proxyHostName, proxyPort))
		{
			LOG.fatal("Unable to configure HttpRetriever");
			return;
		}
		CaplInterpreter.setCapHttpClient(capHttpClient);
		
		CaplInterpreter.setEventRecorder(new EventRecorder(eventFileDirectory, deleteEventFilesAfterDays, eventBufferLength, prettyPrintLoggedJsonEvents));
		
		if (useMicroStream)
			LOG.info("Using MicroStream embedded Java storage manager");
		else LOG.info("Using JSON analysis state file persistence");
		
		new StatePurger().start();

		
		ConsoleOutputter.print(true, "Ready");
		
		LOG.info("CAP REST API server started successfully");
	}
	
	@RequestMapping(name = "ping", method = RequestMethod.GET, path = { "/ping" })
	public String ping()
	{
		return "CAP Version 2.0";
	}
	
	@RequestMapping(name = "analyzeEvent", method = RequestMethod.POST, path = { "/eventAnalysis" }, consumes = "application/json", produces = "application/json")
	public ObjectNode analyzeEvent(@RequestParam(required = true) String analysisPipelines,
							   	   @RequestParam(required = false, defaultValue = "false") boolean useCachedAnalysisState,
							   	   @RequestParam(required = false, defaultValue = "false") boolean resetState,
							   	   @RequestParam(required = false) String variableSubstitutions,
							   	   @RequestBody(required = true) String eventObject,
							   	   @RequestHeader(required = true) String authentication)
	{
		return analyzeEventObject(analysisPipelines, useCachedAnalysisState, resetState, variableSubstitutions, eventObject, authentication);
	}
	
	@RequestMapping(name = "analyzeFile", method = RequestMethod.GET, path = { "/fileAnalysis" }, produces = "application/json")
	public ObjectNode analyzeFile(@RequestParam(required = true) String analysisPipelines, 
							  	  @RequestParam(required = true) String eventFileName,
							  	  @RequestParam(required = false, defaultValue = "false") boolean useCachedAnalysisState,
							  	  @RequestParam(required = false, defaultValue = "false") boolean resetState,
							  	  @RequestParam(required = false) String variableSubstitutions,
							  	  @RequestHeader(required = true) String authentication)
	{
		String jsonInput;
		
		if (isEmpty(eventFileName))
    	{
    		LOG.error("Must specify a non-empty input file with a JSON object (.json extension)");
    		return JsonManager.generateResponseObject("Must specify a non-empty input file with a JSON object (.json extension)");
    	}
    	eventFileName = eventFileDirectory + eventFileName + ".json";
    	jsonInput = FileUtilities.getFileContent(eventFileName);
    	if (LOG.isTraceEnabled())
    		LOG.trace("Reading JSON file \"" + eventFileName + "\"");
    	if (isEmpty(jsonInput))
    	{
    		LOG.error("Specified JSON event file \"" + eventFileName + "\" is empty");
    		return JsonManager.generateResponseObject("Specified JSON event file \"" + eventFileName + "\" is empty");
    	}
    	
    	return analyzeEventObject(analysisPipelines, useCachedAnalysisState, resetState, variableSubstitutions, jsonInput, authentication);
	}
	
	public static void main(String[] args)
	{
		SpringApplication.run(CapApiServer.class, args);
	}

	@PreDestroy
	public void shutdown() 
	{
		terminated = true;
		
		try
		{
			storageManagerField.shutdown();
		} catch (Throwable t)
		{}
		
		if (!useMicroStream)
			synchronized(ENTITIES_SEMAPHORE)
			{
				ENTITIES_SEMAPHORE.notifyAll();
			}
		
		if (consoleOutput)
			ConsoleOutputter.print(true, "Shutting down");
		
		LOG.info("CAP shutting down");
	}
}
