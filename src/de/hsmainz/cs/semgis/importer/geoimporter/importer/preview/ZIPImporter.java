package de.hsmainz.cs.semgis.importer.geoimporter.importer.preview;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.FeatureCollection;
import org.xml.sax.SAXException;

import org.apache.jena.ontology.OntModel;

public class ZIPImporter extends ImporterAPI {


	@Override
	public FeatureCollection getFeatureCollection(URL path, ResType restype)
			throws IOException, SAXException, ParserConfigurationException {
		Path file;
		try {
			file = Paths.get(path.toURI());
			FileInputStream fis = new FileInputStream(file.toFile());
			boolean isZipped = new ZipInputStream(fis).getNextEntry() != null;
			fis.close();
			if(isZipped){
				ZipFile zipFile = new ZipFile(file.toFile());

				Enumeration<? extends ZipEntry> entries = zipFile.entries();

				while(entries.hasMoreElements()){
					ZipEntry entry = entries.nextElement();
					InputStream stream = zipFile.getInputStream(entry);
				}
			}
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected Boolean checkIfAvailable(URL path) {
		// TODO Auto-generated method stub
		return null;
	}


}
