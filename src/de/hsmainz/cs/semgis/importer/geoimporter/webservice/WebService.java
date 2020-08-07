package de.hsmainz.cs.semgis.importer.geoimporter.webservice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.sis.storage.DataStoreException;
import org.geotoolkit.data.AbstractFileFeatureStoreFactory;
import org.geotoolkit.data.FeatureCollection;
import org.geotoolkit.data.FeatureStore;
import org.geotoolkit.data.csv.CSVFeatureStoreFactory;
import org.geotoolkit.data.geojson.GeoJSONFeatureStoreFactory;
import org.geotoolkit.data.query.QueryBuilder;
import org.geotoolkit.data.session.Session;
import org.geotoolkit.data.shapefile.ShapefileFeatureStoreFactory;
import org.geotoolkit.data.wfs.WFSFeatureStoreFactory;
import org.geotoolkit.data.wfs.WebFeatureClient;
import org.geotoolkit.feature.Property;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.PropertyType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import de.hsmainz.cs.semgis.importer.geoimporter.Main;
import de.hsmainz.cs.semgis.importer.geoimporter.config.Config;
import de.hsmainz.cs.semgis.importer.geoimporter.importer.GMLImporter;
import de.hsmainz.cs.semgis.importer.geoimporter.parser.GeoJSONParser;
import de.hsmainz.cs.semgis.importer.geoimporter.user.User;
import de.hsmainz.cs.semgis.importer.geoimporter.user.UserManagementConnection;
import de.hsmainz.cs.semgis.importer.geoimporter.user.UserType;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL.XMLTypes;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

/**
 * The Webservice for the GMLImporter.
 */
@Path("/service")
public class WebService {

	static JSONObject importerconf = null;
	
	/**
	 * The Webservice for the GMLImporter.
	 * @throws IOException on error
	 */
	public WebService() throws IOException {
		if (importerconf == null) {
			String text2 = new String(Files.readAllBytes(Paths.get("importerconfig.json")), StandardCharsets.UTF_8);
			importerconf = new JSONObject(text2);
			System.out.println(importerconf);
		}
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertMS")
	@Operation(
            summary = "Converts a file to RDF using an already existing and accessible mapping schema",
            description = "Converts a file to RDF using an already existing and accessible mapping schema")
    public Response importWithMappingSchema(
    		@Parameter(description="The file to be imported") @FormDataParam("file") InputStream uploadedInputStream,
    		@Parameter(description="The file metadata") @FormDataParam("file") FormDataContentDisposition fileDetail,
			@Parameter(description="The mappingschema used for import") @FormDataParam("file2") InputStream mappingprofileInputStream,
			@Parameter(description="The mappingschema file metadata") @FormDataParam("file2") FormDataContentDisposition mappingprofileDetail,
			@Parameter(description="The data format of the file which is imported") @QueryParam("format") String format,
			@Parameter(description="The data namespace to use for conversion") @DefaultValue("") @QueryParam("namespace") String namespace,
			@Parameter(description="The provider of the dataset") @DefaultValue("") @QueryParam("provider") String provider,
			@Parameter(description="The license under which to be converted file was published") @DefaultValue("") @QueryParam("license") String license,
			@DefaultValue("") @QueryParam("origin") String origin,
			@Parameter(description="The authtoken needed for authorization") @DefaultValue("") @QueryParam("authtoken") String authtoken,
			@Parameter(description="The triplestore to which to import the converted dataset") @DefaultValue("") @QueryParam("triplestore") String triplestore,
			@Parameter(description="The graph of the triplestore to which the import should occur") @DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
			@Parameter(description="The triplestore user for authentication") @DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
			@Parameter(description="The triplestore password for authentication") @DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
        	FileUtils.copyInputStreamToFile(uploadedInputStream, new File("tempfile.gml"));
			OntModel model=GMLImporter.processFile(fileDetail.getType(), "tempfile.gml", false, false, namespace,provider,license,origin);
			System.out.println("Finished the conversion");
			StreamingOutput stream = new StreamingOutput() {
			    @Override
			    public void write(OutputStream os) throws IOException,
			    WebApplicationException {
			      Writer writer = new BufferedWriter(new OutputStreamWriter(os));
			      model.write(writer, "TTL");
			    }
			  };
			  return Response.ok(stream).type("text/ttl").build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			 return Response.ok("").build();	
		} 	       	
	}
	
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/analyzeFile")
	@Operation(
            summary = "Analyzes a file structure in order to create a mapping schema",
            description = "Analyzes a file structure in order to create a mapping schema")
    public Response analyzeFile(
    		@Parameter(description="The file to be analyzed") @FormDataParam("file") InputStream uploadedInputStream,
    		@Parameter(description="The file metadata") @FormDataParam("file") FormDataContentDisposition fileDetail,
    		@Parameter(description="The dataformat of the file upload") @FormDataParam("fileformat") String fileFormat,
    		@Parameter(description="The serviceurl if data is being loaded from a WFS feature type") @FormDataParam("serviceurl") String serviceurl,
			@Parameter(description="The authtoken used for authorization") @DefaultValue("") @QueryParam("authtoken") String authtoken) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        System.out.println(fileFormat);
        try {
			String ext=fileFormat.toUpperCase();
        	File file=new File("tempfile."+ext);
        	FileUtils.copyInputStreamToFile(uploadedInputStream, file);
        	AbstractFileFeatureStoreFactory fac;
        	FeatureStore dataStore=null;
        	Session session;
        	switch(fileFormat) {
        	case "geojson":
        		fac=new GeoJSONFeatureStoreFactory();
            	dataStore=fac.createDataStore(file.toURI());
            	session=dataStore.createSession(true);
        		break;
        	case "csv":
        		fac=new CSVFeatureStoreFactory();
            	dataStore=fac.createDataStore(file.toURI());
            	session=dataStore.createSession(true);
        		break;
        	case "shp":
        		fac=new ShapefileFeatureStoreFactory();
            	dataStore=fac.createDataStore(file.toURI());
    			session=dataStore.createSession(true);
        		break;
        	case "wfs":
        		WFSFeatureStoreFactory fac2=new WFSFeatureStoreFactory();
        		Map<String,String> map=new TreeMap<String,String>();
        		map.put("url",serviceurl);
        		WebFeatureClient test = fac2.create(map);
    			session=test.createSession(true);
        		break;
        	default: 
        		return Response.ok("").build();
        	}    	
			FeatureCollection collect;
			collect = session.getFeatureCollection(QueryBuilder.all(dataStore.getNames().iterator().next()));	
			System.out.println("FeatureCollection created!");
			JSONObject result=new JSONObject();
			result.put("properties",new JSONArray());
			for(PropertyType prop:collect.getFeatureType().getProperties(false)) {
				result.getJSONArray("properties").put(prop.getName());
			}		
			return Response.ok(result.toString(2)).type(MediaType.APPLICATION_JSON).build();
		} catch (IOException | DataStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			 return Response.ok(e.getMessage()).build();	
		}	       	
	}
	

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertGML")
	@Operation(
            summary = "Converts a GML document to RDF",
            description = "Converts a GML document to RDF given a few parameters and constraints")
    public Response importKnownFormat(
    		@Parameter(description="The GML file to be converted") @FormDataParam("file") InputStream uploadedInputStream,
    		@Parameter(description="The GML file metadata") @FormDataParam("file") FormDataContentDisposition fileDetail,
    		@Parameter(description="The dataformat of the file upload") @DefaultValue("gml") @QueryParam("format") String format, 
			@Parameter(description="The data namespace used for conversion") @DefaultValue("") @QueryParam("namespace") String namespace,
			@Parameter(description="The provider of the GML dataset") @DefaultValue("") @QueryParam("provider") String provider,
			@Parameter(description="The license under which the GML file was published") @DefaultValue("") @QueryParam("license") String license,
			@Parameter(description="The origin of the GML file") @DefaultValue("") @QueryParam("origin") String origin,
			@Parameter(description="The authtoken needed for authorization") @DefaultValue("") @QueryParam("authtoken") String authtoken,
			@Parameter(description="The triplestore to which to import the converted dataset") @DefaultValue("") @QueryParam("triplestore") String triplestore,
			@Parameter(description="The graph of the triplestore to which the import should occur") @DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
			@Parameter(description="The triplestore user for authentication") @DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
			@Parameter(description="The triplestore password for authentication") @DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
        	System.out.println("Namespace: "+namespace+" Provider: "+provider+" License: "+license+" Origin:"+origin);
        	FileUtils.copyInputStreamToFile(uploadedInputStream, new File("tempfile.gml"));
			OntModel model=GMLImporter.processFile(fileDetail.getType(), "tempfile.gml", false, false, 
					namespace,provider,license,origin);
			System.out.println("Finished the conversion");
			StreamingOutput stream = new StreamingOutput() {
			    @Override
			    public void write(OutputStream os) throws IOException,
			    WebApplicationException {
			      Writer writer = new BufferedWriter(new OutputStreamWriter(os));
			      model.write(writer, "TTL");
			    }
			  };
			  return Response.ok(stream).type("text/ttl").build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			 return Response.ok("").build();	
		} 	       	
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertGeoJSON")
	@Operation(
            summary = "Converts a GeoJSON document to RDF",
            description = "Converts a GeoJSON document to RDF given a few parameters and constraints")
    public Response convertGeoJSON(
    		@Parameter(description="The GeoJSON file to be converted") @FormDataParam("file") InputStream uploadedInputStream,
    		@Parameter(description="Metadata of the GeoJSON file") @FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("format") String format, 
    		@Parameter(description="The data namespace used for conversion") @QueryParam("namespace") String namespace, 
    		@Parameter(description="The classname of the contents of the GeoJSON file") @QueryParam("classname") String classname, 
    		@Parameter(description="The column including the individual id of the dataset") @QueryParam("indid") String indid,
    		@Parameter(description="The authtoken needed for authorization") @DefaultValue("") @QueryParam("authtoken") String authtoken,
    		@Parameter(description="The triplestore to which to import the converted dataset") @DefaultValue("") @QueryParam("triplestore") String triplestore,
    		@Parameter(description="The graph of the triplestore to which the import should occur") @DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
    		@Parameter(description="The triplestore user for authentication") @DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
    		@Parameter(description="The triplestore password for authentication") @DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
			String theString = IOUtils.toString(uploadedInputStream, "UTF-8");
			JSONObject obj=new JSONObject(theString);
			OntModel model=GeoJSONParser.geoJSONToTTL(obj, namespace, classname, indid);
			System.out.println("Finished the conversion");
			StreamingOutput stream = new StreamingOutput() {
			    @Override
			    public void write(OutputStream os) throws IOException,
			    WebApplicationException {
			      Writer writer = new BufferedWriter(new OutputStreamWriter(os));
			      model.write(writer, "TTL");
			    }
			  };
			  if(!triplestore.isEmpty() && !triplestoregraph.isEmpty() && !triplestoreuser.isEmpty() && !triplestorepasswd.isEmpty()) {
				  storeGraphResult(model,triplestore,triplestoregraph, triplestoreuser,triplestorepasswd);
			  }
			  return Response.ok(stream).type("text/ttl").build();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.ok("Conversion failed").build();
		} 
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertExistingSchema")
    public Response convertWithExistingSchema(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@FormDataParam("format") String fileFormat,
			@FormDataParam("mappingprofile") String schemaurl,
			@DefaultValue("") @QueryParam("authtoken") String authtoken) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
        	System.out.println("File Format: "+fileFormat);
			File file=new File("tempfile."+fileFormat);
			String serviceurl="";
        	FileUtils.copyInputStreamToFile(uploadedInputStream, file);
			Config config=new Config();
			System.out.println("Schema URL: "+schemaurl);
			XMLReader myReader = XMLReaderFactory.createXMLReader();
			myReader.setContentHandler(config);
			myReader.parse(new InputSource(new URL(schemaurl).openStream()));
			Main main=new Main();
			OntModel result=main.processFeatures("tempfile."+fileFormat, "result.ttl", config,fileFormat,serviceurl);
			StreamingOutput stream = new StreamingOutput() {
			    @Override
			    public void write(OutputStream os) throws IOException,
			    WebApplicationException {
			      Writer writer = new BufferedWriter(new OutputStreamWriter(os));
			      result.write(writer, "TTL");
			    }
			  };
			return Response.ok(stream).type("text/ttl").build();
		} catch (IOException | SAXException | DataStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return Response.ok("Conversion failed").build();
		} 
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/saveMappingSchema")
	@Operation(
            summary = "Saves a created mapping schema on the server",
            description = "Saves a created mapping schema on the server")
    public Response saveMappingSchema(
    		@Parameter(description="The mappingschema to be saved") @FormDataParam("data") String fileAsString,
    		@Parameter(description="The name of the mappingschema to be saved") @FormDataParam("filename") FormDataContentDisposition fileName,
    		@Parameter(description="An authtoken needed for authorization") @DefaultValue("") @QueryParam("authtoken") String authtoken) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        System.out.println("File Format: "+fileName);
		File file=new File(importerconf.get("mappingfolder")+"/"+fileName);
		User user=UserManagementConnection.getInstance().loginAuthToken(authtoken);
		if(user!=null && (user.getUserlevel()==UserType.Configurer || user.getUserlevel()==UserType.Administrator)) {
			String serviceurl="";
			try {
				FileWriter writer=new FileWriter(file);
				writer.write(fileAsString);
				writer.close();
			} catch (IOException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
        return Response.ok("Saved").type("text/ttl").build();
	}

	@GET
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getMappingSchemas")
	@Operation(
            summary = "Gets a list of all mapping schemas accessible to the server",
            description = "Gets a list of all mapping schemas accessible to the server")
    public Response getMappingSchemas() { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        return Response.ok(importerconf.getJSONObject("mappings").toString()).type(MediaType.APPLICATION_JSON).build();
	}
	
	
	private static String readAllBytesJava7(String filePath) 
	{
		String content = "";

		try 
		{
			content = new String ( Files.readAllBytes( Paths.get(filePath) ) );
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		return content;
	}
	
	@GET
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getMappingSchema/{mappingschema}")
	@Operation(
            summary = "Gets a representation of a given mapping schema from the server",
            description = "Gets a representation of a given mapping schema from the server")
    public Response getMappingSchema(
    		@Parameter(description="The name of the mapping schema to be retrieved") @PathParam("mappingschema") String mappingschema) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        File folder=new File(importerconf.getString("mappingfolder")+"/"+mappingschema+".xml");
        if(!folder.exists()) {
        	throw new NotFoundException();
        }       
        return Response.ok(readAllBytesJava7(importerconf.getString("mappingfolder")+"/"+mappingschema+".xml")).type(MediaType.APPLICATION_XML).build();
	}
	
	@GET
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getXSLTemplates")
	@Operation(
            summary = "Gets XSL templates available on the server",
            description = "Gets XSL templates available on the server")
    public Response getXSLTemplates() { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        File folder=new File("xsl");
        JSONArray result=new JSONArray();
        for(String file:folder.list()) {
        	if(file.endsWith(".xsl")) {
        		result.put(file);
        	}
        }
        return Response.ok(result.toString()).type(MediaType.APPLICATION_JSON).build();
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/login")
	@Operation(
            summary = "Login function for user login",
            description = "Login function for user login, returns an authtoken if successful")
    public Response login(@FormDataParam("username") String username,@FormDataParam("password") String password) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        User user=UserManagementConnection.getInstance().login(username, password);
        if(user!=null) {
        	return Response.ok(user.authToken).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.ok("").type(MediaType.TEXT_PLAIN).build();
	}
	
	/**
	 * Imports a file using a mapping schema. 
	 * @param uploadedInputStream The input stream in which the file upload is stored
	 * @param fileDetail Details about the uploaded file
	 * @param xsl The name of the XSL stylesheet used for transformation
	 * @param formatname
	 * @param language
	 * @param namespace
	 * @param triplestore
	 * @param triplestoregraph
	 * @param triplestoreuser
	 * @param triplestorepasswd
	 * @return
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertSchemaToOWL")
	@Operation(
            summary = "Converts an XSD file to OWL",
            description = "Converts an XSD file to OWL")
    public Response importWithMappingSchema(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("xsl") String xsl,
			@QueryParam("formatname") String formatname,
			@DefaultValue("en") @QueryParam("language") String language,
			@DefaultValue("") @QueryParam("namespace") String namespace,
			@DefaultValue("") @QueryParam("triplestore") String triplestore,
			@DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
			@DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
			@DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        XSD2OWL xsd2owl=new XSD2OWL();
		xsd2owl.transformercounter = 0;
		String owlfile = "xsd/"+formatname+"/"+formatname+"_testx";
		String xsdfile = "xsd/"+formatname+"/XPlanGML.xsd";
		try {
			xsd2owl.transform(xsdfile, owlfile, xsl, namespace, formatname, null, language);
			xsd2owl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", formatname, namespace);
			xsd2owl.integrateCodeList(new File("xsd/"+formatname+"/XPlanGML_CodeLists.xml"), owlfile + ".owl",
					namespace, "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN, formatname);
		} catch (TransformerFactoryConfigurationError | TransformerException | XPathExpressionException | SAXException | IOException | ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response.ok("").type("text/ttl").build();
	}
	
	public void storeGraphResult(OntModel model,String triplestore,String graph, String triplestoreuser,String triplestorepasswd) {
		RDFConnection builder = RDFConnectionFactory.connect(triplestore);
		builder.load(model);		
	}
	
}
