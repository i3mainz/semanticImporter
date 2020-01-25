package de.hsmainz.cs.semgis.importer.geoimporter.config;

import java.util.Map;

public class DataColumnConfig {

	//id
	public String name;
	
	//query
	public String queryString;
	public String endpoint;
	public String resultvar;
	
	//matching
	public String indname; 
	
	public String concept;	// -> objtype
	public String prop;	// -> proptype
	public String propertyuri;
	
	public String unit; // -> unittype
	public String unitprop;

	public String range; // valuetype
	public String valueprop;

	public String isind;	//TODO??
	
	public String separationCharacter;

	public String splitposition;

	public String splitregex;
	
	public Map<String,String> valuemapping;
	
	
}

