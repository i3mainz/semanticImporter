package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.geotoolkit.data.FeatureCollection;
import org.xml.sax.SAXException;


public abstract class XMLImporter extends ImporterAPI {

	public XMLImporter() {
		// TODO Auto-generated constructor stub
	}

	public abstract FeatureCollection getFeatureCollection(InputStream stream, ResType restype) throws IOException, SAXException, ParserConfigurationException;

	@Override
	public FeatureCollection getFeatureCollection(URL path, ResType restype) throws IOException, SAXException, ParserConfigurationException {
		System.out.println(path);
		return this.getFeatureCollection(path.openStream(), restype);
	}
	
	@Override
	protected List<String> getTables(URL path) {
		return new LinkedList<String>();
	}
}
