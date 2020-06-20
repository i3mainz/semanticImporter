package de.hsmainz.cs.semgis.importer.geoimporter.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureIterator;
import org.geotoolkit.feature.Feature;
import org.opengis.feature.Property;

import de.hsmainz.cs.semgis.importer.geoimporter.config.Config;
import de.hsmainz.cs.semgis.importer.geoimporter.config.DataColumnConfig;
import de.hsmainz.cs.semgis.importer.geoimporter.config.GeoMatching;
import de.hsmainz.cs.semgis.importer.geoimporter.config.ValueMapping;
import de.hsmainz.cs.semgis.importer.geoimporter.connector.TripleStoreConnector;
import de.hsmainz.cs.semgis.importer.geoimporter.connector.TripleStoreConnector.GeographicalResult;
import de.hsmainz.cs.semgis.util.Tuple;


public class DataImporter {
	public static TripleStoreConnector dbConnector = new TripleStoreConnector("prefix.txt");

	public static final String DEFAULTNAMESPACE = "http://www.semgis.de/geodata/";

	private final Config config;
	public final OntModel model;
	private OntClass rootClass;
	private List<OntClass> additionalClasses=new LinkedList<OntClass>();
	private final OntClass geomClass;
	private final OntClass featurecl;
	ObjectProperty hasGeometry;
	DatatypeProperty asWKT,epsg;
	private String epsgCode;
	private Date startTime;
	private Integer sameAsCount=0;
	GregorianCalendar gc = new GregorianCalendar(TimeZone.getTimeZone("CEST"));

	public DataImporter(Config config, String epsgCode, String geomType) {
		this.config = config;
		this.sameAsCount=0;
		this.model = ModelFactory.createOntologyModel();
		ObjectProperty prop=model.createObjectProperty(RDF.type.getURI());
		prop.addLabel("is a", "en");
		prop.addLabel("ist ein(e)","de");
		hasGeometry=model.createObjectProperty("http://www.opengis.net/ont/geosparql#hasGeometry");
		asWKT=model.createDatatypeProperty("http://www.opengis.net/ont/geosparql#asWKT");
		epsg=model.createDatatypeProperty("http://www.opengis.net/ont/geosparql#epsg");
		hasGeometry.addLabel("has Geometry","en");
		hasGeometry.addLabel("hat Geometrie","de");
		asWKT.addLabel("as WKT serialization","en");
		asWKT.addLabel("als WKT Serialisierung","de");
		epsg.addLabel("EPSG code","en");
		epsg.addLabel("EPSG Code","de");
		OntClass geometry = model.createClass("http://www.opengis.net/ont/geosparql#Geometry");
		OntClass spatial = model.createClass("http://www.opengis.net/ont/geosparql#SpatialObject");
		featurecl = model.createClass("http://www.opengis.net/ont/geosparql#Feature");
		featurecl.addSuperClass(spatial);
		OntClass mostgen=null;
		if(config.mostGeneralClass!=null) {
			mostgen=model.createClass(config.mostGeneralClass);
			mostgen.addSuperClass(featurecl);
		}else {
			mostgen=featurecl;
		}
		geometry.addSuperClass(spatial);
		if(!config.rootClass.contains("%%")) {
			this.rootClass = model.createClass(config.rootClass);
			if(config.rootClassLabel!=null) {
				this.rootClass.setLabel(config.rootClassLabel,"en");
			}
			rootClass.addSuperClass(featurecl);
		}
		for(String addcls:config.additionalClasses.keySet()) {
			OntClass addc=model.createClass(addcls);
			for(String lang:config.additionalClasses.get(addcls).labels.keySet()) {
				addc.addLabel(config.additionalClasses.get(addcls).labels.get(lang),lang);
			}
			additionalClasses.add(addc);
			rootClass.addSuperClass(addc);	
			if(config.additionalClasses.get(addcls).superclass!=null) {
				OntClass addc2=model.createClass(config.additionalClasses.get(addcls).superclass);
				addc.addSuperClass(addc2);
				addc2.addSuperClass(mostgen);				
			}
		}
		this.epsgCode = epsgCode;
		if(config.epsg!=null && 0!=config.epsg)
			this.epsgCode=config.epsg.toString();
		System.out.println("EPSG: "+this.epsgCode);
		this.geomClass = model.createClass("http://www.opengis.net/ont/sf#" + geomType.replace("http://geotoolkit.org:",""));
		this.geomClass.setLabel(geomType.replace("http://geotoolkit.org:",""), "en");
		this.geomClass.addSuperClass(geometry);
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
		try (FeatureIterator features = collection.iterator()) {
			
			while (features.hasNext()) {
				System.out.println(counter++ + "/" + collection.size());

				// Transfer the current data row to a hash map.
				Feature feature = features.next();
				Map<String, String> dataRow = new HashMap<String, String>();
				for (Property prop : feature.getProperties()) {
					String value = null != prop.getValue() ? prop.getValue().toString() : null;
					dataRow.put(prop.getName().toString(), value);
					dataRow.put(prop.getName().toString().replace("http://geotoolkit.org:",""), value);
				}
				if(config.rootClass.contains("%%")) {
					String s = new StrSubstitutor(dataRow, "%%", "%%").replace(config.rootClass);
					this.rootClass=model.createClass(s);
					rootClass.addSuperClass(featurecl);
				}
				//System.out.println("ADDCOLUMNS: "+config.addcolumns);
				if (first) {
					if(!config.nometadata) {
						StringBuilder builder = new StringBuilder();
						for (String propName : dataRow.keySet()) {
							builder.append(propName.replace("http://geotoolkit.org:","") + ";");
						}			
						AnnotationProperty annpro = model.createAnnotationProperty("http://semgis.de/geodata#propOrder");
						rootClass.addProperty(annpro, builder.toString());
						rootClass.addProperty(model.createAnnotationProperty("http://semgis.de/geodata#standard"),
							filename.substring(filename.lastIndexOf('/') + 1));
					}
					first = false;
				}
				// Import root individual.
				Individual currentind;
				if(config.indid==null) {
					currentind=rootClass.createIndividual(config.namespace+UUID.randomUUID());
				}else if(config.indid.contains(";")) {
					String[] ids=config.indid.split(";");
					StringBuilder val=new StringBuilder();
					for(String id:ids) {
						val.append(dataRow.get(id));
					}
					String vall=val.toString();
					if(config.indidprefix!=null) {
						vall=config.indidprefix+vall;
					}
					if(config.indidsuffix!=null) {
						vall=vall+config.indidsuffix;
					}	
					currentind=rootClass.createIndividual(config.namespace+vall.replace(" ","_"));
					if(!config.nometadata)
						importMetaData(currentind,vall);
				}else {
					String indd=dataRow.get(config.indid);
					if(config.indidprefix!=null) {
						indd=config.indidprefix+indd;
					}
					if(config.indidsuffix!=null) {
						indd=indd+config.indidsuffix;
					}
					if(indd!=null) {
					currentind = rootClass.createIndividual(config.namespace + indd.replace(" ","_"));
					}else {
						currentind=rootClass.createIndividual(config.namespace + UUID.randomUUID().toString());
					}
					if(!config.nometadata)
						importMetaData(currentind,indd);
				}
				for(OntClass cls:additionalClasses) {
					currentind.addRDFType(cls);
					cls.addSuperClass(featurecl);
				}

				// Import data columns.
				for (String key : config.resultmap.keySet()) {
					List<DataColumnConfig> conflist=config.resultmap.get(key);
					String columnName = key;
					for (DataColumnConfig curconf:conflist) {
						if (config.resultmap.containsKey(columnName)) {
							if(dataRow.get("http://geotoolkit.org:"+key)!=null)
								parseDataColumnConfigs(curconf, currentind, dataRow, dataRow.get("http://geotoolkit.org:"+key), 1);
							else
								parseDataColumnConfigs(curconf, currentind, dataRow, dataRow.get(key), 1);
						}else if(curconf.staticvalue!=null) {
							parseDataColumnConfigs(curconf, currentind, dataRow, curconf.staticvalue, 1);
						}
					} 
					if(config.addcolumns!=null && !config.addcolumns.isEmpty()) {
						for (DataColumnConfig curconf:config.addcolumns) {
							//System.out.println("ADDING COLUMN: "+curconf.staticvalue);
							if(curconf.staticvalue!=null)
								parseDataColumnConfigs(curconf, currentind, dataRow, curconf.staticvalue, 1);
						}
					}
				}
				Map<String,String> labels=new TreeMap<String,String>();
				StmtIterator iter = currentind.listProperties(RDFS.label);
				while(iter.hasNext()) {
					Statement st=iter.next();
					labels.put(st.getObject().asLiteral().getLanguage(),st.getObject().asLiteral().getString());
				}
				iter.close();
				
				// Import geometry column.
				importGeometry(currentind, feature.getDefaultGeometryProperty(),labels);
			}
		}

		model.write(new OutputStreamWriter(new FileOutputStream(outputPath), StandardCharsets.UTF_8), "TTL");
		replaceInFile(new File(outputPath));
		
	}
	
	
	public void replaceInFile(File file) throws IOException {

	    File tempFile = File.createTempFile("buffer", ".tmp");
	    FileWriter fw = new FileWriter(tempFile);

	    Reader fr = new FileReader(file);
	    BufferedReader br = new BufferedReader(fr);

	    while(br.ready()) {
	        fw.write(br.readLine().replace("Ã¤", "ä").replace("Ã¼","ü").replace("Ã¶","ö").replace("Ã","ö") + "\n");
	    }

	    fw.close();
	    br.close();
	    fr.close();

	    // Finally replace the original file.
	    tempFile.renameTo(file);
	}
	
	public void parseDataColumnConfigs(DataColumnConfig curconf,Individual currentind,
			Map<String, String> dataRow,String value, Integer depth) {
			System.out.println(value);
			System.out.println(dataRow);
			if(value!=null)
				value=value.replace("http://geotoolkit.org:","");
			//System.out.println("ParseDataColumnConfigs in depth "+depth);
			//System.out.println(curconf.name+" - "+curconf.isCollection+" "+curconf.subconfigs);
			if(curconf.staticvalue!=null) {
				if(!curconf.propertyuri.isEmpty()) {
					for(String iri:curconf.propertyuri.keySet()) {
						//System.out.println("PROPERTYURI+STATICVALUE: "+iri+" - "+value);
						importValue(curconf, currentind, dataRow, value,iri,curconf.propertyuri.get(iri));
					}
				}else {
					importValue(curconf, currentind, dataRow, value,null,null);
				}
			}else if(curconf.isCollection) {
				System.out.println("Found Collection: "+curconf.name);
				OntClass cls=model.createClass(curconf.concept);
				Individual subind=cls.createIndividual(this.config.namespace+UUID.randomUUID());
				for(String prop:curconf.propertyuri.keySet()) {
					ObjectProperty propp=model.createObjectProperty(prop);
					if(curconf.propertyuri.get(prop)!=null) {
						for(String key:curconf.propertyuri.get(prop).keySet()) {
							propp.addLabel(curconf.propertyuri.get(prop).get(key),key);
						}					
					}					
					currentind.addProperty(propp, subind);
				}
				for(DataColumnConfig subconf:curconf.subconfigs) {	
					System.out.println("Found subconfigs in depth "+depth);
					this.parseDataColumnConfigs(subconf,subind,dataRow,dataRow.get(subconf.name), depth+1);
				}
			}else {
				if (curconf.separationCharacter != null && curconf.splitposition != null && value!=null) {
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
						if(toadd.length()-curconf.separationCharacter.length()>0)
							toadd=toadd.substring(0,toadd.length()-curconf.separationCharacter.length());
					} else {
						position = Integer.valueOf(curconf.splitposition);
						toadd = spl[position];
					}
					if(curconf.valueprefix!=null) {
						toadd=curconf.valueprefix+toadd;
					}
					if(curconf.valuesuffix!=null) {
						toadd=toadd+curconf.valuesuffix;
					}
					if(!curconf.propertyuri.isEmpty()) {
						for(String iri:curconf.propertyuri.keySet()) {
							importValue(curconf, currentind, dataRow, value,iri,curconf.propertyuri.get(iri));
						}
					}else {
						importValue(curconf, currentind, dataRow, value,null,null);
					}
				} else if (curconf.splitregex != null && curconf.splitposition != null) {
					Pattern pattern = Pattern.compile(curconf.splitregex);
					String toadd = pattern.matcher(value).group(Integer.valueOf(curconf.splitposition));
					if(curconf.valueprefix!=null) {
						toadd=curconf.valueprefix+toadd;
					}
					if(curconf.valuesuffix!=null) {
						toadd=toadd+curconf.valuesuffix;
					}
					if(!curconf.propertyuri.isEmpty()) {
						for(String iri:curconf.propertyuri.keySet()) {
							importValue(curconf, currentind, dataRow, value,iri,curconf.propertyuri.get(iri));
						}
					}else {
						importValue(curconf, currentind, dataRow, value,null,null);
					}
				} else {
					if(curconf.valueprefix!=null) {
						value=curconf.valueprefix+value;
					}
					if(curconf.valuesuffix!=null) {
						value=curconf.valuesuffix+value;
					}
					if(!curconf.propertyuri.isEmpty()) {
						for(String iri:curconf.propertyuri.keySet()) {
							importValue(curconf, currentind, dataRow, value,iri,curconf.propertyuri.get(iri));
						}
					}else {
						importValue(curconf, currentind, dataRow, value,null,null);
					}
				}
				
			}
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
	private void importGeometry(Individual ind, Property geom,Map<String,String> labels) {
		Individual geomind = geomClass.createIndividual(ind.getURI() + "_geom");
		StmtIterator iter = ind.listProperties(RDFS.label);
		for(String key:labels.keySet()) {
			geomind.addLabel(labels.get(key)+" Geometry",
					key);
		}
		if(geom!=null && geom.getValue()!=null) {
			if(config.attachepsg) {
				geomind.addLiteral(asWKT,
						model.createTypedLiteral(
								"<http://www.opengis.net/def/crs/EPSG/0/" + epsgCode + "> " + geom.getValue().toString(),
								"http://www.opengis.net/ont/geosparql#wktLiteral"));
			}else {
				geomind.addLiteral(epsg,epsgCode);
				geomind.addLiteral(asWKT,
						model.createTypedLiteral(
								geom.getValue().toString(),
								"http://www.opengis.net/ont/geosparql#wktLiteral"));
			}
		
		
		/*GeoJSONWriter writer = new GeoJSONWriter();
		GeoJSON json = writer.write((Geometry) geom.getValue());
		geomind.addLiteral(model.createDatatypeProperty("http://www.opengis.net/ont/geosparql#asGeoJSON"),
				model.createTypedLiteral("<http://www.opengis.net/def/crs/EPSG/0/" + epsgCode + "> " + json.toString(),
						"http://www.opengis.net/ont/geosparql#geojsonLiteral"));*/
		ind.addProperty(hasGeometry, geomind);
		
		// Import heuristics, try to match an overlapping spatial object from
		// linkedgeodata.org.
		// TODO:
		// Contains workarounds because spatial functions return obscure results:
		// - The query returns multiple result and we pick the first
		// - Currently the classType has been restricted to
		// 'http://linkedgeodata.org/ontology/Hospital' which does not match
		// the requirement to import arbitrary building data.
		for(GeoMatching matching:config.geomatchings) {
			if(matching.geoendpoint!=null && matching.geoquery!=null) {
				Geometry geomObj = (Geometry) geom.getValue();
				Coordinate coord = geomObj.getCentroid().getCoordinate();
				List<GeographicalResult> result = dbConnector.getConceptsGeoGraphicallyNearToWithAddress(coord.y, coord.x, 0.0d,
						matching.geoendpoint, matching.geomatchingclass);
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
							firstObj.addProperty(OWL.sameAs, ind);
						}
					}
					if (null == firstInd) {
						System.out.println("No matching object found matching geo for " + ind.getURI());
					} else {
						if(matching.level1) {
						// Add level 1 information for hospitals from linkedgeodata.org.
						dbConnector.createOntologyFromInd(matching.level1endpoint, firstObj, model, "LinkedGeoData",
								DEFAULTNAMESPACE);
						}
					}
				}
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
	private void importValue(DataColumnConfig xc, Individual ind, Map<String, String> dataRow,
			String value,String propiri,Map<String,String> proplabel) {
		//System.out.println("ImportValue: "+value);
		//System.out.println("PROPVALUE: "+xc.prop);
		if (value == null || (value.isEmpty() && (xc.valuemapping==null || xc.valuemapping.isEmpty())))
			return;
		value=value.replace("http://geotoolkit.org:","");
	
		//System.out.println("DATAROW: "+xc);
		if (xc.prop.equals("subclass")) {
			OntClass cls = null;
			if (xc.valuemapping != null && !xc.valuemapping.isEmpty() && xc.valuemapping.containsKey(value)) {
				for(ValueMapping clsval:xc.valuemapping.get(value)) {				
					cls = model.createClass(clsval.to.replace(" ", "_"));
					if(proplabel!=null) {
						for(String key:proplabel.keySet()) {
							cls.addLabel(proplabel.get(key),key);
						}
					}else if(config.classlabellang!=null) {
						cls.setLabel(value, config.classlabellang);
					}else if(xc.language!=null){
						cls.setLabel(value, xc.language);
					}else {
						cls.setLabel(value, "en");
					}					
				}
				//cls = model.createClass(xc.valuemapping.get(value).replace(" ", "_"));
				//cls.setLabel(value, "de");
			} else if(!value.isEmpty()) {
				if(xc.namespace!=null) {
					cls = model.createClass(xc.namespace+ URLEncoder.encode(toCamelCase(value).replace(" ", "_")));
					if(proplabel!=null) {
						for(String key:proplabel.keySet()) {
							cls.addLabel(proplabel.get(key),key);
						}
					}else if(config.classlabellang!=null) {
						cls.setLabel(value, config.classlabellang);
					}else if(xc.language!=null){
						cls.setLabel(value, xc.language);
					}else {
						cls.setLabel(value, "en");
					}
				}else {
					cls = model.createClass(config.namespace+ URLEncoder.encode(toCamelCase(value).replace(" ", "_")));
					if(proplabel!=null) {
						for(String key:proplabel.keySet()) {
							cls.addLabel(proplabel.get(key),key);
						}
					}if(config.classlabellang!=null) {
						cls.setLabel(value, config.classlabellang);
					}else if(xc.language!=null){
						cls.setLabel(value, xc.language);
					}else {
						cls.setLabel(value, "en");
					}
				}
			}
			if(cls!=null) {
				rootClass.addSubClass(cls);
				ind.addOntClass(cls);
			}
		} else if (xc.prop.equals("data")) {
			DatatypeProperty pro;
			if (propiri == null) {
				pro = model.createDatatypeProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name));
				if(proplabel!=null) {
					for(String key:proplabel.keySet()) {
						pro.addLabel(proplabel.get(key),key);
					}
				}else if(config.proplabellang!=null) {
					pro.setLabel(xc.name, config.proplabellang);
				}else if(xc.language!=null){
					pro.setLabel(xc.name, xc.language);
				}else {
					pro.setLabel(xc.name, "en");
				}
				
			} else {
				pro = model.createDatatypeProperty(propiri.replace(" ", "_"));
				if(proplabel!=null) {
					for(String key:proplabel.keySet()) {
						pro.addLabel(proplabel.get(key),key);
					}
				}else if(config.proplabellang!=null) {
					pro.setLabel(xc.name, config.proplabellang);
				}else if(xc.language!=null){
					pro.setLabel(xc.name, xc.language);
				}else {
					pro.setLabel(xc.name, "en");
				}
			}
			if (xc.range != null) {
				pro.addRange(model.createResource(xc.range));
			}
			if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
				for(ValueMapping val:xc.valuemapping.get(value)) {
					ind.addProperty(pro, model.createTypedLiteral(val.to, xc.range));
				}
			} else {
				ind.addProperty(pro, model.createTypedLiteral(value, xc.range));
			}

		} else if (xc.prop.equals("annotation")) {
			AnnotationProperty pro;
			if (propiri == null) {
				pro = model.createAnnotationProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name.replace(" ", "_")));
				if(proplabel!=null) {
					for(String key:proplabel.keySet()) {
						pro.addLabel(proplabel.get(key),key);
					}
				}else if(config.proplabellang!=null) {
					pro.setLabel(xc.name, config.proplabellang);
				}else if(xc.language!=null){
					pro.setLabel(xc.name, xc.language);
				}else {
					pro.setLabel(xc.name, "en");
				}
			} else {
				pro = model.createAnnotationProperty(propiri.replace(" ", "_"));
				if(proplabel!=null) {
					for(String key:proplabel.keySet()) {
						pro.addLabel(proplabel.get(key),key);
					}
				}else if(config.proplabellang!=null) {
					pro.setLabel(xc.name, config.proplabellang);
				}else if(xc.language!=null){
					pro.setLabel(xc.name, xc.language);
				}else {
					pro.setLabel(xc.name, "en");
				}
			}
			if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
				for(ValueMapping val:xc.valuemapping.get(value)) {
					ind.addProperty(pro, model.createTypedLiteral(val.to, xc.range));
				}
			} else {
				ind.addProperty(pro, model.createTypedLiteral(value, xc.range));
			}
		} else if (xc.prop.equals("obj")) {
			//System.out.println("Creating Object Property!");
			ObjectProperty pro;
			if (propiri == null) {
				pro = model.createObjectProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name.replace(" ", "_")));
				if(proplabel!=null) {
					for(String key:proplabel.keySet()) {
						pro.addLabel(proplabel.get(key),key);
					}
				}else if(config.proplabellang!=null) {
					pro.setLabel(xc.name, config.proplabellang);
				}else if(xc.language!=null){
					pro.setLabel(xc.name, xc.language);
				}else {
					pro.setLabel(xc.name, "en");
				}
			} else {
				pro = model.createObjectProperty(propiri.replace(" ", "_"));
				if(proplabel!=null) {
					for(String key:proplabel.keySet()) {
						pro.addLabel(proplabel.get(key),key);
					}
				}else if(xc.name!=null && config.proplabellang!=null) {
						pro.setLabel(xc.name, config.proplabellang);
					}else if(xc.name!=null &&  xc.language!=null){
						pro.setLabel(xc.name, xc.language);
					}else if (xc.name!=null) {
						pro.setLabel(xc.name, "en");
					}
			}
			if (xc.range != null) {
				pro.addRange(model.createClass(xc.range));
			}
			pro.addDomain(rootClass);
			if (xc.queryString != null) {
				Tuple<String,String> res = dbConnector.executeSPARQLQuery(xc.queryString, xc.endpoint, dataRow);
				if(xc.ignoreUnresolved && res==null) {
					return;
				}
					if(xc.concept!=null) {
						OntClass ccls=model.createClass(xc.concept);
						Individual obj=null;
						if(res!=null) {
							obj=ccls.createIndividual(res.getOne());
						}else {
							try {
								obj=ccls.createIndividual(config.namespace+URLEncoder.encode(value.replace(" ", "_"),"UTF-8"));							
							} catch (UnsupportedEncodingException e) {
								obj=ccls.createIndividual(config.namespace+value.replace(" ", "_"));
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}
						if(res!=null && res.getTwo()!=null) {
							obj.setLabel(res.getTwo(), "en");
						}else if(config.proplabellang!=null) {
							obj.setLabel(value, config.proplabellang);
						}else if(xc.language!=null){
							obj.setLabel(value, xc.language);
						}else {
							obj.setLabel(value, "en");
						}
						ind.addProperty(pro, obj);
					}else {
						DatatypeProperty prop;
						if (propiri == null) {
							prop = model.createDatatypeProperty(DEFAULTNAMESPACE + "has" + toCamelCase(xc.name));
							if(proplabel!=null) {
								for(String key:proplabel.keySet()) {
									prop.addLabel(proplabel.get(key),key);
								}
							}else if(config.proplabellang!=null) {
								prop.setLabel(xc.name, config.proplabellang);
							}else if(xc.language!=null){
								prop.setLabel(xc.name, xc.language);
							}else {
								prop.setLabel(xc.name, "en");
							}
						} else {
							prop = model.createDatatypeProperty(propiri.replace(" ", "_"));
							if(proplabel!=null) {
								for(String key:proplabel.keySet()) {
									prop.addLabel(proplabel.get(key),key);
								}
							}else if(config.proplabellang!=null) {
								prop.setLabel(xc.name, config.proplabellang);
							}else if(xc.language!=null){
								prop.setLabel(xc.name, xc.language);
							}else {
								prop.setLabel(xc.name, "en");
							}
						}
						//Resource obj = model
								//.createResource(res);
							ind.addProperty(prop, value);
					}
				if(xc.keepdataprop!=null && xc.keepdataprop.equals("true")) {
					if(res!=null)
						System.out.println("sameAsCount: "+sameAsCount++);
					ind.addProperty(RDFS.label,value);
				}				
				// TODO: structure not here
				/*dbConnector.createWDOntologyFromInd("https://query.wikidata.org/sparql", obj, model, "Wikidata",
						DEFAULTNAMESPACE);*/
			} else if(xc.concept!=null) {
				OntClass cls = model.createClass(xc.concept);
				Individual obj;
				if(xc.staticvalue!=null && xc.staticvalue.contains("http")) {
					//System.out.println("Has Concept and Static Value: "+xc.concept+" - "+xc.staticvalue);
					obj = cls.createIndividual(xc.staticvalue);
				}else {
					String s = new StrSubstitutor(dataRow, "%%", "%%").replace(xc.indname);
					obj = cls.createIndividual(s.replace(" ", "_"));
				}
				ind.addProperty(pro, obj);
				if (xc.unit != null) {
					obj.addProperty(model.createObjectProperty(xc.unitprop), model.createResource(xc.unit));
				}
				if (xc.valuemapping != null && xc.valuemapping.containsKey(value)) {
					for(ValueMapping val:xc.valuemapping.get(value)) {
						if(val.propiri!=null) {
							DatatypeProperty prop=model.createDatatypeProperty(propiri);
							if(proplabel!=null) {
								for(String key:proplabel.keySet()) {
									prop.addLabel(proplabel.get(key),key);
								}
							}else if(config.proplabellang!=null) {
								prop.setLabel(xc.name, config.proplabellang);
							}else if(xc.language!=null){
								prop.setLabel(xc.name, xc.language);
							}else {
								prop.setLabel(xc.name, "en");
							}
							obj.addProperty(prop,model.createTypedLiteral(val.to, xc.range));
						}else {
							obj.addProperty(model.createDatatypeProperty(xc.valueprop),
									model.createTypedLiteral(val.to, xc.range));
						}
						/*if(!val.addcolumns.isEmpty()) {
							for(DataColumnConfig conf:val.addcolumns) {
								importValue(conf, ind, dataRow, value, propiri);
							}
						}*/
					}
				} 
			}
		}
	}
}