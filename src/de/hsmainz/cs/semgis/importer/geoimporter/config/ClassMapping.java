package de.hsmainz.cs.semgis.importer.geoimporter.config;

import java.util.Map;
import java.util.TreeMap;

/**
 * Describes a classmapping used in the DataColumn configuration.
 *
 */
public class ClassMapping {
	/** The IRI of the class.*/
	public String iri;
	/** The IRI of a superclass.*/
	public String superclass;
	/** A map of language to labels for the class.*/
	public Map<String,String> labels=new TreeMap<>();
	
}
