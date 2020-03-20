package de.hsmainz.cs.semgis.importer.geoimporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFileFeatureStoreFactory;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStoreFactory;
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

	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, DataStoreException {
		Main main=new Main();
		String shpfile="importdata/POI/BFW.shp";
		String configfile="config/bfw.xml";
		main.start(shpfile,shpfile.substring(shpfile.lastIndexOf('/')+1,shpfile.lastIndexOf('.'))+"_result.ttl",configfile,false);
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
			//System.out.println(config.toString());
			this.processFeatures(shpfile, outputPath, config);
		}
	}
	
	public void processFeatures(String featurefile, String outputPath, Config config) throws IOException, DataStoreException {
		
		//Open a shape file and import with geotools.
		System.setProperty("file.encoding","UTF-8");
		File file = new File(featurefile);
		Map<String, Object> map = new HashMap<>();
		map.put("url", file.toURI().toURL());
		map.put("charset","UTF-8");
		AbstractFileFeatureStoreFactory fac=new ShapefileFeatureStoreFactory();
		FeatureStore dataStore=null;
    	Session session;
    	dataStore=fac.createDataStore(file.toURI());
		session=dataStore.createSession(true);
		FeatureCollection collection;
		collection = session.getFeatureCollection(QueryBuilder.all(dataStore.getNames().iterator().next()));	
		//Try to determine the epsg code and the geometry type, if not present, exit the config.
		
		/*Integer epsgCode = null;
		collection.
		try {
			epsgCode = collection.getFeatureType().getCoordinateReferenceSystem()., true);
		} catch (FactoryException e) {
			e.printStackTrace();
		}
		finally {
			if (null == epsgCode)
				epsgCode = config.epsg;
		}
		if (null == epsgCode)
			throw new RuntimeException("The epsg code could not be determined. Please provide the information in the profile.xml");
	*/	
		
		String geomType = collection.getFeatureType().getGeometryDescriptor().getType().getName().toString();


		//Import the data
		DataImporter dataImporter = new DataImporter(config, 0, geomType);
		dataImporter.importToOwl(outputPath, collection,featurefile);
	}
	
}
