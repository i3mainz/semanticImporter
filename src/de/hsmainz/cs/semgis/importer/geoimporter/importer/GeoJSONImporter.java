package de.hsmainz.cs.semgis.importer.geoimporter.importer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.JSONObject;

import de.hsmainz.cs.semgis.importer.geoimporter.parser.GeoJSONParser;


public class GeoJSONImporter {

	public static Map<String, List<String>> formatToOntology;


	public static OntModel enrichClasses(OntModel model, OntModel model2, Boolean enrich) {
		OntModel result = ModelFactory.createOntologyModel();
		if (enrich) {
			model.add(model2);
		}

		
		return result;
	}

	public static OntModel processFile(String fileformat,String filepath,
			Boolean isString,Boolean enrich,String outpath,String namespace,String concept,String indid) {
		File infile=null;
		String file="";
		if(!isString) {
			infile = new File(filepath);
			if(!infile.exists() || infile.isDirectory()) {
				System.out.println("Input file " + infile.getPath() + " does not exist!");
				return null;
			}
		}else {
			file=filepath;
		}
		try {
			String content = FileUtils.readFileToString(new File(filepath), StandardCharsets.UTF_8);
			JSONObject geojson=new JSONObject(content);
			OntModel resultmodel=GeoJSONParser.geoJSONToTTL(geojson, namespace, concept, indid);
			if(outpath==null || outpath.isEmpty()) {
				return resultmodel;
			}else {
				resultmodel.write(new FileWriter(new File(outpath)), "TTL");
				return resultmodel;
			}

		} catch (IOException e) {
			System.out.println("There was an error reading the file: " + e.getMessage());
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length < 5) {
			System.out.println("Too less arguments, exiting program");
			System.out.println("SYNTAX: programname inputfile outputfile namespace concept indid " + formatToOntology.keySet() + "");
		} else {
			String filepath = args[0];
			String outpath = args[1];
			String namespace = args[2];
			String concept = args[3];
			String indid = args[1];
			String fileformat = "";
			if (args.length > 2)
				fileformat = args[2];
			boolean enrich = false;
			if (args.length > 3) {
				if (args[3].toUpperCase().equals("ENRICH")) {
					enrich = true;
				}
			}
			System.out.println(filepath + " - " + outpath + " - " + fileformat);
			processFile(fileformat, filepath, false, enrich, outpath,namespace,concept,indid);
		}

	}

}
