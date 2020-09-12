package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.geojson.GeoJSONFeatureStoreFactory;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.xml.sax.SAXException;

/**
 * GeoJSONImporter to convert GML
 * @author timo.homburg
 *
 */
public class GeoJSONImporter extends XMLImporter {

	public GeoJSONImporter() {
		// TODO Auto-generated constructor stub
	}

	
	@Override
	public FeatureCollection getFeatureCollection(URL path, ResType restype)
			throws IOException, SAXException, ParserConfigurationException {
		GeoJSONFeatureStoreFactory fac=new GeoJSONFeatureStoreFactory();	
		try {
			FeatureStore dataStore=fac.createDataStore(path.toURI());
			Session session=dataStore.createSession(true);
			FeatureCollection collection;
			collection = session
				     .getFeatureCollection(QueryBuilder.all(dataStore.getNames().iterator().next()));	
			return collection;		
		} catch (DataStoreException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public FeatureCollection getFeatureCollection(InputStream stream,
			ResType restype) throws IOException, SAXException,
			ParserConfigurationException {
		return null;
	}

	@Override
	protected Boolean checkIfAvailable(URL path) {
		GeoJSONFeatureStoreFactory fac=new GeoJSONFeatureStoreFactory();	
		try {
			FeatureStore dataStore=fac.createDataStore(path.toURI());
			Session session=dataStore.createSession(true);
		} catch (DataStoreException | URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
