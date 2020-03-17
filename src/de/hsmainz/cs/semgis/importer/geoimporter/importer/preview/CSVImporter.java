package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.xml.sax.SAXException;


/**
 * Importer for CSV files.
 * @author timo.homburg
 *
 */
public class CSVImporter extends ImporterAPI {


	/**
	 * Constructor for this class.
	 */
	public CSVImporter() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public FeatureCollection getFeatureCollection(URL path, ResType restype)
			throws IOException, SAXException, ParserConfigurationException {
	    // Create the map with the file URL to be passed to DataStore.
	    Map<String,URL> map = new HashMap<String,URL>();
	    map.put("url", path);
	    if (map.size() > 0) {
	    	CSVFeatureStoreFactory fac=new CSVFeatureStoreFactory();
	    	FeatureStore dataStore;
			try {
				dataStore = fac.createDataStore(path.toURI());
	    	Session session=dataStore.createSession(true);
	    	FeatureCollection collect = session
	    		     .getFeatureCollection(QueryBuilder.all(dataStore.getNames().iterator().next()));  		
	    	return collect;
			} catch (DataStoreException | URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	    }
	    return null;
	}

	@Override
	protected Boolean checkIfAvailable(URL path) {
		// TODO Auto-generated method stub
		return null;
	}

}
