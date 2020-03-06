package de.hsmainz.cs.semgis.importer.geoimporter.config;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hsmainz.cs.semgis.importer.geoimporter.Style;
import de.hsmainz.cs.semgis.util.Tuple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;


public class Config extends DefaultHandler2 {
	
	public Map<String,List<DataColumnConfig>> resultmap=new TreeMap<>();
	
	public String rootClass;
	
	public Set<String> additionalClasses=new TreeSet<String>();
	
	public String indid;
	
	public String indidprefix;
	
	public String indidsuffix;	
	
	public String geomatchingclass;
	
	public String geoendpoint;
	
	public String namespace;
	
	public String attnamespace;
	
	public String publisher;
	
	public String timestamp;
	
	public String license;
	
	public String interlinkItem;
	
	public Boolean nearestMatch=false;
	
	public Style pointStyle=new Style();
	
	public Style lineStringStyle=new Style();
	
	public Style polygonStyle=new Style();
	
	public DataColumnConfig currentconfig;

	//If present in shapefiles, shapefile epsg enjoys precendence.
	public Integer epsg;
	
	public Integer collectiondepth=0;
	
	Stack<List<DataColumnConfig>> columnLists=new Stack<List<DataColumnConfig>>();



	public static String getAttrValue(Attributes attr, String qName, String defaultVal)
	{
		String val = attr.getValue(qName);
		return (null != val)? val : defaultVal;
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// TODO Auto-generated method stub
		super.startElement(uri, localName, qName, attributes);
		switch(qName) {
		case "file":
				rootClass=attributes.getValue("class");
				indid=attributes.getValue("indid");
				indidprefix=attributes.getValue("indidprefix");
				indidsuffix=attributes.getValue("indidsuffix");
				namespace=attributes.getValue("namespace");
				attnamespace=attributes.getValue("attnamespace");
				geomatchingclass=attributes.getValue("geomatchingclass");
				geoendpoint=attributes.getValue("geoendpoint");
				epsg = Integer.valueOf(attributes.getValue("epsg"));
				break;
		case "columncollection":
			  System.out.println("ColumnCollection "+this.columnLists.size());
			  currentconfig=new DataColumnConfig();
			  currentconfig.isCollection=true;
			  currentconfig.subconfigs=new LinkedList<>();
			  currentconfig.concept=attributes.getValue("class");
			  currentconfig.propertyuri.add(attributes.getValue("propiri"));
			  currentconfig.name=attributes.getValue("name");
			  if(this.columnLists.isEmpty()) {
				  System.out.println("Top Level Collection: Adding "+attributes.getValue("name"));
				  if(!resultmap.containsKey(attributes.getValue("name"))) {
					 resultmap.put(attributes.getValue("name"),new LinkedList<DataColumnConfig>());
				  }
				  resultmap.get(attributes.getValue("name")).add(currentconfig);
			  }else {
				  this.columnLists.peek().add(currentconfig);
			  }
			  this.columnLists.push(currentconfig.subconfigs);
			  System.out.println(this.columnLists.peek());
			  System.out.println(this.columnLists.size());
			  break;
		case "mapping":
			  break;
		case "metadata":
			  publisher=attributes.getValue("publisher");
			  license=attributes.getValue("license");
			  timestamp=attributes.getValue("timestamp");
			  break;
		case "point":		  
			  this.pointStyle.border=attributes.getValue("border");
			  this.pointStyle.borderColor=attributes.getValue("borderColor");
			  this.pointStyle.size=attributes.getValue("size");
			  this.pointStyle.fillColor=attributes.getValue("fillColor");
			  break;
		case "polygon":
			  this.polygonStyle.border=attributes.getValue("border");
			  this.polygonStyle.borderColor=attributes.getValue("borderColor");
			  this.polygonStyle.size=attributes.getValue("size");
			  this.polygonStyle.fillColor=attributes.getValue("fillColor");
			  break;
		case "linestring":
			  this.lineStringStyle.border=attributes.getValue("border");
			  this.lineStringStyle.borderColor=attributes.getValue("borderColor");
			  this.lineStringStyle.size=attributes.getValue("size");
			  this.lineStringStyle.fillColor=attributes.getValue("fillColor");
			  break;
		case "valuemapping":
			  if(!currentconfig.valuemapping.containsKey(attributes.getValue("from"))) {
				  currentconfig.valuemapping.put(attributes.getValue("from"),new LinkedList<>());
			  }
			  currentconfig.valuemapping.get(attributes.getValue("from")).add(attributes.getValue("to"));
			  break;
		case "propiri":
			  currentconfig.propertyuri.add(attributes.getValue("value"));
			  break;
		case "classmapping":
			  additionalClasses.add(attributes.getValue("class"));
			  break;
		case "addcolumn":
			  currentconfig=new DataColumnConfig();
			  currentconfig.propertyuri.add(attributes.getValue("propiri"));
			  currentconfig.staticvalue=attributes.getValue("value");
		case "column":
				//id
				currentconfig=new DataColumnConfig();
				//TODO read via getAttrValue()
				  System.out.println(this.columnLists.size());
				  
				if(!resultmap.containsKey(attributes.getValue("name"))) {
					resultmap.put(attributes.getValue("name"),new LinkedList<DataColumnConfig>());
				}
				if(columnLists.isEmpty()) {
					resultmap.get(attributes.getValue("name")).add(currentconfig);
				}else {
					columnLists.peek().add(currentconfig);
					System.out.println(this.columnLists.peek());
				}
				for(String key:resultmap.keySet()) {
					System.out.println(key+" - "+resultmap.get(key));
				}
				currentconfig.name=attributes.getValue("name");
				//resolve queries
				currentconfig.queryString=attributes.getValue("query");
				currentconfig.resultvar=attributes.getValue("resultvar");
				currentconfig.endpoint=attributes.getValue("endpoint");
				//matching
				currentconfig.prop=attributes.getValue("prop");
				currentconfig.concept=attributes.getValue("concept");
				currentconfig.unit=attributes.getValue("unit");
				currentconfig.range=attributes.getValue("range");
				currentconfig.isind=attributes.getValue("isind");
				currentconfig.language=attributes.getValue("language");
				currentconfig.propertyuri.add(attributes.getValue("propiri"));
				currentconfig.keepdataprop=attributes.getValue("keepdataprop");
				currentconfig.valueprefix=attributes.getValue("valueprefix");
				currentconfig.valuesuffix=attributes.getValue("valuesuffix");
				/*if(attributes.getValue("valuemapping")!=null) {
					currentconfig.valuemapping=new TreeMap<String,String>();
					String valmap=attributes.getValue("valuemapping");
					String[] spl=valmap.split(";");
					for(String keyval:spl) {
						currentconfig.valuemapping.put(keyval.substring(0,keyval.indexOf(':')),
								keyval.substring(keyval.indexOf(':')+1));
					}
				}*/
				currentconfig.splitposition=attributes.getValue("splitposition");
				currentconfig.splitregex=attributes.getValue("splitregex");
				currentconfig.separationCharacter=attributes.getValue("splitcharacter");
				
				String induri = getAttrValue(attributes, "indname", namespace + currentconfig.name);
				if(attnamespace!=null) {
					currentconfig.indname= induri.startsWith("http")? induri : namespace + induri;	//TODO proper isRelativeUri test
				}else {
					currentconfig.indname= induri.startsWith("http")? induri : attnamespace + induri;	//TODO proper isRelativeUri test
				}

				
				currentconfig.valueprop=getAttrValue(attributes, "valueprop", "http://qudt.org/schema/qudt#value"); 
				currentconfig.unitprop=getAttrValue(attributes, "unitprop", "http://qudt.org/schema/qudt#unit"); 
		}
	}
	
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(qName.equals("columncollection")) {
			System.out.println("Before pop: "+this.columnLists.size());
			this.columnLists.pop();
			System.out.println("After pop: "+this.columnLists.size());
		}
	}
		
}

