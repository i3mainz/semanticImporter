package de.hsmainz.cs.semgis.importer.geoimporter.importer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import de.hsmainz.cs.semgis.importer.geoimporter.parser.ConfigParser;

public abstract class FormatImporter {

	public static Map<String, List<String>> formatToOntology;

	public static void readConfig() throws IOException {
		ConfigParser parser = new ConfigParser("config2.csv");
		formatToOntology = parser.maps;
	}
	
}
