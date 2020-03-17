package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStoreFactory;
import org.geotoolkit.feature.Feature;
import org.xml.sax.SAXException;


public class SHPImporter extends ImporterAPI{

	public SHPImporter() {
		// TODO Auto-generated constructor stub
	}

@Override
public org.geotoolkit.data.FeatureCollection getFeatureCollection(URL path, ResType restype)
		throws IOException, SAXException, ParserConfigurationException, DataStoreException {
	long start=System.currentTimeMillis();

	ShapefileFeatureStoreFactory fac=new ShapefileFeatureStoreFactory();
	Map<String,Serializable> params=new TreeMap<>();
	params.put("url", path);
	params.put("charset", "UTF-8");
	FeatureStore dataStore=null;
	FeatureCollection collect=null;
	try {
		dataStore = fac.createDataStore(path.toURI());

		System.out.println(Arrays.asList(dataStore.getNames()));
		final Session session = dataStore.createSession(true);
		collect = session
				.getFeatureCollection(QueryBuilder.all(dataStore.getNames().iterator().next()));
	} catch (URISyntaxException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
	dataStore.close();
	return collect;
}


@Override
protected Boolean checkIfAvailable(URL path) {
	// TODO Auto-generated method stub
	return null;
}

}
