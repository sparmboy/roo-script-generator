/**
 * 
 */
package com.sarm.utils.spring.roo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sarm.utils.spring.roo.RooScriptGenerator.CARDINALITY;

/**
 * Represents a field to be added to an entity in roo
 * 
 * @author Spencer
 * 
 */
public class RooField
{
	
	Logger logger = LoggerFactory.getLogger(RooField.class);
	
	/**
	 * The cardinality of this field within the owning entity
	 */
	CARDINALITY cardinality;
	
	/**
	 * the name of the field
	 */
	String fieldName;

	/**
	 * The XSD type of this field
	 */
	String xsdType;

	
	/**
	 * The roo type of this field
	 */
	String rooType;

	/**
	 * The entity owning this field
	 */
	String owningEntity;

	/**
	 * Whether this field can be null
	 */
	Boolean notNull;

	/**
	 * Whether this value must be null
	 */
	Boolean nullRequired;

	/**
	 * The BigDecimal string-based representation of the minimum value
	 */
	Double decimalMin;

	/**
	 * The BigDecimal string based representation of the maximum value
	 */
	Double decimalMax;

	/**
	 * The minimum integer size
	 */
	Integer min;

	/**
	 * The maximum integer size
	 */
	Integer max;

	
	/**
	 * The minimum string length
	 */
	Integer sizeMin;

	/**
	 * The maximum string length
	 */
	Integer sizeMax;

	/**
	 * The required regular expression pattern
	 */
	String regexp;

	/**
	 * Inserts an optional Spring @Value annotation with the given content
	 */
	Object value;

	/**
	 * An optional comment for JavaDocs
	 */
	String comment;

	/**
	 * Indicates to mark the field as transient
	 */
	Boolean tranzient;

	/**
	 * Indicates whether to mark the field with a unique constraint
	 */
	Boolean unique;

	/**
	 * Indicates that this field is a Large Object
	 */
	Boolean lob;
	
	@Override
	public String toString()
	{
		return "field " +
				rooType + " " +
				"--fieldName " + fieldName  + " " +
				"--class " + owningEntity + " " + 
				(unique != null && unique == true ? "--unique " : "") +
				(notNull != null && notNull == true ? "--notNull " : "") +
				(nullRequired != null && nullRequired == true ? "--nullRequired " : "") +
				(regexp != null ? "--regexp " + regexp + " " : "") +
				(sizeMin != null ? "--sizeMin " + sizeMin + " " : "") +
				(sizeMax != null ? "--sizeMax " + sizeMax + " " : "") +
				(min != null ? "--min " + min + " " : "") +
				(max != null ? "--max " + max + " " : "") +
				(tranzient != null && tranzient == true ? "--transient " : "") +
				(value != null ? "--value " + value + " " : "") +
				(lob != null ? "--lob " : "");
		
		
	}

}
