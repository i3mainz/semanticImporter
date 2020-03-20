package de.hsmainz.cs.semgis.importer.geoimporter.webservice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.PropertyType;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.hsmainz.cs.semgis.importer.geoimporter.Main;
import de.hsmainz.cs.semgis.importer.geoimporter.config.Config;
import de.hsmainz.cs.semgis.importer.geoimporter.importer.GMLImporter;
import de.hsmainz.cs.semgis.importer.geoimporter.parser.GeoJSONParser;
import de.hsmainz.cs.semgis.importer.geoimporter.user.User;
import de.hsmainz.cs.semgis.importer.geoimporter.user.UserManagementConnection;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL.XMLTypes;

@Path("/service")
public class WebService {

	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertMS")
    public Response importWithMappingSchema(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@FormDataParam("file") InputStream mappingprofileInputStream,
			@FormDataParam("file") FormDataContentDisposition mappingprofileDetail,
			@QueryParam("format") String format,
			@DefaultValue("") @QueryParam("namespace") String namespace,
			@DefaultValue("") @QueryParam("provider") String provider,
			@DefaultValue("") @QueryParam("license") String license,
			@DefaultValue("") @QueryParam("origin") String origin,
			@DefaultValue("") @QueryParam("authtoken") String authtoken,
			@DefaultValue("") @QueryParam("triplestore") String triplestore,
			@DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
			@DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
			@DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
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
    public Response analyzeFile(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@FormDataParam("fileformat") String fileFormat,
			@FormDataParam("serviceurl") String serviceurl,
			@DefaultValue("") @QueryParam("authtoken") String authtoken) { 
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
    public Response importKnownFormat(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@DefaultValue("gml") @QueryParam("format") String format, 
			@DefaultValue("") @QueryParam("namespace") String namespace,
			@DefaultValue("") @QueryParam("provider") String provider,
			@DefaultValue("") @QueryParam("license") String license,
			@DefaultValue("") @QueryParam("origin") String origin,
			@DefaultValue("") @QueryParam("authtoken") String authtoken,
			@DefaultValue("") @QueryParam("triplestore") String triplestore,
			@DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
			@DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
			@DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
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
    public Response convertGeoJSON(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("format") String format, 
			@QueryParam("namespace") String namespace, 
			@QueryParam("classname") String classname, 
			@QueryParam("indid") String indid,
			@DefaultValue("") @QueryParam("authtoken") String authtoken,
			@DefaultValue("") @QueryParam("triplestore") String triplestore,
			@DefaultValue("") @QueryParam("triplestoregraph") String triplestoregraph,
			@DefaultValue("") @QueryParam("triplestoreuser") String triplestoreuser,
			@DefaultValue("") @QueryParam("triplestorepasswd") String triplestorepasswd) { 
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
    public Response convertGeoJSON(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("mappingschema") String schemaurl,
			@DefaultValue("") @QueryParam("authtoken") String authtoken) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
			String theString = IOUtils.toString(uploadedInputStream, "UTF-8");
        	FileUtils.copyInputStreamToFile(uploadedInputStream, new File("tempfile.gml"));
			Config config=new Config();
			XMLReader myReader = XMLReaderFactory.createXMLReader();
			myReader.setContentHandler(config);
			myReader.parse(new InputSource(new URL(schemaurl).openStream()));
			Main main=new Main();
			main.processFeatures("tempfile.gml", "result.ttl", config);
			OntModel result=null;
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
	
	@GET
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getXSLTemplates")
    public Response importWithMappingSchema() { 
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
    public Response login(@FormDataParam("username") String username,@FormDataParam("password") String password) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        User user=UserManagementConnection.getInstance().login(username, password);
        if(user!=null) {
        	return Response.ok(user.authToken).type(MediaType.TEXT_PLAIN).build();
        }
        return Response.ok("").type(MediaType.TEXT_PLAIN).build();
	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertSchemaToOWL")
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
