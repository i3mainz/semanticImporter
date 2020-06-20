package de.hsmainz.cs.semgis.importer.geoimporter.config;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hsmainz.cs.semgis.importer.geoimporter.Style;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;


public class Config extends DefaultHandler2 {
	
	public Map<String,List<DataColumnConfig>> resultmap=new TreeMap<>();
	
	public Map<String,ClassMapping> additionalClasses=new TreeMap<String,ClassMapping>();
	
	public String indid;
	
	public String indidprefix;
	
	public String indidsuffix;	
	
	public String namespace;
	
	public String attnamespace;
	
	public String publisher;
	
	public String timestamp;
	
	public Boolean nometadata=false;
	
	public Boolean ignoreUnresolved=true;
	
	public String indlabellang;
	
	public String classlabellang;
	
	public String proplabellang;
	
	public String license;
	
	public String interlinkItem;
	
	public Boolean nearestMatch=false;
	
	public String rootClass,rootClassLabel;
	
	public String mostGeneralClass;
	
	public Style pointStyle=new Style();
	
	public Style lineStringStyle=new Style();
	
	public Style polygonStyle=new Style();
	
	public DataColumnConfig currentconfig;
	
	public ValueMapping currentValueMapping=new ValueMapping();

	//If present in shapefiles, shapefile epsg enjoys precendence.
	public Integer epsg;
	
	public Integer collectiondepth=0;
	
	public Boolean valueMapping=false;
	
	Stack<List<DataColumnConfig>> columnLists=new Stack<List<DataColumnConfig>>();
	
	public List<DataColumnConfig> addcolumns=new LinkedList<>();
	
	public List<GeoMatching> geomatchings=new LinkedList<>();

	public boolean attachepsg=false;
	
	public String curclass="";


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
				rootClassLabel=attributes.getValue("classlabel");
				indid=attributes.getValue("indid");
				indidprefix=attributes.getValue("indidprefix");
				indidsuffix=attributes.getValue("indidsuffix");
				if(attributes.getValue("nometadata")!=null) {
					nometadata=Boolean.valueOf(attributes.getValue("nometadata"));
				}
				if(attributes.getValue("attachepsg")!=null) {
					attachepsg=Boolean.valueOf(attributes.getValue("attachepsg"));
				}
				classlabellang=attributes.getValue("classlabellang");
				indlabellang=attributes.getValue("indlabellang");
				proplabellang=attributes.getValue("proplabellang");
				namespace=attributes.getValue("namespace");
				attnamespace=attributes.getValue("attnamespace");
				epsg = Integer.valueOf(attributes.getValue("epsg"));
				break;
		case "columncollection":
			  System.out.println("ColumnCollection "+this.columnLists.size());
			  currentconfig=new DataColumnConfig();
			  currentconfig.isCollection=true;
			  currentconfig.subconfigs=new LinkedList<>();
			  currentconfig.concept=attributes.getValue("class");
			  currentconfig.propertyuri.put(attributes.getValue("propiri"),new TreeMap<>());
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
			  if(currentconfig.valuemapping!=null && !currentconfig.valuemapping.containsKey(attributes.getValue("from"))) {
				  currentconfig.valuemapping.put(attributes.getValue("from"),new LinkedList<>());
			  }
			  valueMapping=true;
			  this.currentValueMapping=new ValueMapping();
			  this.currentValueMapping.from=attributes.getValue("from");
			  this.currentValueMapping.to=attributes.getValue("to");
			  this.currentValueMapping.propiri=attributes.getValue("propiri");
			  currentconfig.valuemapping.get(attributes.getValue("from")).add(this.currentValueMapping);
			  break;
		case "geomatching":
			  GeoMatching match=new GeoMatching();
			  match.geoendpoint=attributes.getValue("geoendpoint");
			  match.geoquery=attributes.getValue("geoquery");
			  match.geomatchingclass=attributes.getValue("geomatchingclass");
			  match.level1=Boolean.valueOf(attributes.getValue("level1"));
			  match.level1Query=attributes.getValue("level1");
			  match.level1endpoint=attributes.getValue("level1endpoint");
			  this.geomatchings.add(match);
			  break;
		case "clslabel":
			  if(!curclass.isEmpty()) {
				  this.additionalClasses.get(curclass).labels.put(attributes.getValue("lang"),attributes.getValue("value"));
			  }
			  break;
		case "proplabel":
			  if(currentconfig!=null && currentconfig.propertyuri!=null) {
				  String key=currentconfig.propertyuri.keySet().iterator().next();
				  if(currentconfig.propertyuri.get(key)==null) {
					  currentconfig.propertyuri.put(key,new TreeMap<>());
				  }
				  currentconfig.propertyuri.get(key).put(attributes.getValue("lang"),attributes.getValue("value"));
			  }
			  break;
		case "rootclass":
			  this.mostGeneralClass=attributes.getValue("class");
			  break;
		case "classmapping":
			  if(additionalClasses!=null && attributes.getValue("class")!=null) {
				  ClassMapping mapp=new ClassMapping();
				  mapp.iri=attributes.getValue("class");
				  curclass=mapp.iri;
				  if(attributes.getValue("superclass")!=null) {
					  mapp.superclass=attributes.getValue("superclass");				  
				  }
				  additionalClasses.put(attributes.getValue("class"),mapp);
			  }
			  break;
		case "addcolumn":
			  if(valueMapping) {
				  currentconfig=new DataColumnConfig();
				  currentconfig.propertyuri.put(attributes.getValue("propiri"),new TreeMap<>());
				  currentconfig.staticvalue=attributes.getValue("value");
				  currentconfig.prop=attributes.getValue("prop");
				  currentconfig.concept=attributes.getValue("concept");
				  currentconfig.namespace=attributes.getValue("namespace");
			  }else {
				  DataColumnConfig dconfig=new DataColumnConfig();
				  if(attributes.getValue("propiri")!=null)
					  dconfig.propertyuri.put(attributes.getValue("propiri"),new TreeMap<>());
				  dconfig.staticvalue=attributes.getValue("value");
				  dconfig.prop=attributes.getValue("prop");
				  dconfig.concept=attributes.getValue("concept");
				  if(this.currentValueMapping==null) {
					  this.currentValueMapping=new ValueMapping();
				  }
				  this.addcolumns.add(dconfig);
			  }
			  break;
		case "column":
				//id
				currentconfig=new DataColumnConfig();
				//TODO read via getAttrValue()
				  System.out.println(this.columnLists.size());
				System.out.println(resultmap);
				if(attributes.getValue("name")!=null && !resultmap.containsKey(attributes.getValue("name"))) {
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
				if(attributes.getValue("ignoreUnresolved")!=null) {
					currentconfig.ignoreUnresolved=Boolean.valueOf(attributes.getValue("ignoreUnresolved"));
				}
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
				currentconfig.namespace=attributes.getValue("namespace");
				if(attributes.getValue("propiri")!=null)
					currentconfig.propertyuri.put(attributes.getValue("propiri"),new TreeMap<>());
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
		if(qName.equals("valuemapping")) {
			valueMapping=false;
		}
		if(qName.equals("classmapping")) {
			curclass="";
		}
	}
		
}

