package com.avaya.microstream;

import java.util.Map.Entry;

import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import one.microstream.concurrency.XThreads;
import one.microstream.storage.embedded.types.EmbeddedStorageManager;

@SpringBootApplication
@RestController
public class Tester implements CommandLineRunner
{
	private static final Logger LOG = Logger.getLogger(Tester.class);

	private EmbeddedStorageManager microStreamStorageManager;
	
	private StateData stateData;
	
	private class StateUpdater implements Runnable
	{
		private String keyField,
					   valueField;
		
		StateUpdater(String key, String value)
		{
			keyField = key;
			valueField = value;
		}

		@Override
		public void run()
		{
			String value = stateData.getStateData().get(keyField);
			
			if (value == null)
			{
				stateData.getStateData().put(keyField, valueField);
				stateData.save(microStreamStorageManager);
				LOG.debug("Updated state");
			}
			
			LOG.debug("State data:");
			for (Entry<String, String> oneEntry: stateData.getStateData().entrySet())
				LOG.debug(oneEntry.getKey() + " => " + oneEntry.getValue());
		}
	}
	
	public Tester(EmbeddedStorageManager embeddedStorageManager, StateData stateData)
	{
		microStreamStorageManager = embeddedStorageManager;
		this.stateData = stateData;
		LOG.debug("State data:");
		for (Entry<String, String> oneEntry: stateData.getStateData().entrySet())
			LOG.debug(oneEntry.getKey() + " => " + oneEntry.getValue());
	}
	
	@Override
	public void run(String... args) throws Exception
	{
		LOG.info("MicroStreamTester started successfully");
	}
	
	@RequestMapping(name = "ping", method = RequestMethod.GET, path = { "/ping" })
	public String ping()
	{
		XThreads.executeSynchronized(new StateUpdater("key-" + System.currentTimeMillis(), "value-" + Math.random()));
		
		return "Updated embedded storage";
	}

	@PreDestroy
	public void shutdown() 
	{
		microStreamStorageManager.shutdown();
	
		LOG.info("MicroStreamTester shutting down");
	}
	
	public static void main(String[] args)
	{
		SpringApplication.run(Tester.class, args);
	}
}
