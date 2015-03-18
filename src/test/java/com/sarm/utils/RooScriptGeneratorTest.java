package com.sarm.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;

import junitx.framework.FileAssert;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sarm.utils.spring.roo.RooScriptGenerator;
import com.sarm.utils.spring.roo.RooScriptGenerator.DATATBASE_TYPE;

/**
 * Runs tests against all available test schemas and compares the output to the
 * associated expected roo scripts
 * 
 * @TODO: Make these tests parametized
 * 
 * @author Spencer
 *
 */
public class RooScriptGeneratorTest 
{
	private Logger logger = LoggerFactory.getLogger(RooScriptGeneratorTest.class);
	
	RooScriptGenerator gen = new RooScriptGenerator();
	
	@Test
	@Ignore
	public void testGokartGenerateScript() throws Exception
	{				
		generateScript("gokart");
	}

	@Test
	@Ignore
	public void testMenuGenerateScript() throws Exception
	{				
		generateScript("menu");
	}

	@Test
	public void testDefaultGenerateScript() throws Exception
	{	
		gen.setActiveRecordStyle(false);
		gen.setDatabaseType(DATATBASE_TYPE.HYPERSONIC_PERSISTENT);
		gen.setGenerateSeleniumTests(false);
		gen.setGenerateWebTier(false);
		gen.setJsonOnly(true);

		generateScript("default");
	}

	@Test
	public void testJsonOnlyGenerateScript() throws Exception
	{				
		gen.setActiveRecordStyle(false);
		gen.setDatabaseType(DATATBASE_TYPE.HYPERSONIC_PERSISTENT);
		gen.setGenerateSeleniumTests(true);
		gen.setGenerateWebTier(true);
		gen.setJsonOnly(false);

		generateScript("jsonOnly");
	}

	
	private void generateScript(String name) throws Exception
	{
		logger.info("Generating script");
		
		File srcFile = new File("src/test/resources/" + name + "/" + name + ".xsd");		
		File expectedRooFile = new File("src/test/resources/" + name + "/" + name + ".roo");
		File expectedUpdateFile = new File("src/test/resources/" + name + "/" + name + "Update.roo");
		File targetRooFile = new File("target/" + name + "/" + name + ".roo");
		File targetUpdateFile = new File("target/" + name + "/" + name + "Update.roo");
		
		gen.setSrcFile(srcFile);
		gen.setTargetFilename(targetRooFile);
		gen.generateScript();

		
		assertTrue("Target roo file not generated",targetRooFile.exists());
		assertTrue("Target roo update file not generated",targetRooFile.exists());

		FileAssert.assertEquals("Roo file not as expected",expectedRooFile, targetRooFile);
		FileAssert.assertEquals("Roo update file not as expected",expectedUpdateFile, targetUpdateFile);		
	}

}
