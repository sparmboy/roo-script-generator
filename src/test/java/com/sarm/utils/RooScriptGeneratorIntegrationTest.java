package com.sarm.utils;





import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Integration test suite that runs restful queries on the running
 * webapp that has been generated
 * 
 * @author Spencer
 *
 */
public class RooScriptGeneratorIntegrationTest
{
	Logger logger = LoggerFactory.getLogger(RooScriptGeneratorIntegrationTest.class);
	
	public final static String BASE_URL = "http://localhost:8077/test";

	@Test
	public void testMinSizeField() throws Exception
	{
		logger.info("Connecting to base URL " + BASE_URL );		
		String url = BASE_URL + "/people";		
		String driverJson = "{'name':'Spencer',age:-1}";
		
		// Create
		createJson(url, driverJson, HttpStatus.SC_INTERNAL_SERVER_ERROR, "'must be greater than or equal to 0', propertyPath=age");
	}

	@Test
	public void testMaxSizeField() throws Exception
	{
		logger.info("Connecting to base URL " + BASE_URL );		
		String url = BASE_URL + "/people";		
		String driverJson = "{'name':'Spencer',age:3000}";
		
		// Create
		createJson(url, driverJson, HttpStatus.SC_INTERNAL_SERVER_ERROR, "'must be less than or equal to 200', propertyPath=age");
	}

	
	@Test
	public void testMandatoryField() throws Exception
	{
		logger.info("Connecting to base URL " + BASE_URL );		
		String url = BASE_URL + "/people";		
		String driverJson = "{'name':'Spencer'}";
		
		// Create
		createJson(url, driverJson, HttpStatus.SC_INTERNAL_SERVER_ERROR, "interpolatedMessage='may not be null', propertyPath=age");
	}
	
	@Test
	public void testCreatePerson() throws Exception
	{
		logger.info("Connecting to base URL " + BASE_URL );		
		String url = BASE_URL + "/people";		
		String driverJson = "{'name':'Spencer','age':34}";
		
		// Create
		String newUrl = createJson(url, driverJson).toString();
         
        // Fetch
		String json = fetchJson(newUrl);
		assertTrue("Person not doesnt contain firstname",json.contains("\"name\":\"Spencer\""));
		
		// Delete
		delete(newUrl);
		
		// Fetch should fail
		json = fetchJson(newUrl, HttpStatus.SC_NOT_FOUND);
	}
	
	private String fetchJson( String url ) throws ParseException, IOException
	{
		return fetchJson(url, HttpStatus.SC_OK);		
	}

	private String fetchJson( String url, int expectedStatus ) throws ParseException, IOException
	{
		HttpGet get = new HttpGet(url);
		HttpClient httpClient = new DefaultHttpClient();
		get.setHeader("produces", "application/json");
		HttpResponse response = httpClient.execute(get);
		StatusLine status = response.getStatusLine();
		String json =  EntityUtils.toString( response.getEntity() );
		assertEquals("get failed",expectedStatus,status.getStatusCode());
		return json;		
	}

	
	private URL createJson( String url, String json ) throws ClientProtocolException, IOException
	{
		return createJson(url, json, HttpStatus.SC_CREATED);
	}
	
	private URL createJson( String url, String json, int expectedStatus ) throws ClientProtocolException, IOException
	{
		return createJson( url, json, expectedStatus, null );
	}
	private URL createJson( String url, String json, int expectedStatus, String expectedTextInBody ) throws ClientProtocolException, IOException
	{
		HttpPost post = new HttpPost(url);
		HttpClient httpClient = new DefaultHttpClient();
		StringEntity entity = new StringEntity(json);        
		post.setEntity(  entity);
        post.addHeader("content-type", "application/json");
        HttpResponse response = httpClient.execute(post);
        
        if( expectedTextInBody != null && !expectedTextInBody.trim().equals("") )
        {
	        String enc = getParameterValueFromHeaders(response.getAllHeaders(),"charset=");                
	        StringWriter writer = new StringWriter();
	        IOUtils.copy(response.getEntity().getContent(), writer, enc);
	        String responseBody = writer.toString();
	        
	        assertTrue("Response body did not contain '" + expectedTextInBody + "'",responseBody.contains(expectedTextInBody));
        }        
        
        StatusLine status = response.getStatusLine();        
        assertEquals("post failed",expectedStatus,status.getStatusCode());
        
        if( status.getStatusCode() != HttpStatus.SC_CREATED )
        {
        	return null;
        }
        
        String newUrlStr = response.getHeaders("Location")[0].getValue();        
        assertNotNull("New URL not created",newUrlStr);
        
        return new URL(newUrlStr);
	}
	
	private String getParameterValueFromHeaders(Header[] headers, String paramName )
	{
		for( Header header : headers )
		{
			for(HeaderElement element : header.getElements() )
			{
				if( element.toString().toLowerCase().contains(paramName) )
				{
					return element.toString().substring( element.toString().indexOf(paramName) + paramName.length() );
				}				
			}
		}
		
		return "";
	}
	
	private void delete( String url ) throws ClientProtocolException, IOException
	{
		HttpDelete delete = new HttpDelete(url);
		HttpClient httpClient = new DefaultHttpClient();
		HttpResponse response = httpClient.execute(delete);
		StatusLine status = response.getStatusLine();
		assertEquals("Delete failed",HttpStatus.SC_OK,status.getStatusCode());

	}
}
