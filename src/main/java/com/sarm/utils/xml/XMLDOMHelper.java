/**
 * 
 */
package com.sarm.utils.xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Spencer
 *
 */
public class XMLDOMHelper
{
	private static Logger logger = LoggerFactory.getLogger(XMLDOMHelper.class);
	
	public static Document readFile( String filename ) throws DocumentException, SAXException
	{
		Document doc = null;
		SAXReader reader = new SAXReader();
		
		reader.setValidation(false);
		reader.setFeature("http://xml.org/sax/features/validation",false);
		reader.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",false);
		
		doc = reader.read(filename);
		
		return doc;
	}
	
	public static Element addChildElementWithAttributes( Element parent, String childName, Attribute[] attributes )
	{
		Element childElement = parent.addElement(childName);
		for( Attribute at : attributes )
		{
			childElement.add(at);
		}
		return childElement;
	}

	public static Element addChildElementWithAttributes( Element parent, QName childName, Attribute[] attributes )
	{
		Element childElement = parent.addElement(childName);
		for( Attribute at : attributes )
		{
			childElement.add(at);
		}
		return childElement;
	}

	public static void updateNodesText( Document doc, String xpath, String value ) throws Exception
	{
		List<Node> nodes = doc.getRootElement().selectNodes(xpath);
		if( nodes != null && nodes.size() > 0 )
		{
			for( Node n : nodes )
			{
				logger.debug("Old value = " + n.getText());
				n.setText(value);
				logger.debug("New value = " + n.getText());
			}
		}
		else
		{
			throw new Exception("Failed to find nodes at for xpath '" + xpath + "'");
		}
	}
	
	public static void saveXMLDoc( Document doc, String filename ) throws IOException
	{
		logger.debug("Saving XML document to '" + filename + "'");
		
		FileOutputStream fos = new FileOutputStream( filename );
		OutputFormat format = OutputFormat.createPrettyPrint();
		
		XMLWriter writer = new XMLWriter( fos, format);
		
		writer.write(doc);
		writer.flush();
		
		writer.close();
		fos.close();
		
		logger.debug("Saved XML document to '" + filename + "'");
	}
	
	public static void addNamespace( Document doc, String prefix, String namespace )
	{
		doc.getRootElement().addNamespace(prefix, namespace);
	}

	public static void removeNamespace( Document doc,Namespace namespace )
	{
		doc.getRootElement().remove( namespace);
	}

}
