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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.jena.ontology.OntModel;
import org.json.JSONObject;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.hsmainz.cs.semgis.importer.geoimporter.importer.GMLImporter;
import de.hsmainz.cs.semgis.importer.geoimporter.parser.GeoJSONParser;

@Path("/")
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
	
}
