package de.hsmainz.cs.semgis.importer.geoimporter.importer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.impl.AnnotationPropertyImpl;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.XSD;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.wololo.geojson.GeoJSON;
import org.wololo.jts2geojson.GeoJSONWriter;

import de.hsmainz.cs.semgis.importer.geoimporter.config.Config;
import de.hsmainz.cs.semgis.importer.geoimporter.config.DataColumnConfig;
import de.hsmainz.cs.semgis.importer.geoimporter.connector.TripleStoreConnector;
import de.hsmainz.cs.semgis.importer.geoimporter.connector.TripleStoreConnector.GeographicalResult;


public class DataImporter {
	public static TripleStoreConnector dbConnector = new TripleStoreConnector("prefix.txt");

	public static final String DEFAULTNAMESPACE = "http://www.geomer.de/geodata/";

	private final Config config;
	private final OntModel model;
	private OntClass rootClass;
	private final OntClass geomClass;
	private final OntClass featurecl;
	private final Integer epsgCode;
	private Date startTime;
	GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("CEST"));

	public DataImporter(Config config, Integer epsgCode, String geomType) {
		this.config = config;
		this.model = ModelFactory.createOntologyModel();
		OntClass geometry = model.createClass("http://www.opengis.net/ont/geosparql#Geometry");
		OntClass spatial = model.createClass("http://www.opengis.net/ont/geosparql#SpatialObject");
		featurecl = model.createClass("http://www.opengis.net/ont/geosparql#Feature");
		featurecl.addSuperClass(spatial);
		geometry.addSuperClass(spatial);
		if(!config.rootClass.contains("%%")) {
			this.rootClass = model.createClass(config.rootClass);
			rootClass.addSuperClass(featurecl);
		}
		this.epsgCode = epsgCode;
		this.geomClass = model.createClass("http://www.opengis.net/ont/sf#" + geomType);
		startTime=new Date(System.currentTimeMillis());


	}

	public static String toCamelCase(String s) {
		return Character.toUpperCase(s.charAt(0)) + s.substring(1);
	}
	
	public void importStyle(Individual ind) {
		ObjectProperty hasStyle=model.createObjectProperty(DEFAULTNAMESPACE+"hasStyle");
		OntClass style=model.createClass(DEFAULTNAMESPACE+"Style");
		Individual styleind=style.createIndividual(this.rootClass.getURI()+"_style");
		AnnotationProperty lineStringStyle=model.createAnnotationProperty(DEFAULTNAMESPACE+"lineStringStyle");
		AnnotationProperty pointStyle=model.createAnnotationProperty(DEFAULTNAMESPACE+"pointStyle");	
		AnnotationProperty polygonStyle=model.createAnnotationProperty(DEFAULTNAMESPACE+"polygonStyle");
		if(this.config.lineStringStyle!=null) {
			styleind.addProperty(lineStringStyle, "\"fillColor\":"+this.config.lineStringStyle.fillColor+", \"fill\":true, \"stroke\":true, \"color\":"+"");				
		}

		//styleind.addProperty(p, o)
	}

	public void importToOwl(String outputPath, FeatureCollection collection, String filename) throws IOException {
		Boolean first = true;
		Integer counter = 0;
		try (FeatureIterator<SimpleFeature> features = collection.features()) {
			
			while (features.hasNext()) {
				System.out.println(counter++ + "/" + collection.size());

				// Transfer the current data row to a hash map.
				SimpleFeature feature = features.next();
				Map<String, String> dataRow = new HashMap<String, String>();
				for (Property prop : feature.getProperties()) {
					String value = null != prop.getValue() ? prop.getValue().toString() : null;
					dataRow.put(prop.getName().toString(), value);
				}
				if(config.rootClass.contains("%%")) {
					String s = new StrSubstitutor(dataRow, "%%", "%%").replace(config.rootClass);
					this.rootClass=model.createClass(s);
					rootClass.addSuperClass(featurecl);
				}
				if (first) {
					StringBuilder builder = new StringBuilder();
					for (String propName : dataRow.keySet()) {
						builder.append(propName + ";");
					}
					AnnotationProperty annpro = model.createAnnotationProperty("http://semgis.de/geodata#propOrder");
					rootClass.addProperty(annpro, builder.toString());
					rootClass.addProperty(model.createAnnotationProperty("http://semgis.de/geodata#standard"),
							filename.substring(filename.lastIndexOf('/') + 1));
					first = false;
				}
				// Import root individual.
				Individual currentind;
				if(config.indid==null) {
					currentind=rootClass.createIndividual(config.namespace+counter);
				}else if(config.indid.contains(";")) {
					String[] ids=config.indid.split(";");
					StringBuilder val=new StringBuilder();
					for(String id:ids) {
						val.append(dataRow.get(id));
					}
					currentind=rootClass.createIndividual(config.namespace+val);
					//importMetaData(currentind,val.toString());
				}else {
					currentind = rootClass.createIndividual(config.namespace + dataRow.get(config.indid));
					//importMetaData(currentind,dataRow.get(config.indid));
				}


				// Import geometry column.
				importGeometry(currentind, feature.getDefaultGeometryProperty());

				// Import data columns.
				for (Map.Entry<String, String> property : dataRow.entrySet()) {
					String columnName = property.getKey();
					String value = property.getValue();
					if (config.resultmap.containsKey(columnName)) {
						for (DataColumnConfig curconf : config.resultmap.get(columnName)) {
							if (curconf.separationCharacter != null && curconf.splitposition != null) {
								String[] spl = value.split(curconf.separationCharacter);
								Integer position = 0;
								String toadd = "";
								if (curconf.splitposition.equals("last")) {
									position = spl.length - 1;
									toadd = spl[position];
								} else if (curconf.splitposition.equals("untillast")) {
									for (int i = 0; i < spl.length - 1; i++) {
										toadd += spl[i]+curconf.separationCharacter;
									}
									toadd=toadd.substring(0,toadd.length()-curconf.separationCharacter.length());
								} else {
									position = Integer.valueOf(curconf.splitposition);
									toadd = spl[position];
								}
								importValue(curconf, currentind, dataRow, toadd);
							} else if (curconf.splitregex != null && curconf.splitposition != null) {
								Pattern pattern = Pattern.compile(curconf.splitregex);
								String toadd = pattern.matcher(value).group(Integer.valueOf(curconf.splitposition));
								importValue(curconf, currentind, dataRow, toadd);
							} else {
								importValue(curconf, currentind, dataRow, value);
							}
						}

					} else {
						System.out.println("Property " + columnName + " nicht erfasst!");
					}
				}
			}
		}

		model.write(new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8), "TTL");
	}

	private void importMetaData(Individual ind,String indname) {
		OntClass entity=model.createClass("http://www.w3.org/ns/prov#Entity");
		OntClass agent=model.createClass("http://www.w3.org/ns/prov#Agent");
		OntClass activity=model.createClass("http://www.w3.org/ns/prov#Activity");
		Individual importactivity=activity.createIndividual(DEFAULTNAMESPACE+indname+"_schemaImporter");
		ObjectProperty wasAttributedTo=model.createObjectProperty("http://www.w3.org/ns/prov#wasAttributedTo");
		ObjectProperty wasAssociatedWith=model.createObjectProperty("http://www.w3.org/ns/prov#wasAssociatedWith");
		ObjectProperty wasGeneratedBy=model.createObjectProperty("http://www.w3.org/ns/prov#wasGeneratedBy");
		ObjectProperty used=model.createObjectProperty("http://www.w3.org/ns/prov#used");
		DatatypeProperty startedAtTime=model.createDatatypeProperty("http://www.w3.org/ns/prov#startedAtTime");
		DatatypeProperty endedAtTime=model.createDatatypeProperty("http://www.w3.org/ns/prov#endedAtTime");
		Individual agentind=agent.createIndividual(DEFAULTNAMESPACE+this.config.publisher);
		ind.addProperty(wasAttributedTo, agentind);
		ind.addProperty(wasGeneratedBy, importactivity);
		importactivity.addProperty(wasAssociatedWith, agentind);
		importactivity.addProperty(used, ind);
		String convertedDate;
		try {
			gc.setTime(startTime);
			convertedDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc).toXMLFormat();
			importactivity.addProperty(startedAtTime, convertedDate);
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			gc.setTime(new Date(System.currentTimeMillis()));
			convertedDate = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc).toXMLFormat();
			importactivity.addProperty(endedAtTime, convertedDate);
		} catch (DatatypeConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ind.addRDFType(entity);
	}

	/**
	 * Imports and models a geometry according to the GeoSPARQL vocabulary.
	 * 
	 * @param ind
	 *            the individual to append the geometry to
	 * @param geom
	 *            the geometry
	 */
	private void importGeometry(Individual ind, GeometryAttribute geom) {
		Individual geomind = geomClass.createIndividual(ind.getURI() + "_geom");
		if(geom!=null && geom.getValue()!=null) {
		geomind.addLiteral(model.createDatatypeProperty("http://www.opengis.net/ont/geosparql#asWKT"),
				model.createTypedLiteral(
						"<http://www.opengis.net/def/crs/EPSG/0/" + epsgCode + "> " + geom.getValue().toString(),
						"http://www.opengis.net/ont/geosparql#wktLiteral"));
		
		GeoJSONWriter writer = new GeoJSONWriter();
		GeoJSON json = writer.write((Geometry) geom.getValue());
		geomind.addLiteral(model.createDatatypeProperty("http://www.opengis.net/ont/geosparql#asGeoJSON"),
				model.createTypedLiteral("<http://www.opengis.net/def/crs/EPSG/0/" + epsgCode + "> " + json.toString(),
						"http://www.opengis.net/ont/geosparql#geojsonLiteral"));
		ind.addProperty(model.createObjectProperty("http://www.opengis.net/ont/geosparql#hasGeometry"), geomind);
		
		// Import heuristics, try to match an overlapping spatial object from
		// linkedgeodata.org.
		// TODO:
		// Contains workarounds because spatial functions return obscure results:
		// - The query returns multiple result and we pick the first
		// - Currently the classType has been restricted to
		// 'http://linkedgeodata.org/ontology/Hospital' which does not match
		// the requirement to import arbitrary building data.
		Geometry geomObj = (Geometry) geom.getValue();
		Coordinate coord = geomObj.getCentroid().getCoordinate();
		List<GeographicalResult> result = dbConnector.getConceptsGeoGraphicallyNearToWithAddress(coord.y, coord.x, 0.0d,
				config.geoendpoint, config.geomatchingclass);
		String firstInd = null;
		Individual firstObj = null;
		if (result != null) {
			for (GeographicalResult item : result) {
				if (null == firstInd)
					firstInd = item.ind;

				if (firstInd == item.ind) {
					OntClass cls = model.createClass(item.classType);
					firstObj = cls.createIndividual(item.ind);
					firstObj.addLabel(item.label);
					ind.addProperty(OWL.sameAs, firstObj);
				}
			}
			if (null == firstInd) {
				System.out.println("No matching object found matching geo for " + ind.getURI());
			} else {
				// Add level 1 information for hospitals from linkedgeodata.org.
				dbConnector.createOntologyFromInd("http://linkedgeodata.org/sparql", firstObj, model, "LinkedGeoData",
						DEFAULTNAMESPACE);
			}
		}
		}
	}

	/**
	 * Imports a value either as ObjectProperty, DataProperty or subclass according
	 * to the XML specification.
	 * 
	 * @param xc
	 *            the column configuration
	 * @param ind
	 *            the column individual
	 * @param dataRow
	 *            the dataRow given from the shapefile
	 * @param value
	 *            the value to insert
	 */
	private void importValue(DataColumnConfig xc, Individual ind, Map<String, String> dataRow, String value) {
		if (value == null || value.isEmpty())
			return;
		if (xc.prop.equals("subclass")) {
			OntClass cls = null;
			if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
				cls = model.createClass(xc.valuemapping.get(value).replace(" ", "_"));
				cls.setLabel(value, "de");
			} else {
				cls = model.createClass(DEFAULTNAMESPACE + URLEncoder.encode(toCamelCase(value).replace(" ", "_")));
				cls.setLabel(value, "de");
			}
			rootClass.addSubClass(cls);
			ind.addOntClass(cls);
		} else if (xc.prop.equals("data")) {
			DatatypeProperty pro;
			if (xc.propertyuri == null) {
				pro = model.createDatatypeProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name));
			} else {
				pro = model.createDatatypeProperty(xc.propertyuri.replace(" ", "_"));
			}
			if (xc.range != null) {
				pro.addRange(model.createResource(xc.range));
			}
			if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
				ind.addProperty(pro, model.createTypedLiteral(xc.valuemapping.get(value), xc.range));
			} else {
				ind.addProperty(pro, model.createTypedLiteral(value, xc.range));
			}

		} else if (xc.prop.equals("annotation")) {
			AnnotationProperty pro;
			if (xc.propertyuri == null) {
				pro = model.createAnnotationProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name.replace(" ", "_")));
			} else {
				pro = model.createAnnotationProperty(xc.propertyuri.replace(" ", "_"));
			}
			if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
				ind.addProperty(pro, model.createTypedLiteral(xc.valuemapping.get(value), xc.range));
			} else {
				ind.addProperty(pro, model.createTypedLiteral(value, xc.range));
			}
		} else if (xc.prop.equals("obj")) {
			ObjectProperty pro;
			if (xc.propertyuri == null) {
				pro = model.createObjectProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name.replace(" ", "_")));
			} else {
				pro = model.createObjectProperty(xc.propertyuri.replace(" ", "_"));
			}
			if (xc.range != null) {
				pro.addRange(model.createClass(xc.range));
			}
			pro.addDomain(rootClass);
			if (xc.queryString != null) {
				Resource obj = model
						.createResource(dbConnector.executeSPARQLQuery(xc.queryString, xc.endpoint, dataRow));
				ind.addProperty(pro, obj);

				// TODO: structure not here
				dbConnector.createWDOntologyFromInd("https://query.wikidata.org/sparql", obj, model, "Wikidata",
						DEFAULTNAMESPACE);
			} else {
				OntClass cls = model.createClass(xc.concept);
				String s = new StrSubstitutor(dataRow, "%%", "%%").replace(xc.indname);
				Individual obj = cls.createIndividual(s.replace(" ", "_"));
				ind.addProperty(pro, obj);

				if (xc.unit != null) {
					obj.addProperty(model.createObjectProperty(xc.unitprop), model.createResource(xc.unit));
				}
				if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
					obj.addProperty(model.createDatatypeProperty(xc.valueprop),
							model.createTypedLiteral(xc.valuemapping.get(value), xc.range));
				} else {
					obj.addProperty(model.createDatatypeProperty(xc.valueprop),
							model.createTypedLiteral(value, xc.range));
				}

			}
		}
	}
}