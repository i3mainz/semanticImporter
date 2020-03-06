package de.hsmainz.cs.semgis.importer.geoimporter.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

import de.hsmainz.cs.semgis.util.Tuple;

public class DataColumnConfig {

	@Override
	public String toString() {
		return "DataColumnConfig [isCollection=" + isCollection + ", subconfigs=" + subconfigs + ", isSubConfig="
				+ isSubConfig + ", name=" + name + ", queryString=" + queryString + ", endpoint=" + endpoint
				+ ", resultvar=" + resultvar + ", indname=" + indname + ", concept=" + concept + ", prop=" + prop
				+ ", propertyuri=" + propertyuri + ", unit=" + unit + ", unitprop=" + unitprop + ", range=" + range
				+ ", valueprop=" + valueprop + ", keepdataprop=" + keepdataprop + ", isind=" + isind
				+ ", separationCharacter=" + separationCharacter + ", splitposition=" + splitposition + ", splitregex="
				+ splitregex + ", valuemapping=" + valuemapping + "]";
	}

	public Boolean isCollection=false;
	
	public List<DataColumnConfig> subconfigs;
	
	public Boolean isSubConfig=false;
	
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
	public Set<String> propertyuri;
	
	public String unit; // -> unittype
	public String unitprop;

	public String range; // valuetype
	public String valueprop;
	
	public String language;
	
	public String keepdataprop;

	public String isind;	//TODO??
	
	public String separationCharacter;

	public String splitposition;

	public String splitregex;
	
	public Map<String,List<String>> valuemapping;

	public String valueprefix=null;
	
	public String valuesuffix=null;

	public String staticvalue;
	
	
}

