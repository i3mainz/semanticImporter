package de.hsmainz.cs.semgis.importer.geoimporter.parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.XSD;
import org.json.JSONArray;
import org.json.JSONObject;
import org.wololo.jts2geojson.GeoJSONReader;

public class GeoJSONParser {

	public static final String NSGEO = "http://www.opengis.net/ont/geosparql#";
	public static final String NSSF = "http://www.opengis.net/ont/sf#";
	public static final String GML = "gml";
	public static final String WKT = "asWKT";
	public static final String ASGML = "asGML";
	public static final String WKTLiteral = "wktLiteral";
	public static final String NAME = "name";
	public static final String TYPE = "type";
	public static final String GMLLiteral = "gmlLiteral";

	private static final String TRUE = "true";

	private static final String FALSE = "false";

	private static final String POINT = "Point";

	private static final String HTTP = "http://";
	
	private static final String HTTPS = "https://";

	
	private OntModel model;
	
	private JSONObject geojson;
	
	static OntClass geometryCls;
	
	static OntClass feature;
	
	static GeoJSONReader reader = new GeoJSONReader();
	
	static final SimpleDateFormat parserSDF1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	static final SimpleDateFormat parserSDF2 = new SimpleDateFormat("yyyy-MM-dd");

	public GeoJSONParser(OntModel model,JSONObject geojson) {
		this.model=model;
		this.geojson=geojson;
	}
	
	public static void addGeoSPARQLVocab(OntModel model) {
		OntClass spatialObject=model.createClass("http://www.opengis.net/ont/geosparql#SpatialObject");
		feature=model.createClass("http://www.opengis.net/ont/geosparql#Feature");
		geometryCls=model.createClass("http://www.opengis.net/ont/geosparql#Geometry");
		feature.addSuperClass(spatialObject);
		geometryCls.addSuperClass(spatialObject);
	}
	
	private static Literal determineLiteralType(String literal,OntModel model) {
		try {
			if (StringUtils.isNumeric(literal) && !literal.contains(".")) {
				return model.createTypedLiteral(Integer.valueOf(literal), XSD.xint.getURI());
			} else if (StringUtils.isNumeric(literal) && literal.contains(".")) {
				Double d = Double.valueOf(literal);
				return model.createTypedLiteral(d, XSD.xdouble.getURI());
			}
		} catch (Exception e) {

		}
		// System.out.println("DETERMINE LITERAL TYPE - "+literal);
		try {
			Date date = parserSDF1.parse(literal);
			// System.out.println("DETERMINE LITERAL TYPE DATE? "+date);
			if (date != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				String dateStr = sdf.format(date);
				return model.createTypedLiteral(dateStr, XSD.dateTime.getURI());
			}
		} catch (Exception e) {

		}
		try {
			Date date = parserSDF2.parse(literal);
			// System.out.println("DETERMINE LITERAL TYPE DATE? "+date);
			if (date != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
				String dateStr = sdf.format(date);
				return model.createTypedLiteral(dateStr, XSD.date.getURI());
			}
		} catch (Exception e) {

		}
		if (TRUE.equals(literal) || FALSE.equals(literal)) {
			return model.createTypedLiteral(Boolean.valueOf(literal), XSD.xboolean.getURI());
		} else {
			return model.createTypedLiteral(literal, XSD.xstring.getURI());
		}

	}
	
	public static OntModel geoJSONToTTL(JSONObject geojson,String namespace,String owlclass, String idatt) {
		OntModel result=ModelFactory.createOntologyModel();
		addGeoSPARQLVocab(result);
		OntClass curclass=result.createClass(namespace+owlclass);
		curclass.addSuperClass(feature);
		JSONArray features=geojson.getJSONArray("features");
		for(int i=0;i<features.length();i++) {
			JSONObject geometry=features.getJSONObject(i).getJSONObject("geometry");
			JSONObject properties=features.getJSONObject(i).getJSONObject("properties");
			String fid="";
			if(idatt==null) {
				fid=namespace+UUID.randomUUID();
			}else {
				fid=namespace+properties.get(idatt).toString();
			}
			Individual ind=curclass.createIndividual(fid);
			Individual geomind=geometryCls.createIndividual(fid+"_geom");
			ind.addProperty(result.createObjectProperty("http://www.opengis.net/ont/geosparql#hasGeometry"), geomind);			
			org.locationtech.jts.geom.Geometry wktgeom = reader.read(geometry.toString());
			geomind.addProperty(result.createDatatypeProperty("http://www.opengis.net/ont/geosparql#asWKT"), result.createTypedLiteral(wktgeom.toText(),"http://www.opengis.net/ont/geosparql#wktLiteral"));
			for(String key:properties.keySet()) {
				if(key.equals(idatt))
					continue;
				boolean objprop=false;
				if(properties.get(key).toString().contains(HTTP) || properties.get(key).toString().contains(HTTPS)) {
					objprop=true;
				}
				if(key.contains(HTTP) || key.contains(HTTPS)) {
					if(objprop) {
						ind.addProperty(result.createObjectProperty(key), result.createIndividual(result.createResource(properties.get(key).toString())));
					}else {
						ind.addProperty(result.createDatatypeProperty(key), result.createTypedLiteral(determineLiteralType(properties.get(key).toString(),result)));
					}
				}else {
						ind.addProperty(result.createDatatypeProperty(namespace+key), result.createTypedLiteral(determineLiteralType(properties.get(key).toString(),result)));
				}		
			}
		}
		return result;
	}		
}
