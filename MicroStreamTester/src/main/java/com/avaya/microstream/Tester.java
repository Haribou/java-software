package com.avaya.microstream;

import java.util.Set;

import javax.annotation.PreDestroy;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;

@SpringBootApplication
@Configuration
public class Tester implements CommandLineRunner
{
	private static final int NUM_ADDED_ENTRIES = 10;

	private StateData stateDataField;
	
	public Tester(StateData stateData)
	{
		stateDataField = stateData;
		if (stateDataField.getStateData().size() == 0)
			System.out.println("Persistent file storage is empty");
		else
		{
			System.out.println("Loaded state data from persistent file storage:");
			for (String onePersistedEntry: stateDataField.getStateData())
				System.out.println(onePersistedEntry);
		}
	}
	
	@Override
	public void run(String... args) throws Exception
	{
		final Set<String> stateDataSet = stateDataField.getStateData();
		
		String randomString;
		
		System.out.println("MicroStreamTester started successfully");
		
		for (int index = 0; index < NUM_ADDED_ENTRIES; index++)
		{
			randomString = "randomString-" + (int) (Math.floor(Math.random() * 1000));
			stateDataSet.add(randomString);
			System.out.println("Added string \"" + randomString + "\" to persistent state data");
		}
		
		stateDataField.save();
		
		System.out.println("Generated " +  NUM_ADDED_ENTRIES + " random strings and persisted them with MicroStream");
		
		System.exit(0);
	}

	@PreDestroy
	public void shutdown() 
	{
		System.out.println("MicroStreamTester shutting down");
	}
	
	public static void main(String[] args)
	{
		SpringApplication.run(Tester.class, args);
	}
}
