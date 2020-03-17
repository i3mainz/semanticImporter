package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.simple.SimpleFeature;
import org.geotoolkit.feature.type.GeometryType;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.xml.sax.SAXException;


import org.apache.jena.iri.IRI;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.iterator.ExtendedIterator;


public abstract class ImporterAPI extends Importer {
	/**
	 * The current OntModel to consider.
	 */
	private static OntModel model;
	
	public static String currentTable;
	
	public ImporterAPI(String modelpath){

	}
	
	public ImporterAPI(InputStream stream){

	}
	
	public ImporterAPI(){
		
	}


	
	/**
	 * Gets source individuals for all classes.
	 * @param user the user who owns the ontology
	 * @param modelpath the modelpath of the ontology
	 * @return the sources strings of the classes
	 */
	public static String[] getSources2(String user, String modelpath){
		StringBuilder queryBuilder=new StringBuilder();
		queryBuilder.append("SELECT DISTINCT ?propertyAttribute ?classid ?individualid ?sourceType ?sourceURL \n"
				+ "WHERE {\n ?class <http://semgis.de/geodata#sources2> ?sources .\n"
				+ "?sources <http://semgis.de/datasource#propertyAttribute> ?propertyAttribute .\n"
				+" ?sources <http://semgis.de/datasource#classID> ?classid .\n"
				+" ?sources <http://semgis.de/datasource#individualID> ?individualid .\n"+
				" ?sources <http://semgis.de/datasource#sourceType> ?sourceType .\n"+
				" ?sources <http://semgis.de/datasource#sourceURL> ?sourceURL .\n"+"}\n");
		System.out.println("GetSourcesQuery: "+queryBuilder.toString());
		QueryExecution qe = QueryExecutionFactory.create(queryBuilder.toString(), model);
		ResultSet results =  qe.execSelect();
		List<String> resultss=new LinkedList<String>();
		Map<String,Set<String>> resultSet=new TreeMap<String,Set<String>>();
		while(results.hasNext()){
			QuerySolution res=results.next();
			String propertyAttribute=res.get("?propertyAttribute").toString().substring(0,res.get("?propertyAttribute").toString().indexOf("^^"));
			String individualid=res.get("?individualid").toString().substring(0,res.get("?individualid").toString().indexOf("^^"));
			String sourceType=res.get("?sourceType").toString().substring(0,res.get("?sourceType").toString().indexOf("^^"));
			String sourceURL=res.get("?sourceURL").toString().substring(0,res.get("?sourceURL").toString().indexOf("^^"));
			String classid=res.get("?classid").toString().substring(0,res.get("?classid").toString().indexOf("^^"));
			if(!resultSet.containsKey(sourceType+"_"+sourceURL+","+classid+","+individualid)){
				resultSet.put(sourceType+"_"+sourceURL+","+classid+","+individualid, new TreeSet<String>());
			}
			resultSet.get(sourceType+"_"+sourceURL+","+classid+","+individualid).add(propertyAttribute);
			System.out.println("PropAtt: "+propertyAttribute+" Classid: "+classid+" Individualid: "+individualid+" SourceType: "+sourceType+" SourceURL: "+sourceURL);
			//resultss.append(sourceType+"_"+sourceURL+","+classid+","+individualid+",")
		}
		qe.close();
		System.out.println(resultSet.toString());
		String[] resultt=new String[resultSet.keySet().size()];
		int i=0;
		for(String key:resultSet.keySet()){
			System.out.println(i);
			resultt[i++]=key+","+resultSet.get(key).toString().replace(",", ";");
		}
		System.out.println(Arrays.toString(resultt));
		return resultt;
	}
	

	
	public static List<String> previewSources(URL uri,ResType restype) throws IOException, SAXException, ParserConfigurationException, DataStoreException{
		ImporterAPI api=null;
		FeatureCollection collection=null;
		switch(restype){
		case CSV:
			api=new CSVImporter();
			collection=api.getFeatureCollection(uri, restype);
			break;
		/*case GML:
		case GML3:
			api=new GMLImporter();
			collection=api.getFeatureCollection(uri, restype);
			break;*/
		case GEOJSON:
			api=new GeoJSONImporter();
			collection=api.getFeatureCollection(uri, restype);
			break;
		/*case KML:
			api=new KMLImporter();
			collection=api.getFeatureCollection(uri, restype);
			break;*/
		case SHP:
			api=new SHPImporter();
			collection=api.getFeatureCollection(uri, restype);
			break;
		case ZIP:
			api=new ZIPImporter();
			collection=api.getFeatureCollection(uri, restype);
		default:
			return new LinkedList<String>();
		}
		StringBuilder builder=new StringBuilder();
		StringBuilder builder2=new StringBuilder();
		Set<String> keywords=new TreeSet<String>();
		if(collection!=null && collection.getFeatureType()!=null){
			Feature feat=collection.iterator().next();
			builder2.append(feat.getDefaultGeometryProperty().getName()+",");
			for(Property prop:feat.getProperties()){
				if(!(prop.getName().equals(feat.getDefaultGeometryProperty().getName()))){
					builder.append(prop.getName()+",");
				}
			}
				/*feat.getDefaultGeometryProperty().getName()
				if(!(prop instanceof GeometryDescriptor)){
					builder2.append(prop.getName()+",");
				}*/
				
			
		}
		List<String> result=new LinkedList<String>();
		//result.add(ResType.exportHTML(collection, ResType.HTML,false,((GeometryType)collection.getFeatureType()).getCoordinateReferenceSystem()
		//		,((GeometryType)collection.getFeatureType()).getCoordinateReferenceSystem()));
		//result.add(ResType.exportHTML(collection, ResType.GEOJSON,true,
		//		((GeometryType)collection.getFeatureType()).getCoordinateReferenceSystem(),(CoordinateReferenceSystem) GeographicCRS.WGS84,language));
		if(builder.length()>0)
			result.add(builder.substring(0,builder.length()-1));
		if(builder2.length()>0)
			result.add(builder2.substring(0,builder2.length()-1));
		result.addAll(ResType.exportConceptNames(collection));
		return result;
	}
	
	public static String getSelectLanguages(String language,String displayLanguage,Boolean longOrShort)
	{
		Locale locale;
		if(displayLanguage.contains("_")){
			locale=new Locale(displayLanguage.split("_")[0],displayLanguage.split("_")[1]);
		}else{
			locale=new Locale(displayLanguage.split("_")[0]);
		}
		StringBuilder languages=new StringBuilder();
		Set<String> languageStr=new TreeSet<String>();
		for(Locale loc:SimpleDateFormat.getAvailableLocales()){
			if(longOrShort)
				languageStr.add(loc.toString());
			else
				languageStr.add(loc.getLanguage());
			//BrokerOntologyConnection.log.debug(loc.toString());
		}
		for(String lan:languageStr){
			if(lan.trim().isEmpty())
				continue;
			languages.append("<option value=\"");
			languages.append(lan.toUpperCase()+"\"");
			//BrokerOntologyConnection.log.debug(lan.toUpperCase()+" - "+language.toUpperCase());
			if(language!=null && lan.toUpperCase().contains(language.toUpperCase())){
				languages.append(" selected=\"selected\">");
			}else{
				languages.append(">");
			}
			String[] spl=lan.split("_");
			if(longOrShort){
				if(spl.length>1)
					languages.append(new Locale(spl[0],spl[1]).getDisplayLanguage(locale)+" ("+new Locale(spl[0],spl[1]).getDisplayCountry(locale)+")");
				else
					languages.append(new Locale(spl[0]).getDisplayLanguage(locale));
			}else{
				languages.append(lan);				
			}

			languages.append("</option>");
		}
		return languages.toString();
	}
	

	protected abstract Boolean checkIfAvailable(URL path);
	
	protected List<String> getTables(URL path) throws IOException{
		return null;
	}
	
	public abstract org.geotoolkit.data.FeatureCollection getFeatureCollection(URL path, ResType restype) throws IOException, SAXException, ParserConfigurationException, DataStoreException;
	


	/*public static void main(String[] args) throws MalformedURLException, IOException, SAXException, ParserConfigurationException{
		//ImporterAPI.previewSources(new URL("http://tzmz.service24.rlp.de/cgi-bin/mapserv?map=/db/tz_web/geoportal/mapserver/mapfiles/dlr_dlrgrenzen_wfs.map&REQUEST=getCapabilities&VERSION=1.0.0&SERVICE=WFS"), ResType.WFS1);
		//BrokerOntologyConnection.log.debug(BabelAPIConnection.getBabelSynsets(Language.EN, "tree"));
		//ImporterAPI.previewSources(new File("baum_region.geojson").toURL(), ResType.GEOJSON);
		//ImporterAPI.previewSources(new File("hospital.xlsx").toURL(), ResType.XLSX,"DE");
		//BrokerOntologyConnection.log.debug(ImporterAPI.getSelectLanguages("de_DE", "zh_CN", true));
		//ImporterAPI.getAvailableFileEncodings();
		//BrokerOntologyConnection.log.debug(ImporterAPI.getKnownRanges());
		ImporterAPI.getSources2("", "abc.owl");
	}*/
}
