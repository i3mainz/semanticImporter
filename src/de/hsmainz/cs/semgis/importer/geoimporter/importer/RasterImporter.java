package de.hsmainz.cs.semgis.importer.geoimporter.importer;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public abstract class RasterImporter extends FormatImporter {

	public abstract String toCoverageJSON(File file,Map<String,String> columns,String crs) throws IOException;
	
	
}
