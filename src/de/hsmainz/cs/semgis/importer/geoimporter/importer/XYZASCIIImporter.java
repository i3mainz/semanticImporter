package de.hsmainz.cs.semgis.importer.geoimporter.importer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.hsmainz.cs.semgis.importer.geoimporter.parser.ConfigParser;
import de.hsmainz.cs.semgis.importer.geoimporter.parser.KnownSchemaParser;

public class XYZASCIIImporter extends RasterImporter {

	
	
	
	public static OntModel processFile(String fileformat,String filepath,Boolean isString,Boolean enrich,String outpath,String provider, String license, String origin) {
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
		OntModel model = ModelFactory.createOntologyModel();
		try {
			KnownSchemaParser parser;
			if (fileformat.isEmpty()) {
				parser = new KnownSchemaParser(model, true, true,outpath,provider,license,origin);
			} else {
				parser = new KnownSchemaParser(model, false, false,outpath,provider,license,origin);
			}
			XMLReader reader = XMLReaderFactory.createXMLReader();
			reader.setContentHandler(parser);
			if(isString) {
				reader.parse(new InputSource(new StringReader(file)));
			}else{
				reader.parse(new InputSource(new FileReader(infile)));
			}
			if (formatToOntology!=null && formatToOntology.containsKey(fileformat.toUpperCase())) {
				for (String s : formatToOntology.get(fileformat)) {
					OntModel modell = ModelFactory.createOntologyModel();
					modell.read("ontologies/" + s);
					//enrichClasses(model, modell, enrich);
				}
			} else {
				KnownSchemaParser.restructureDomains(model);
			}
			if(outpath==null || outpath.isEmpty()) {
				return parser.model;
			}else {
				parser.model.write(new FileWriter(new File(outpath)), "TTL");
				return parser.model;
			}

		} catch (IOException | SAXException e) {
			System.out.println("There was an error reading the file: " + e.getMessage());
		}
		return null;
	}
	
	public static void main(String[] args) throws IOException {
		File file=new File("testdata.xyz");
		XYZASCIIImporter xyz=new XYZASCIIImporter();
		xyz.toCoverageJSON(file,new TreeMap<>(),"25832");

	}

	@Override
	public String toCoverageJSON(File file, Map<String, String> columns,String crs) throws IOException {
		JSONObject result=new JSONObject();
		result.put("type", "Coverage");
		JSONObject domain=new JSONObject();
		result.put("domain", domain);
		domain.put("type", "Domain");
		domain.put("domainType", "Grid");
		JSONObject axes=new JSONObject();
		domain.put("axes", axes);
		JSONObject parameters=new JSONObject();
		JSONObject parameter=new JSONObject();
		parameters.put("altitude",parameter);
		result.put("parameters",parameters);
		parameter.put("type","Parameter");
		JSONObject paramdescription=new JSONObject();
		parameter.put("description", paramdescription);
		paramdescription.put("en", "altitude");
		JSONObject unit=new JSONObject();		
		parameter.put("unit", unit);
		JSONObject unitlabel=new JSONObject();
		unitlabel.put("en", "meter");
		JSONObject unitsymbol=new JSONObject();
		unit.put("label", unitlabel);
		unit.put("symbol", unitsymbol);
		unitsymbol.put("value","meter");
		unitsymbol.put("type","http://www.opengis.net/def/uom/UCUM/");	
		JSONArray referencing=new JSONArray();
		domain.put("referencing", referencing);
		JSONObject ref=new JSONObject();
		referencing.put(ref);
		JSONArray coordinates=new JSONArray();
		coordinates.put("x");
		coordinates.put("y");
		ref.put("coordinates", coordinates);
		JSONObject system=new JSONObject();
		ref.put("system", system);
		system.put("type", "GeographicCRS");
		system.put("id", "http://www.opengis.net/def/crs/EPSG/0/"+crs);
		JSONObject ranges=new JSONObject();
		result.put("ranges",ranges);
		JSONObject altituderange=new JSONObject();
		ranges.put("altitude",altituderange);
		altituderange.put("type","ndArray");
		altituderange.put("dataType", "float");	
		JSONArray axisNames=new JSONArray();
		altituderange.put("axisNames", axisNames);
		axisNames.put("x");
		axisNames.put("y");
		JSONArray values=new JSONArray();
		altituderange.put("values", values);
		Integer numlines=1;
		JSONObject x=new JSONObject();
		x.put("values",new JSONArray());
		JSONObject y=new JSONObject();
		y.put("values", new JSONArray());
		Double maxx=Double.MIN_VALUE,maxy=Double.MIN_VALUE,maxz=Double.MIN_VALUE,minx=Double.MAX_VALUE,miny=Double.MAX_VALUE,minz=Double.MAX_VALUE;
		try {
			BufferedReader reader=new BufferedReader(new FileReader(file));
			String firstline=reader.readLine();
			while((firstline=reader.readLine())!=null) {
				String[] splitted=firstline.split(" ");
				for(String spl:splitted) {
					/*Double x=Double.valueOf(splitted[0]);
					if(x<minx) {
						minx=x;
					}
					if(x>maxx) {
						maxx=x;
					}
					Double y=Double.valueOf(splitted[1]);
					if(y<miny) {
						miny=y;
					}
					if(y>maxy) {
						maxy=y;
					}*/
					x.getJSONArray("values").put(Double.valueOf(splitted[0]));
					y.getJSONArray("values").put(Double.valueOf(splitted[1]));
					values.put(Double.valueOf(splitted[2]));
				}
				numlines++;
			}
			reader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		/*JSONObject x=new JSONObject();
		x.put("num", numlines);
		x.put("min",minx);
		x.put("max", maxx);
		JSONObject y=new JSONObject();
		y.put("num", numlines);
		y.put("min",miny);
		y.put("max", maxy);
		JSONObject z=new JSONObject();
		z.put("num", numlines);
		z.put("min",minz);
		z.put("max", maxz);*/
		axes.put("x", x);
		axes.put("y", y);
		System.out.println(result.toString(2));
		return result.toString();
	}
	


	
}
