package de.hsmainz.cs.semgis.importer.geoimporter.webservice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

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
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.hsmainz.cs.semgis.importer.geoimporter.importer.GMLImporter;
import de.hsmainz.cs.semgis.importer.geoimporter.parser.GeoJSONParser;
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
			@DefaultValue("") @QueryParam("namespace") String namespace) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
        	FileUtils.copyInputStreamToFile(uploadedInputStream, new File("tempfile.gml"));
			OntModel model=GMLImporter.processFile(fileDetail.getType(), "tempfile.gml", false, false, namespace);
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
	@Path("/convertGML")
    public Response importKnownFormat(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@DefaultValue("gml") @QueryParam("format") String format, 
			@DefaultValue("") @QueryParam("namespace") String namespace) { 
		final String dir = System.getProperty("user.dir");
        System.out.println("current dir = " + dir); 
        try {
        	FileUtils.copyInputStreamToFile(uploadedInputStream, new File("tempfile.gml"));
			OntModel model=GMLImporter.processFile(fileDetail.getType(), "tempfile.gml", false, false, namespace);
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
			@QueryParam("indid") String indid) { 
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
			  return Response.ok(stream).type("text/ttl").build();
		} catch (IOException e) {
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
	@Produces({"text/ttl"})
	@Path("/convertSchemaToOWL")
    public Response importWithMappingSchema(@FormDataParam("file") InputStream uploadedInputStream,
			@FormDataParam("file") FormDataContentDisposition fileDetail,
			@QueryParam("xsl") String xsl,
			@QueryParam("formatname") String formatname,
			@DefaultValue("en") @QueryParam("language") String language,
			@DefaultValue("") @QueryParam("namespace") String namespace) { 
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
	
}
