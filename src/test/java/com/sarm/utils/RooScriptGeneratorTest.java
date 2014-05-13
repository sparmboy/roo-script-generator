package com.sarm.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;

import junitx.framework.FileAssert;

import org.junit.Ignore;
import org.junit.Test;

import com.sarm.utils.spring.roo.RooScriptGenerator;
import com.sarm.utils.spring.roo.RooScriptGenerator.DATATBASE_TYPE;

public class RooScriptGeneratorTest 
{
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
	public void testGenerateScript() throws Exception
	{				
		generateScript("test");
	}

	
	private void generateScript(String name) throws Exception
	{
		File srcFile = new File("src/test/resources/" + name + ".xsd");		
		File expectedRooFile = new File("src/test/resources/" + name + ".roo");
		File expectedUpdateFile = new File("src/test/resources/" + name + "Update.roo");
		File targetRooFile = new File("target/" + name + ".roo");
		File targetUpdateFile = new File("target/" + name + "Update.roo");
		
		RooScriptGenerator gen = new RooScriptGenerator();
		gen.setActiveRecordStyle(false);
		gen.setSrcFile(srcFile);
		gen.setTargetFilename(targetRooFile);
		gen.setDatabaseType(DATATBASE_TYPE.HYPERSONIC_PERSISTENT);
		gen.generateScript();
		
		assertTrue("Target roo file not generated",targetRooFile.exists());
		assertTrue("Target roo update file not generated",targetRooFile.exists());

		FileAssert.assertEquals("Roo file not as expected",expectedRooFile, targetRooFile);
		FileAssert.assertEquals("Roo update file not as expected",expectedUpdateFile, targetUpdateFile);
		
	}

}
