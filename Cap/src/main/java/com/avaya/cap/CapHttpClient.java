/*
 * 
 * @author Reinhard Klemm, Avaya
 * 
 */


package com.avaya.cap;

import static com.avaya.messaging.commons.utilities.StringUtils.isEmptyOrBlank;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.avaya.messaging.commons.io.HttpClient;
import com.avaya.messaging.commons.io.StackTraceLogger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class CapHttpClient
{
	private static final Logger LOG = Logger.getLogger(CapHttpClient.class);
	
	private HttpClient toWebService = null;
	
	private static final Header CONTENT_TYPE_HEADER = new BasicHeader("Content-Type", "application/json");
	
	public static String getAsString(CaplValue caplValue)
	{
		switch (caplValue.getValueDataType())
		{
		  	case NUMBER: return caplValue.getNumberValue() + "";
		  	case STRING: return caplValue.getStringValue();
		  	case BOOLEAN: return caplValue.getBooleanValue() + "";
			default: return null;
		}
	}

	public boolean initialize(String trustStorePathAndName,
							  String trustStorePassword,
							  boolean proxyOn,
							  String proxyHostName,
							  int proxyPort) 
	{
		try
		{
			if (!isEmptyOrBlank(trustStorePathAndName) && trustStorePassword == null)
			{
				LOG.error("If the value of environment variable \"trustStorePathAndName\" is defined, the variable \"trustStorePassword\" must be defined (you may use an empty string as a password)");
				return false;
			}
			
			if (proxyOn && (isEmptyOrBlank(proxyHostName) || proxyPort < 0))
			{
				LOG.error("If the value of environment variable \"proxy\" has a value of \"true\", the variables \"proxyHostName\" and \"proxyPort\" must be defined");
				return false;
			}
			if (isEmptyOrBlank(trustStorePathAndName))
				if (proxyOn)
					toWebService = new HttpClient(proxyHostName, proxyPort, null);
				else toWebService = new HttpClient(null, -1, null);
			else if (proxyOn)
				toWebService = new HttpClient(proxyHostName, proxyPort, null, trustStorePathAndName, trustStorePassword);
			else toWebService = new HttpClient(null, -1, null, trustStorePathAndName, trustStorePassword);
			
			return true;
		} catch (Exception e)
		{
			StackTraceLogger.log("Unable to initialize the HttpRetriever", Level.FATAL, e, LOG);
			return false;
		}
	}
	
	public JsonElement httpRequest(boolean isGet, String url, String queryParameters, Map<String, CaplValue> headers, CaplValue payload)
	{
		final List<Header> headersList = new ArrayList<>();
		
		String parameterValue;
		
		Header[] headersArray;
		
		JsonObject responseObject;
		
		JsonElement contentElement;
		
		int responseStatus;
		
		if (queryParameters == null)
		{
			LOG.error("Invalid query parameters for HTTP request");
			return null;
		}
		
		if (LOG.isTraceEnabled())
			LOG.trace("HTTP request to URL \"" + url + "\"");
		
		try
		{
			if (queryParameters.length() > 0)
			{
				if (!url.endsWith("?"))
					url += "?";
				url += queryParameters;
			}
			
			if (headers == null)
			{
				headersArray = new BasicHeader[1];
				headersArray[0] = CONTENT_TYPE_HEADER;
			}
			else 
			{
				for (Entry<String, CaplValue> header: headers.entrySet())
				{
					parameterValue = getAsString(header.getValue());
					if (parameterValue == null)
					{
						LOG.error("Invalid value \"" + header.getValue() + "\" for header \"" + header.getKey() + "\" in HTTP request - header value must be of primitive NUMBER, STRING, or BOOLEAN type");
						return null;
					}
					headersList.add(new BasicHeader(header.getKey(), parameterValue));
				}
					
				headersList.add(CONTENT_TYPE_HEADER);
				headersArray = headersList.toArray(new BasicHeader[headersList.size()]);
			}
			
			if (isGet)
				responseObject = toWebService.sendGetRequestJsonReturn(url, headersArray, true);
			else responseObject = toWebService.sendPostRequestJsonReturn(url, payload.toString(), headersArray, true);
			responseStatus = responseObject.get("responseStatus").getAsInt(); 
			if (responseStatus >= 300)
			{
				LOG.error("HTTP request to URL \"" + url + "\" failed with HTTP status code " + responseStatus);
				return null;
			}
			if (responseObject.has("content"))
			{
				contentElement = responseObject.get("content");
				if (LOG.isTraceEnabled())
					LOG.trace("Web service response: " + contentElement);
				
				return contentElement;
			}
			LOG.error("HTTP request to URL \"" + url + "\" failed because the expected \"content\" property is missing: " + responseObject);
			return null;
		} catch (Exception e)
		{
			StackTraceLogger.log("Unable to execute HTTP request to URL \"" + url + "\"", Level.WARN, e, LOG);
			return null;
		}
	}
}
