package de.hsmainz.cs.semgis.importer.geoimporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFileFeatureStoreFactory;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.data.geojson.GeoJSONFeatureStoreFactory;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStoreFactory;
import org.geotoolkit.data.wfs.WFSFeatureStoreFactory;
import org.geotoolkit.data.wfs.WebFeatureClient;
import org.geotoolkit.geometry.jts.JTS;
import org.geotoolkit.referencing.CRS;
/*import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.referencing.CRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.FactoryException;*/
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.hsmainz.cs.semgis.importer.geoimporter.config.Config;
import de.hsmainz.cs.semgis.importer.geoimporter.importer.DataImporter;
import de.hsmainz.cs.semgis.importer.geoimporter.parser.KnownSchemaParser;

public class Main {

	
	static Map<String,String> fileToConf=new HashMap<>();
	
	public Main() {

		/*fileToConf.put("importdata/POI/BFW.shp","config/bfw.xml");
		fileToConf.put("importdata/POI/BPOL.shp","config/bpol.xml");
		fileToConf.put("importdata/POI/BotKon.shp","config/botkon.xml");
		fileToConf.put("importdata/POI/BBeh_plus.shp","config/bbeh.xml");
		fileToConf.put("importdata/POI/Gerichte.shp","config/gerichte.xml");
		fileToConf.put("importdata/POI/HS.shp","config/hs.xml");
		fileToConf.put("importdata/POI/JVA.shp","config/jva.xml");
		fileToConf.put("importdata/POI/Kfz.shp","config/kfz.xml");
		fileToConf.put("importdata/POI/KHV_plus.shp","config/khv.xml");
		fileToConf.put("importdata/POI/LBeh.shp","config/lbeh.xml");
		fileToConf.put("importdata/POI/LPOL.shp","config/lpol.xml");
		fileToConf.put("importdata/POI/RefKon.shp","config/refkon.xml");
		fileToConf.put("importdata/POI/RHV.shp","config/rhv.xml");
		fileToConf.put("importdata/POI/Schulen_allg.shp","config/schulen_allg.xml");
		fileToConf.put("importdata/POI/Schulen_beruf.shp","config/schulen_beruf.xml");
		fileToConf.put("importdata/POI/StA.shp","config/sta.xml");
		fileToConf.put("importdata/POI/THW.shp","config/thw.xml");
		fileToConf.put("importdata/POI/UNOrg.shp","config/unorg.xml");
		fileToConf.put("importdata/POI/Zoll.shp","config/zoll.xml");*/
		//fileToConf.put("importdata/schools/schulen_nrw.shp","config/nrw_schulen.xml");
		//fileToConf.put("importdata/schools/brandenburg_schule.shp","config/brandenburg_schulen.xml");
		fileToConf.put("importdata/HadriansWall/forts_hw.geojson","config/fort.xml");
		/*
		fileToConf.put("importdata/schools/Schulstandorte_HB.shp","config/bremen_schulen.xml");
		fileToConf.put("importdata/schools/Schulstandorte_BHV.shp","config/bremen_schulen.xml");
		//fileToConf.put("importdata/unesco/Welterbe_HB.shp","config/bremen_unesco.xml");
		/*fileToConf.put("unesco/42001g.shp","config/bauhaus_unesco.xml");
		fileToConf.put("unesco/41001g.shp","config/hamburg_unesco.xml");
		fileToConf.put("unesco/42001g.shp","config/sachsenanhalt_unesco.xml");
		
		 * 		fileToConf.put("importdata/xplanung/41001g.shp","config/xplanung_st_bp_plan.xml");
		fileToConf.put("importdata/xplanung/42001g.shp","config/xplanung_st_fp_plan.xml");
		 * fileToConf.put("importdata/POI/KITA.shp","config/kita.xml");*/
	}
	
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, DataStoreException {
		Main main=new Main();
		for(String key:fileToConf.keySet()) {
			String shpfile=key;//"importdata/POI/BFW.shp";
			String configfile=fileToConf.get(key);//"config/bfw.xml";
			main.start(shpfile,"result/"+shpfile.substring(shpfile.lastIndexOf('/')+1,shpfile.lastIndexOf('.'))+"_result.ttl",configfile,false);
		}

	}
	
	
	public void importKnownSchema(String file,String outpath) throws SAXException, FileNotFoundException, IOException {
		OntModel model = ModelFactory.createOntologyModel();
		KnownSchemaParser parser=new KnownSchemaParser(model, true, true,"","","","");
		XMLReader reader = XMLReaderFactory.createXMLReader();
		reader.setContentHandler(parser);
		reader.parse(new InputSource(new FileReader(file)));
		parser.model.write(new FileWriter(new File(outpath)), "TTL");
		System.out.println("Finished the conversion");
	}

	public void start(String shpfile,String outputPath, String configfile,Boolean schemaGiven) throws SAXException, IOException, ParserConfigurationException, DataStoreException {
		Config config=new Config();
		if(schemaGiven) {
			importKnownSchema(shpfile,outputPath);
		}else {
			SAXParserFactory.newInstance().newSAXParser().parse(new File(configfile), config);
			//System.out.println(config.toString())
			this.processFeatures(shpfile, outputPath, config,shpfile.substring(shpfile.lastIndexOf('.')+1),null);
		}
	}
	
	public OntModel processFeatures(String featurefile, String outputPath, Config config,String fileFormat,String serviceurl) throws IOException, DataStoreException {
		
		//Open a shape file and import with geotools.
		System.setProperty("file.encoding","UTF-8");
		File file = new File(featurefile);
		Map<String, Object> map = new HashMap<>();
		map.put("url", file.toURI().toURL());
		map.put("charset","UTF-8");
		AbstractFileFeatureStoreFactory fac;
    	FeatureStore dataStore=null;
    	Session session;
    	switch(fileFormat) {
    	case "geojson":
    		fac=new GeoJSONFeatureStoreFactory();
        	dataStore=fac.createDataStore(file.toURI());
        	session=dataStore.createSession(true);
    		break;
    	case "csv":
    		fac=new CSVFeatureStoreFactory();
        	dataStore=fac.createDataStore(file.toURI());
        	session=dataStore.createSession(true);
    		break;
    	case "shp":
    		fac=new ShapefileFeatureStoreFactory();
        	dataStore=fac.createDataStore(file.toURI());
			session=dataStore.createSession(true);
    		break;
    	case "wfs":
    		WFSFeatureStoreFactory fac2=new WFSFeatureStoreFactory();
    		Map<String,String> mapp=new TreeMap<String,String>();
    		map.put("url",serviceurl);
    		WebFeatureClient test = fac2.create(mapp);
			session=test.createSession(true);
    		break;
    	default: 
    		return null;
    	}    	
		FeatureCollection collection;
		collection = session.getFeatureCollection(QueryBuilder.all(dataStore.getNames().iterator().next()));
		String epsgCode="0";
		if(!collection.getEnvelope().getCoordinateReferenceSystem().getIdentifiers().isEmpty()) {
			epsgCode=collection.getEnvelope().getCoordinateReferenceSystem().getIdentifiers().iterator().next().getCode();
		}	
		String geomType = collection.getFeatureType().getGeometryDescriptor().getType().getName().toString();


		//Import the data
		DataImporter dataImporter = new DataImporter(config, epsgCode, geomType);
		dataImporter.importToOwl(outputPath, collection,featurefile);
		return dataImporter.model;
	}
	
}
