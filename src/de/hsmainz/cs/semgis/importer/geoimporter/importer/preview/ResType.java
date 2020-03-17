package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.sis.referencing.CRS;
import org.apache.sis.referencing.crs.DefaultGeographicCRS;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.data.FeatureStoreUtilities;
import org.geotoolkit.feature.Feature;
import org.geotoolkit.feature.FeatureBuilder;
import org.geotoolkit.feature.FeatureTypeBuilder;
import org.geotoolkit.feature.Property;
import org.geotoolkit.feature.simple.SimpleFeature;
import org.geotoolkit.feature.simple.SimpleFeatureType;
import org.geotoolkit.feature.type.FeatureType;
import org.geotoolkit.feature.type.PropertyDescriptor;
import org.geotoolkit.geometry.jts.JTS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.FactoryException;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;


/**
 * The resulttypes of the SemanticGIS workbench.
 * @author timo.homburg
 *
 */
public enum ResType {
	CSV("csv","http://semgis.de#format","http://semgis.de#geodata",new CSVImporter()),
	GEOJSON("geojson","http://semgis.de#format",null,new GeoJSONImporter()),
	/*GML("gml","http://semgis.de#format",null,new GMLImporter()),
	GML2("gml2","http://semgis.de#format",null,new GMLImporter()),
	GML3("gml3","http://semgis.de#format","http://www.opengis.net/gml#",new GMLImporter()),
	GML32("gml32","http://semgis.de#format","http://www.opengis.net/gml/3.2#GML32",new GMLImporter()),*/
	HTML("html","http://semgis.de#format",null,null),
	JSON("json","http://semgis.de#format",null,null),
	//KML("kml","http://semgis.de#format","http://www.opengis.net/kml/2.2",new KMLImporter()),
	SHP("shp","http://semgis.de#format","http://semgis.de#geodata",new SHPImporter()),
	XML("xml","http://semgis.de#format",null,null),
	ZIP(null,"http://semgis.de#format",null,null);
	
	public void setRestype(String restype) {
		this.restype = restype;
	}
	
	private String restype;
	
	private String resURI;
	
	private String namespace;
	
	private ImporterAPI importer;


	private ResType(String restype,String resuri,String namespace,ImporterAPI importer){
		this.restype=restype;
		this.namespace=namespace;
		this.importer=importer;
		this.resURI=resuri;
	}
	
	public String getRestype() {
		return restype;
	}
	
	public String getURI(){
		return this.resURI+"/"+this.restype;
	}
	
	public ImporterAPI getImporter() {
		return importer;
	}
		
	
	/*public static String exportHTML(FeatureCollection collection,ResType restype,Boolean geometry,
			CoordinateReferenceSystem sourceCRS,CoordinateReferenceSystem targetCRS) throws IOException{
  		return export(null,null,collection,restype,null,(SimpleFeatureType) collection.getFeatureType(),null,((GeometryType)collection.getFeatureType()).getCoordinateReferenceSystem(),targetCRS,geometry);
	}*/
	
	public static List<String> exportConceptNames(FeatureCollection collection){
		Map<Integer,Set<String>> resMap=new TreeMap<Integer,Set<String>>();
		FeatureIterator iter=collection.iterator();
		int i=0;
		for(PropertyDescriptor desc:((SimpleFeatureType)collection.getFeatureType()).getAttributeDescriptors()){
			resMap.put(i++, new TreeSet<String>());
			resMap.get(i-1).add(desc.getName().toString());
		}
		while(iter.hasNext()){
			Feature feature=iter.next();
			i=0;			
			for(Property prop:feature.getProperties()){
				//if(!prop.getDescriptor().equals(((AttributeType)collection.getSchema()).getGeometryDescriptor())){
					if(prop.getName()!=null)
						resMap.get(i).add(prop.getName().toString());
					if(prop.getValue()!=null){
						resMap.get(i).add(prop.getValue().toString());
					//}
				}/*else{
				
					resMap.get(i).add(collection.getSchema().getGeometryDescriptor().getType().getName().toString());
				}*/
				i++;
			}
		}
		iter.close();
		List<String> result=new LinkedList<String>();
		for(Set<String> con:resMap.values()){
			if(con.size()<2)
				continue;
			StringBuilder builder=new StringBuilder();
			for(String str:con){
				builder.append(str+";");
			}
			result.add(builder.substring(0, builder.length()-1));
		}
		return result;
	}
	

}
