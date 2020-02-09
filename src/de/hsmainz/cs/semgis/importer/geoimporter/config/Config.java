package de.hsmainz.cs.semgis.importer.geoimporter.config;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hsmainz.cs.semgis.importer.geoimporter.Style;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class Config extends DefaultHandler2 {
	
	public Map<String,List<DataColumnConfig>> resultmap=new TreeMap<>();
	
	public String rootClass;
	
	public String indid;	
	
	public String geomatchingclass;
	
	public String geoendpoint;
	
	public String namespace;
	
	public String publisher;
	
	public String timestamp;
	
	public String license;
	
	public String interlinkItem;
	
	public Style pointStyle;
	
	public Style lineStringStyle;
	
	public Style polygonStyle;
	
	public DataColumnConfig currentconfig;

	//If present in shapefiles, shapefile epsg enjoys precendence.
	public Integer epsg;



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
				namespace=attributes.getValue("namespace");
				geomatchingclass=attributes.getValue("geomatchingclass");
				geoendpoint=attributes.getValue("geoendpoint");
				epsg = Integer.valueOf(attributes.getValue("epsg"));
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
		case "column":
				//id
				currentconfig=new DataColumnConfig();
				//TODO read via getAttrValue()

				if(!resultmap.containsKey(attributes.getValue("name"))) {
					resultmap.put(attributes.getValue("name"),new LinkedList<DataColumnConfig>());
				}
				resultmap.get(attributes.getValue("name")).add(currentconfig);
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
				currentconfig.propertyuri=attributes.getValue("propiri");
				currentconfig.keepdataprop=attributes.getValue("keepdataprop");
				if(attributes.getValue("valuemapping")!=null) {
					currentconfig.valuemapping=new TreeMap<String,String>();
					String valmap=attributes.getValue("valuemapping");
					String[] spl=valmap.split(";");
					for(String keyval:spl) {
						currentconfig.valuemapping.put(keyval.substring(0,keyval.indexOf(':')), keyval.substring(keyval.indexOf(':')+1));
					}
				}
				currentconfig.splitposition=attributes.getValue("splitposition");
				currentconfig.splitregex=attributes.getValue("splitregex");
				currentconfig.separationCharacter=attributes.getValue("splitcharacter");
				
				String induri = getAttrValue(attributes, "indname", namespace + currentconfig.name);
				currentconfig.indname= induri.startsWith("http")? induri : namespace + induri;	//TODO proper isRelativeUri test
				
				currentconfig.valueprop=getAttrValue(attributes, "valueprop", "http://qudt.org/schema/qudt#value"); 
				currentconfig.unitprop=getAttrValue(attributes, "unitprop", "http://qudt.org/schema/qudt#unit"); 
		}
	}
	
	
	
}
