package com.sarm.utils.spring.roo;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.dom4j.Document;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.tree.DefaultAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.roo.model.ReservedWords;

import com.sarm.utils.xml.XMLDOMHelper;

/**
 * Application to generate a Roo script containing all steps to setup a full web
 * application within Roo, from a source XSD file that defines the entire entity
 * model.
 * 
 * Once the script is run against Roo and the application is generated, you can
 * either leave Roo within the project setup, or remove it following the steps
 * detailed on the roo <a href=
 * "http://static.springsource.org/spring-roo/reference/html/removing.html"
 * >tutorial</a>
 * 
 * 
 * @author Spencer
 * 
 */
public class RooScriptGenerator
{
	public final static char[] CRLF = new char[]
	{ 13, 10 };

	private Logger logger = LoggerFactory.getLogger(RooScriptGenerator.class);

	public final static String XSD_FILE_OPTION = "xsdFile";
	public final static String TARGET_FILE_OPTION = "targetFile";
	public final static String DATABASE_TYPE_OPTION = "databaseType";
	
	/**
	 * The start of a http protocol in a namespace
	 */
	public final static String HTTP_PREFIX = "http://";

	/**
	 * The start of a www part in a namespace
	 */
	public final static String WWW_PREFIX = "www.";

	/**
	 * Roo command to create a selenium test
	 */
	private final static String ROO_CREATE_TEST = "selenium test --controller ";

	/**
	 * Roo command to create an entity
	 */
	private final static String ROO_CREATE_ENTITY = "entity jpa --class %%PACKAGE%%%%ENTITY%% --activeRecord %%ACTIVE_RECORD%% --testAutomatically";

	/**
	 * Roo command to to add a many to many relationship
	 */
	private final static String ROO_MANY_TO_MANY = "field set --fieldName %%FIELD_NAME%% --type %%PACKAGE%%%%ENTITY%% --class %%FROM_PACKAGE%%%%FROM_ENTITY%% --cardinality MANY_TO_MANY";

	/**
	 * Roo command to to add a many to one relationship
	 */
	private final static String ROO_MANY_TO_ONE = "field reference --fieldName %%FIELD_NAME%% --type %%PACKAGE%%%%ENTITY%% --class %%FROM_PACKAGE%%%%FROM_ENTITY%% --cardinality MANY_TO_ONE";

	
	/**
	 * Roo command to to add a one to many relationship
	 */
	private final static String ROO_ONE_TO_MANY = "field set --fieldName %%FIELD_NAME%% --type %%PACKAGE%%%%ENTITY%% --class %%FROM_PACKAGE%%%%FROM_ENTITY%% --cardinality ONE_TO_MANY";

	/**
	 * Roo command to to add a one to one relationship
	 */
	private final static String ROO_ONE_TO_ONE = "field reference --fieldName %%FIELD_NAME%% --type %%PACKAGE%%%%ENTITY%% --class %%FROM_PACKAGE%%%%FROM_ENTITY%% --cardinality ONE_TO_ONE";

	/**
	 * Roo command to to add a reference
	 */
	private final static String ROO_REFERENCE = "field reference --fieldName %%FIELD_NAME%% --type %%PACKAGE%%%%ENTITY%% --class %%FROM_PACKAGE%%%%FROM_ENTITY%%";

	
	/**
	 * Roo command to create a selenium test for an entity
	 */
	private final static String ROO_CREATE_SELENIUM_TEST = "selenium test --controller %%PACKAGE%%%%ENTITY%%Controller";

	/**
	 * Roo command to create an entity repository
	 */
	private final static String ROO_CREATE_REPO = "repository jpa --interface ";

	private final static String PACKAGE_TAG = "%%PACKAGE%%";

	private final static String FROM_PACKAGE_TAG = "%%FROM_PACKAGE%%";

	private final static String ENTITY_TAG = "%%ENTITY%%";

	private final static String FROM_ENTITY_TAG = "%%FROM_ENTITY%%";

	private final static String ACTIVERECORD_TAG = "%%ACTIVE_RECORD%%";

	private final static String TYPE_TAG = "%%TYPE%%";

	private final static String FIELD_NAME_TAG = "%%FIELD_NAME%%";

	/**
	 * The namespace prefix for the XSD schema
	 */
	private String xsdNsPrefix = "";
	
	/**
	 * The target namespace prefix for the XSD schema
	 */
	private String targetNsPrefix = "";
	
	/**
	 * The XSD schema document we load in for parsing
	 */
	private Document srcSchema;
	
	
	
	/**
	 * Annotation to add to an element in a complex schema to indicate that it
	 * is a ket field in a complex element e.g.:
	 * 
	 * <code>
	 *  <complexType name="Person">
	 * 	<sequence>
	 * 		<element name="name" type="string" maxOccurs="1"
	 * 			minOccurs="1">
	 *            <annotation>
	 *            	<documentation>key</documentation>
	 *            </annotation>
	 * 		</element>
	 * 	</sequence>
	 * 	</complexType>
	 * 
	 * </code>
	 */
	public final static String HIBERNATE_KEY_ANOTATION = "key";

	/**
	 * Annotation to add to an element in a complex schema to indicate that it
	 * is a key field in a complex element and that an automatic sequence should
	 * be appliede.g.:
	 * 
	 * <code>
	 *  <complexType name="Person">
	 * 	<sequence>
	 * 		<element name="personId" type="int" maxOccurs="1"
	 * 			minOccurs="1">
	 *            <annotation>
	 *            	<documentation>key sequence</documentation>
	 *            </annotation>
	 * 		</element>
	 * 	</sequence>
	 * 	</complexType>
	 * 
	 * </code>
	 */
	public final static String HIBERNATE_SEQUENCE_ANOTATION = "sequence";

	/**
	 * Specifies the the source XSD
	 */
	private File srcFile;

	public void setSrcFile(File srcFile)
	{
		this.srcFile = srcFile;
	}

	/**
	 * Specifies the target roo script file
	 */
	private File targetFile;

	public void setTargetFilename(File targetFile)
	{
		this.targetFile = targetFile;
	}

	/**
	 * Instructs the mojo to annotate generated entities with
	 * @RooJpaActiveRecord. This is the default pattern for Roo entities which
	 * means that an entity encapsulates and maintains its own CRUD interactions
	 * with the data source. Setting this value to false will instead annotate
	 * the entity with @RooJpaEntity and append to the script to generate a
	 * repository class for it
	 * 
	 */
	private boolean activeRecordStyle;

	/**
	 * @return the activeRecordStyle
	 */
	public boolean isActiveRecordStyle()
	{
		return activeRecordStyle;
	}

	/**
	 * @param activeRecordStyle
	 *            the activeRecordStyle to set
	 */
	public void setActiveRecordStyle(boolean activeRecordStyle)
	{
		this.activeRecordStyle = activeRecordStyle;
	}

	public enum CARDINALITY
	{
		OPTIONAL, MANDATORY, ONE_OR_MORE, UNBOUNDED, NONE
	}

	// ==================== XSD DEFINITIONS =================================

	private final static String UNBOUNDED = "unbounded";

	public final static String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	/**
	 * This list is taken from here:
	 * 
	 * https://github.com/spring-projects/spring-roo/blob/master/addon-jpa/src/main/java/org/springframework/roo/addon/jpa/JdbcDatabase.java
	 * 
	 * Until the addon-jpa artifact is available publicly in a maven repo its a copy and paste job
	 * 
	 * @author Spencer
	 *
	 */
	public enum DATATBASE_TYPE 
	{
		 DATABASE_DOT_COM, //
		 DB2_400, //
		 DB2_EXPRESS_C, //
		 DERBY_CLIENT, //
		 DERBY_EMBEDDED, //
		 FIREBIRD, //
		 GOOGLE_APP_ENGINE, //
		 H2_IN_MEMORY, //
		 HYPERSONIC_IN_MEMORY, //
		 HYPERSONIC_PERSISTENT, //
		 MSSQL, //
		 MYSQL, //
		 ORACLE, //
		 POSTGRES, //
		 SYBASE
	}
	
	private static DATATBASE_TYPE DEFAULT_DATABASE_TYPE = DATATBASE_TYPE.H2_IN_MEMORY;
	
	/**
	 * The database type that the roo script will use
	 */
	private DATATBASE_TYPE databaseType = DEFAULT_DATABASE_TYPE;
	
	
	
	/**
	 * @return the databaseType
	 */
	public DATATBASE_TYPE getDatabaseType()
	{
		return databaseType;
	}

	/**
	 * @param databaseType the databaseType to set
	 */
	public void setDatabaseType(DATATBASE_TYPE databaseType)
	{
		this.databaseType = databaseType;
	}

	/**
	 * Reads the XSD file and generates the roo script
	 * 
	 * @throws Exception
	 */
	public void generateScript() throws Exception
	{
		
		// Create the target dir if it does not exist
		File targetDirFile = targetFile;
		File targetDir = targetDirFile.getParentFile();

		if (!targetDir.exists())
		{
			if (!targetDir.mkdirs())
				throw new RuntimeException("Failed to create all directories in the specified target directory path [" + targetDirFile + "].");
		}

		// We also create an update script which only contains the changes to
		// the entities and none of the scaffolding
		String updateFilename = targetDirFile.getAbsoluteFile().getName();
		if (updateFilename.contains("."))
		{
			updateFilename = updateFilename.substring(0, updateFilename.lastIndexOf(".")) + "Update" + updateFilename.substring(updateFilename.lastIndexOf("."));
		} else
		{
			updateFilename += "Update";
		}

		// Create our script
		PrintStream rooScript = new PrintStream(targetDirFile);

		PrintStream rooUpdateScript = new PrintStream(targetDir + File.separator + updateFilename);
		rooUpdateScript.println("##########################################");
		rooUpdateScript.println("# This script only contains the entities #");
		rooUpdateScript.println("# and should be run against an existing  #");
		rooUpdateScript.println("# roo projects to update any changes in  #");
		rooUpdateScript.println("# the model							  #");
		rooUpdateScript.println("##########################################");

		// Load in the doc
		srcSchema = XMLDOMHelper.readFile(srcFile.getAbsolutePath());
		srcSchema.getRootElement().addNamespace("xs", XSD_NAMESPACE);

		// Find the target namespace for this schema
		DefaultAttribute targetNs = (DefaultAttribute) srcSchema.getRootElement().selectSingleNode("@targetNamespace");
		DefaultAttribute xmlns = (DefaultAttribute) srcSchema.getRootElement().selectSingleNode("@xmlns");

		
		List<Namespace> namespaces = srcSchema.getRootElement().declaredNamespaces();
		for (Namespace ns : namespaces)
		{
			if (ns.getURI().equals(targetNs.getStringValue()))
			{
				targetNsPrefix = ns.getPrefix();
			}
			else if( ns.getURI().equals(XSD_NAMESPACE) )
			{
				xsdNsPrefix = ns.getPrefix();
			}
		}

		String entityPackageName = extractPackageFromNamespace(targetNs.getStringValue());

		// Create standard project initialisation steps in script
		rooScript.println("##########################");
		rooScript.println("# Project Initialisation #");
		rooScript.println("##########################");
		rooScript.println("");
		rooScript.println("# Create the project");
		rooScript.println("project --topLevelPackage " + entityPackageName.substring(0, entityPackageName.length() - 1));
		rooScript.println("");
		rooScript.println("# Setup persistence");
		rooScript.println("jpa setup --provider HIBERNATE --database " + databaseType);
		rooScript.println("");
		rooScript.println("#######################");
		rooScript.println("# Create the entities #");
		rooScript.println("#######################");
		rooScript.println("");

		rooUpdateScript.println("#######################");
		rooUpdateScript.println("# Update the entities #");
		rooUpdateScript.println("#######################");
		rooUpdateScript.println("");

		// Fetch all the complex types
		// List<Node> complexTypes =
		// srcSchema.selectNodes("//xs:complexType | //xs:simpleType");
		List<Node> complexTypeNodes= srcSchema.getRootElement().selectNodes("xs:complexType");
		List<Node> elementNodes = srcSchema.getRootElement().selectNodes("xs:element");
		
		List<Node> entityElements = new ArrayList<Node>(complexTypeNodes);
		entityElements.addAll(elementNodes);
		
		Map<String, List<RooField>> entities = new HashMap<String, List<RooField>>();
		
		// Create the Roo entites
		for (Node entityNode : entityElements)
		{
			String nodeName = entityNode.selectSingleNode("@name") == null ? "" : entityNode.selectSingleNode("@name").getStringValue();

			if (nodeName == null || nodeName.equals(""))
				continue;

			String newEntityName = convertReservedWords(nodeName);

			// Now find any elements in the node that reference other
			// entities
			List<Node> elements = entityNode.selectNodes(entityNode.getUniquePath() + "//xs:element");

			// Lets build a map of all complex types by name, what
			// elements they have, and their cardinality
			List<RooField> containingElements = new ArrayList<RooField>();

			// Inheritance
			Node extensionNode = entityNode.selectSingleNode(entityNode.getUniquePath() + "//xs:extension");
			String extensionCommand = "";
			if (extensionNode != null)
			{
				String base = extensionNode.selectSingleNode("@base") == null ? "" : extensionNode.selectSingleNode("@base").getStringValue();

				if (!base.equals(""))
				{
					if (base.contains(":"))
					{
						base = base.substring(base.indexOf(":") + 1);
					}

					extensionCommand = " --extends " + base;
				}
			}

			// Write the create entity script line
			rooScript.println("# " + nodeName);
			rooScript.println(ROO_CREATE_ENTITY.replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, newEntityName).replace(ACTIVERECORD_TAG, "" + activeRecordStyle) + extensionCommand);

			rooUpdateScript.println("# " + nodeName);
			rooUpdateScript.println(ROO_CREATE_ENTITY.replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, newEntityName).replace(ACTIVERECORD_TAG, "" + activeRecordStyle) + extensionCommand);

			// First iteration to extract OneToMany
			for (Node element : elements)
			{

				RooField rooField = xsdElementToRooField(entityNode,element);

				// Does the type have a prefix?
				if( rooField.xsdType.contains(":") )
				{
					// Check if the type references another entity in this
					// schema
					if (rooField.xsdType.startsWith(targetNsPrefix))
					{
						rooField.xsdType = rooField.xsdType.split(":")[1];
						containingElements.add(rooField);
						//addContainingElements(element, elementName, type, containingElements);
						
					}
					// TODO: check if type refers to another namespace
					/*else if(  )
					{
						
					}*/
					// Otherwise its a normal element field
					else
					{

						rooField.owningEntity = entityPackageName + convertReservedWords( nodeName );
						rooScript.println( rooField.toString() );
						rooUpdateScript.println( rooField.toString() );
					}
				}
				else
				{
					containingElements.add(rooField);
				}
				
				

			}

			entities.put(nodeName, containingElements);

			// Add the entity to the Roo repo script file
			if (!activeRecordStyle)
			{
				rooScript.println(ROO_CREATE_REPO + entityPackageName + nodeName + "Repository --entity " + entityPackageName + nodeName);
				rooUpdateScript.println(ROO_CREATE_REPO + entityPackageName + nodeName + "Repository --entity " + entityPackageName + nodeName);
			}

			rooScript.println("");
			rooUpdateScript.println("");

		}

		rooScript.println("########################");
		rooScript.println("# Entity Relationships #");
		rooScript.println("########################");

		rooUpdateScript.println("########################");
		rooUpdateScript.println("# Entity Relationships #");
		rooUpdateScript.println("########################");

		// Set a store for each binding node we create and any bidings
		// that need to be
		// updated later by a separate element that refers back to it
		Map<String, String> relationCommands = new HashMap<String, String>();
		Map<String, String> mapLater = new HashMap<String, String>();

		// Now lets go through our built up list of entities and define
		// the relational bindings
		List<String> alreadyMapped = new ArrayList<String>();
		Iterator<String> entityKeysIt = entities.keySet().iterator();
		while (entityKeysIt.hasNext())
		{
			String entityName = entityKeysIt.next();
			List<RooField> elements = entities.get(entityName);

			// Iterate each element in the entity
			for(RooField rooField : elements)
			{					
				// Fetch the element name and cardinality
				String elementName = rooField.fieldName;
				String cardinality = rooField.cardinality.name();
				String elementType = rooField.xsdType;
				

				elementName = convertReservedWords(elementName);
				
				// Get the details of the element being referred to
				List<RooField> referredEntity = entities.get(elementType);
				String referredElementName = "";
				String referredCardinality = "";
				
				if (referredEntity != null)
				{
					RooField referredElementDetails = getReferredEntityFromElementDetails( referredEntity, entityName);
					if (referredElementDetails != null)
					{
						referredElementName = referredElementDetails.fieldName;
						referredCardinality = referredElementDetails.cardinality.toString();
					}
				}

				// ONE TO MANY
				if (cardinality.equals(CARDINALITY.UNBOUNDED.toString()) || cardinality.equals(CARDINALITY.ONE_OR_MORE.toString()))
				{
					// MANY TO MANY
					if (referredCardinality.equals(CARDINALITY.UNBOUNDED.toString()) || referredCardinality.equals(CARDINALITY.ONE_OR_MORE.toString()))
					{
						String command = new String(CRLF) + "# " + entityName + " to " + elementType + " [Many to Many]" + new String(CRLF);

						// TODO: doesnt handle references to external
						// namespaces!
						command += ROO_MANY_TO_MANY.replace(FIELD_NAME_TAG, elementName).replace(FROM_PACKAGE_TAG, entityPackageName).replace(FROM_ENTITY_TAG, entityName).replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, elementType);

						String key = entityName.compareTo(elementType) < 0 ? entityName + elementType : elementType + entityName;
						if (!alreadyMapped.contains(key))
						{
							command += " --mappedBy " + referredElementName;
							alreadyMapped.add(key);
						}

						rooScript.println(command);
						rooUpdateScript.println(command);

						relationCommands.put(entityName + ":" + elementType, command);
					}
					// ONE TO MANY
					else
					{
						String command = new String(CRLF) + "# " + entityName + " to " + elementType + " [One to Many]" + new String(CRLF);

						// TODO: doesnt handle references to external
						// namespaces!
						command += ROO_ONE_TO_MANY.replace(FIELD_NAME_TAG, elementName).replace(FROM_PACKAGE_TAG, entityPackageName).replace(FROM_ENTITY_TAG, entityName).replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, elementType);

						String mappedBy = mapLater.get(entityName + ":" + elementType);
						if (mappedBy != null)
						{
							// command += " --mappedBy " +
							// referredElementName;
						}

						rooScript.println(command);
						rooUpdateScript.println(command);

						relationCommands.put(entityName + ":" + elementType, command);

					}
				}
				// MANY TO ONE
				else if (cardinality.equals(CARDINALITY.MANDATORY.toString()) && (referredCardinality.equals(CARDINALITY.UNBOUNDED.toString()) || referredCardinality.equals(CARDINALITY.ONE_OR_MORE.toString())))
				{
					String command = new String(CRLF) + "# " + entityName + " to " + elementType + " [Many to One]" + new String(CRLF);

					// TODO: doesnt handle references to external
					// namespaces!
					command += ROO_MANY_TO_ONE.replace(FIELD_NAME_TAG, elementName).replace(FROM_PACKAGE_TAG, entityPackageName).replace(FROM_ENTITY_TAG, entityName).replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, elementType);

					// We need to fetch the parent binding or if its not
					// been created yet, add a flag
					// to set it later
					String oneToManyNodeCommand = relationCommands.get(elementType + ":" + entityName);
					if (oneToManyNodeCommand != null)
					{
						/*
						 * TODO Element parentClassNode = (Element)
						 * oneToManyNode
						 * .selectSingleNode(oneToManyNode.getUniquePath() +
						 * "//annox:annotate[@annox:class='" +
						 * OneToMany.class.getName() + "']");
						 * parentClassNode.addAttribute("mappedBy",
						 * elementName);
						 */
					} else
					{
						mapLater.put(elementType + ":" + entityName, elementName);
					}

					rooScript.println(command);
					rooUpdateScript.println(command);

					relationCommands.put(entityName + ":" + elementType, command);
				}

				// ONE TO ONE
				else if (cardinality.equals(CARDINALITY.MANDATORY.toString()) && referredCardinality.equals(CARDINALITY.MANDATORY.toString()))
				{
					String command = new String(CRLF) + "# " + entityName + " to " + elementType + " [One to One]" + new String(CRLF);

					// TODO: doesnt handle references to external
					// namespaces!
					command += ROO_ONE_TO_ONE.replace(FIELD_NAME_TAG, elementName).replace(FROM_PACKAGE_TAG, entityPackageName).replace(FROM_ENTITY_TAG, entityName).replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, elementType);

					rooScript.println(command);
					rooUpdateScript.println(command);

					relationCommands.put(entityName + ":" + elementType, command);
				}
				// Plain singular reference
				else if (cardinality.equals(CARDINALITY.NONE.toString()) ||
						 cardinality.equals(CARDINALITY.OPTIONAL.toString()) ||
						(cardinality.equals(CARDINALITY.MANDATORY.toString() ) && (referredEntity == null || referredEntity.size() == 0) ) )
				{
					// Find the defined element type in the source
					// schema and determine if it is a complexType or a
					// simpleType
					// TODO: For now we ignore simple types as we dont
					// want to impose a binding against a simple type
					// (this could change)
					if (srcSchema.selectSingleNode("//xs:complexType[@name=\"" + elementType + "\"]") != null)
					{
						String command = new String(CRLF) + "# " + entityName + " to " + elementType + " \n";

						// TODO: doesnt handle references to external
						// namespaces!
						command += ROO_REFERENCE.replace(FIELD_NAME_TAG, elementName).replace(FROM_PACKAGE_TAG, entityPackageName).replace(FROM_ENTITY_TAG, entityName).replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, elementType);

						rooScript.println(command);
						rooUpdateScript.println(command);
					}
					// Then it must be a simple type
					else
					{
						rooField.owningEntity = entityPackageName + convertReservedWords( entityName );

						// Write script entries to create the entities
						//rooScript.println(ROO_CREATE_FIELD.replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, entityName).replace(TYPE_TAG, rooType).replace(FIELD_NAME_TAG, fieldName));
						//rooUpdateScript.println(ROO_CREATE_FIELD.replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, entityName).replace(TYPE_TAG, rooType).replace(FIELD_NAME_TAG, fieldName) );
						
						rooScript.println( rooField.toString() );
						rooUpdateScript.println( rooField.toString() );

					}
				}

			}
		}

		rooScript.println("");
		rooScript.println("########################");
		rooScript.println("# Web Tier	       #");
		rooScript.println("########################");
		rooScript.println("json all");			
		rooScript.println("web mvc json setup");
		rooScript.println("web mvc json all --package " + entityPackageName.substring(0, entityPackageName.length() - 1));

		rooUpdateScript.println("");
		rooUpdateScript.println("json all");
		rooUpdateScript.println("");
		
		// Add selenium tests
		rooScript.println("");
		rooScript.println("########################");
		rooScript.println("# Add Selenium Tests   #");
		rooScript.println("########################");
		for (Node node : entityElements)
		{
			String nodeName = node.selectSingleNode("@name") == null ? "" : node.selectSingleNode("@name").getStringValue();

			if (nodeName == null || nodeName.equals(""))
				continue;

			String newEntityName = convertReservedWords(nodeName);

			rooScript.println(ROO_CREATE_SELENIUM_TEST.replace(PACKAGE_TAG, entityPackageName).replace(ENTITY_TAG, newEntityName));

		}

		// close the file and we're done
		rooScript.close();
		rooUpdateScript.close();
	}
	
	/**
	 * 
	 * The list contains the element details int he format
	 * 
	 * name:cardinality:type
	 * 
	 * e.g.
	 * 
	 * firstName:MANDATORY:string
	 * 
	 * This retuns the element detail matching the specified type.
	 * 
	 * TODO: THis is SHIT. Create an ElementDetail type instead!
	 * 
	 * @param elementDetails
	 * @param type
	 * @return
	 */
	private RooField getReferredEntityFromElementDetails( List<RooField> elementDetails, String type )
	{
		for( RooField rooField : elementDetails )
		{
			if( rooField.rooType.equals( type ) )
			{
				return rooField;
			}
		}
		
		return null;
	}

	/**
	 * Derives a class' package that will be generated from the namespace that
	 * an entity belongs to
	 * 
	 * @param namespace
	 * @return
	 */
	public String extractPackageFromNamespace(String namespace)
	{
		String path = "";

		// Strip the protocol and www
		if (namespace.contains(HTTP_PREFIX))
		{
			namespace = namespace.substring(namespace.indexOf(HTTP_PREFIX) + HTTP_PREFIX.length());
		}

		if (namespace.contains(WWW_PREFIX))
		{
			namespace = namespace.substring(namespace.indexOf(WWW_PREFIX) + WWW_PREFIX.length());
		}

		String trailingPath = "";
		if (namespace.contains("/"))
		{
			trailingPath = namespace.substring(namespace.indexOf("/") + 1);
			namespace = namespace.substring(0, namespace.indexOf("/"));
		}

		String[] elements = namespace.split("\\.");
		for (int i = elements.length - 1; i >= 0; i--)
		{
			path += elements[i] + ".";
		}

		elements = trailingPath.split("/");
		for (String e : elements)
		{
			if( e.length() > 0 )
			{
				path += e + ".";
			}
		}

		return path;

	}
	
	/**
	 * Converts an XSD element node that belongs to an entity to a Roo Field object
	 * @return
	 */
	private RooField xsdElementToRooField( Node entity, Node element )
	{
		RooField rooField = new RooField();

		String elementName = element.selectSingleNode("@name") == null ? "" : element.selectSingleNode("@name").getStringValue();
		String type = element.selectSingleNode("@type") == null ? "" : element.selectSingleNode("@type").getStringValue();
		String minOccurs = element.selectSingleNode("@minOccurs") == null ? "" : element.selectSingleNode("@minOccurs").getStringValue();
		String maxOccurs = element.selectSingleNode("@maxOccurs") == null ? "" : element.selectSingleNode("@maxOccurs").getStringValue();
		String defaultValue = element.selectSingleNode("@default") == null ? null : element.selectSingleNode("@default").getStringValue();
		String documentation = element.selectSingleNode(element.getUniquePath() + "//*[name()='documentation']") == null ? null : element.selectSingleNode(element.getUniquePath() + "//*[name()='documentation']").getStringValue().trim();
		String regexp = element.selectSingleNode(element.getUniquePath() + "//*[name()='pattern']/@value") == null ? null : element.selectSingleNode(element.getUniquePath() + "//*[name()='pattern']/@value").getStringValue();		
		String minValue = element.selectSingleNode(element.getUniquePath() + "//*[name()='minExclusive']/@value") == null ? null : element.selectSingleNode(element.getUniquePath() + "//*[name()='minExclusive']/@value").getStringValue() ; 
		String maxValue = element.selectSingleNode(element.getUniquePath() + "//*[name()='maxExclusive']/@value") == null ? null : element.selectSingleNode(element.getUniquePath() + "//*[name()='maxExclusive']/@value").getStringValue() ;
		String minLength = element.selectSingleNode(element.getUniquePath() + "//*[name()='minLength']/@value") == null ? null : element.selectSingleNode(element.getUniquePath() + "//*[name()='minLength']/@value").getStringValue() ; 
		String maxLength = element.selectSingleNode(element.getUniquePath() + "//*[name()='maxLength']/@value") == null ? null : element.selectSingleNode(element.getUniquePath() + "//*[name()='maxLength']/@value").getStringValue() ;

		// If type is not specified as an attribute we need to determine it is a simple type with a restriction
		if( type.equals("") )
		{
			Node typeNode = element.selectSingleNode(element.getUniquePath() + "//@base");  
			if( typeNode == null )
			{
				throw new RuntimeException("Could not determine type of element " + elementName + ". Element=[" + element + "]");
			}
			else
			{
				type = typeNode.getStringValue();
			}
		}
		
		// Check if the element has been annotated with a
		// special "KEY" to denote it is a key element for
		// hibernate
		if (element.hasContent())
		{
			Node anon = element.selectSingleNode(element.getUniquePath() + "//xs:documentation");
			if (anon != null && anon.getText().toLowerCase().contains(HIBERNATE_KEY_ANOTATION.toLowerCase()))
			{
				rooField.unique = true;
			}

		}

		
		
		
		
		rooField.fieldName = convertReservedWords( elementName );
		rooField.xsdType = type;
		rooField.rooType = mapXsdTypeToRooType( type );
		rooField.regexp = regexp;		
		rooField.value = defaultValue;
		rooField.comment = documentation;
		if( isElementDefinedAsUnique( entity, element) )rooField.unique = true;
		
		if( minValue != null )
		{
			if( rooField.xsdType.equals("int") || rooField.xsdType.equals("integer") )
				rooField.min = Integer.parseInt(minValue);
			if( rooField.xsdType.equals("double") || rooField.xsdType.equals("decimal") )
				rooField.decimalMin = Double.parseDouble(minValue);

		}
		if( maxValue != null )
		{
			if( rooField.xsdType.equals("int") || rooField.xsdType.equals("integer") )
				rooField.max = Integer.parseInt(maxValue);
			if( rooField.xsdType.equals("double") || rooField.xsdType.equals("decimal") )
				rooField.decimalMax = Double.parseDouble(maxValue);
			
		}
		if( minLength != null )rooField.sizeMin = Integer.parseInt(minLength);
		if( maxLength != null )rooField.sizeMax = Integer.parseInt(maxLength);
		
		if (minOccurs.equals("0") && maxOccurs.equals("1"))
		{
			rooField.cardinality = CARDINALITY.OPTIONAL;
			rooField.notNull = false;
		} 
		else if (minOccurs.equals("1") && maxOccurs.equals("1"))
		{
			rooField.cardinality =  CARDINALITY.MANDATORY;
			rooField.notNull = true;
		} 
		else if (minOccurs.equals("0") && maxOccurs.equalsIgnoreCase(UNBOUNDED))
		{
			rooField.cardinality =  CARDINALITY.UNBOUNDED;
			//rooField.rooType = "list";
		} 
		else if (minOccurs.equals("1") && maxOccurs.equalsIgnoreCase(UNBOUNDED))
		{
			rooField.cardinality =  CARDINALITY.ONE_OR_MORE;
			//rooField.rooType = "list";
		} 
		else
		{
			rooField.cardinality =  CARDINALITY.NONE;
		}

		return rooField;
	}
	
	/**
	 * Determines is a node within an entity has a unique definition
	 * defined for it in the schema
	 * @param element
	 * @return
	 */
	private boolean isElementDefinedAsUnique( Node entity, Node element )
	{
		List<Node> uniqueNodes = entity.selectNodes("xs:unique");
		for( Node uniqueNode : uniqueNodes )
		{
			Node selectorXpath = uniqueNode.selectSingleNode( "xs:selector/@xpath");
			Node fieldXpath = uniqueNode.selectSingleNode("xs:field/@xpath");
			if( selectorXpath != null && fieldXpath != null )
			{
				String selectorXpathStr = selectorXpath.getStringValue();
				
				/*
				 * We expect the selector xpath to be prefixed with the target namespace
				 * so that it references an element in this schema. If not we return false
				 * 
				 * @TODO: Culd relference other schema namespaces
				 */
				if( !selectorXpathStr.startsWith( targetNsPrefix + ":" ) )
				{
					return false;
				}
				selectorXpathStr = selectorXpathStr.split(":")[1];
				
				
				Node selectedNode = srcSchema.selectSingleNode("//xs:element[./@name='" + selectorXpathStr + "']");
				if( entity.equals(selectedNode) )
				{
					Node fieldNode = selectedNode.selectSingleNode(fieldXpath.getStringValue());
					return element.equals(fieldNode);
				}
			}
		}
		return false;
	}
	
	/**
	 * Takes an xsd type and maps it to a roo entity field type
	 * 
	 * @TODO: Dont think this works in all variations of unique definitions. Needs
	 * more testing with other schemas
	 * 
	 * @return
	 */
	public String mapXsdTypeToRooType(String type)
	{
		if( type.startsWith(xsdNsPrefix) )type = type.substring( xsdNsPrefix.length() + 1 );
		
		if (type.equals("anyURI"))
			return "other --type java.net.URI";
		if (type.equals("base64Binary"))
			return "string";
		if (type.equals("boolean"))
			return "boolean";
		if (type.equals("byte"))
			return "number --type byte";
		if (type.equals("date"))
			return "date --type java.util.Calendar";
		if (type.equals("dateTime"))
			return "date --type java.util.Calendar";
		if (type.equals("decimal"))
			return "number --type java.math.BigDecimal";
		if (type.equals("double"))
			return "number --type double";
		if (type.equals("duration"))
			return "long";
		if (type.equals("float"))
			return "number --type float";
		if (type.equals("gDay"))
			return "string";
		if (type.equals("gMonth"))
			return "string";
		if (type.equals("gMonthDay"))
			return "string";
		if (type.equals("gYear"))
			return "string";
		if (type.equals("gYearMonth"))
			return "string";
		if (type.equals("hexBinary"))
			return "string";
		if (type.equals("ID"))
			return "string";
		if (type.equals("IDREF"))
			return "string";
		if (type.equals("IDREFS"))
			return "string";
		if (type.equals("int"))
			return "number --type int";
		if (type.equals("integer"))
			return "number --type java.lang.Integer";
		if (type.equals("language"))
			return "String";
		if (type.equals("long"))
			return "number --type long";
		if (type.equals("Name"))
			return "string";
		if (type.equals("NCName"))
			return "string";
		if (type.equals("negativeInteger"))
			return "number --type int --max -1";
		if (type.equals("NMTOKEN"))
			return "string";
		if (type.equals("NMTOKENS"))
			return "string";
		if (type.equals("nonNegativeInteger"))
			return "number --type int --min -1";
		if (type.equals("nonPositiveInteger"))
			return "number --type int --max 0";
		;
		if (type.equals("normalizedString"))
			return "string";
		if (type.equals("positiveInteger"))
			return "number --type int --min 0";
		if (type.equals("QName"))
			return "string";
		if (type.equals("short"))
			return "number --type short";
		if (type.equals("string"))
			return "string";
		if (type.equals("time"))
			return "date --type java.util.Calendar";
		if (type.equals("token"))
			return "string";
		if (type.equals("unsignedByte"))
			return "number --type byte --min 0 --max 255";
		if (type.equals("unsignedInt"))
			return "number --type int --min 0 --max 2147483647";
		if (type.equals("unsignedLong"))
			return "number --type long --min 0 --max 18446744073709551615";
		if (type.equals("unsignedShort"))
			return "number --type short --min 0 --max 65535";
		
		logger.warn("Could not find xsd type of " + type + ". Returning string");
		
		return "string";
	}



	/**
	 * Converts a string in camel case to underscores
	 * 
	 * @param camelCaseVar
	 * @return
	 */
	public static String camelToUnderScores(String camelCaseVar)
	{
		return camelCaseVar.replaceAll(String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])"), "_").toUpperCase();
	}


	/**
	 * Takes any name of a field and converts it if it is a reserved word in roo
	 * 
	 * @param name
	 * @return
	 */
	public String convertReservedWords(String name)
	{
		// Remove any hyphens
		name = name.replace("-", "_");				

		if (ReservedWords.RESERVED_JAVA_KEYWORDS.contains(name.toLowerCase()) || ReservedWords.RESERVED_SQL_KEYWORDS.contains(name.toLowerCase()))
		{
			logger.warn("Found reserved word '" + name + "' converting to '" + name + "1'");
			return name + "1";
		}

		return name;
	}

	public final static void main(String[] args) throws Exception 
	{
		// Specify command line options
		Options options = new Options();
		Option xsdOption = new Option(XSD_FILE_OPTION, true, "The XSD file to be parsed");
		Option targetFileOption = new Option(TARGET_FILE_OPTION, true, "The target file to write the roo script to");
		Option databaseTypeOption = new Option(DATABASE_TYPE_OPTION, true, "The type of database to be used for persisting the entities in the RESTful service. Default is " + DEFAULT_DATABASE_TYPE + ". Valid options are " + Arrays.asList( DATATBASE_TYPE.values() ) );
		
		xsdOption.setRequired(true);
		targetFileOption.setRequired(true);
		databaseTypeOption.setRequired(false);
		
		options.addOption( xsdOption );
		options.addOption(targetFileOption);
		options.addOption(databaseTypeOption);
		
		// Parse
		BasicParser parser = new BasicParser();
		try
		{
			CommandLine cl = parser.parse(options, args);
			
			if ( cl.hasOption('h') ) 
			{
			    showUsage(options);
			    System.exit(0);
			}
		
			// Configure the generator
			RooScriptGenerator generator = new RooScriptGenerator();
			generator.setSrcFile(new File(cl.getOptionValue(XSD_FILE_OPTION)));
			generator.setTargetFilename(new File(cl.getOptionValue(TARGET_FILE_OPTION)));
			
			// Optional parameters
			if( cl.hasOption(DATABASE_TYPE_OPTION) )
			{
				generator.setDatabaseType( DATATBASE_TYPE.valueOf( cl.getOptionValue(DATABASE_TYPE_OPTION) ) );
			}
			
			generator.generateScript();
		}
		catch (ParseException e)
		{
			showUsage(options);
			System.exit(0);
		}
	}
	
	public static void  showUsage( Options options )
	{
		HelpFormatter f = new HelpFormatter();
	    f.printHelp("A utility that parses an XSD file defining your entity domains and generates a roo file which builds RESTful webservice for your entities using Spring Roo:", options);
	}
}
