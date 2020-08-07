package de.hsmainz.cs.semgis.importer.schemaconverter.webservice;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
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
import org.apache.jena.ontology.OntModel;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import de.hsmainz.cs.semgis.importer.geoimporter.importer.GMLImporter;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL.XMLTypes;
import io.swagger.v3.oas.annotations.Parameter;

@Path("/service")
public class WebService {

	public WebService() {
		
	}
	
	/**
	 * Converts an XSD schema to OWL.
	 * @param uploadedInputStream The XSD schema which is uploaded
	 * @param fileDetail File metadata of the file being uploaded
	 * @param xsl
	 * @param formatname
	 * @param language
	 * @param namespace
	 * @return
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces({"text/ttl"})
	@Path("/convertSchemaToOWL")
    public Response importWithMappingSchema(
    		@Parameter(description="The file to be converted") @FormDataParam("file") InputStream uploadedInputStream,
    		@Parameter(description="The metainformation of the file") @FormDataParam("file") FormDataContentDisposition fileDetail,
    		@Parameter(description="The XSL stylesheet used for conversion") @QueryParam("xsl") String xsl,
			@QueryParam("formatname") String formatname,
			@Parameter(description="The language used in the dataset") @DefaultValue("en") @QueryParam("language") String language,
			@Parameter(description="The namespace to used in the conversion process") @DefaultValue("") @QueryParam("namespace") String namespace) { 
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
	
	/**
	 * Retrieves available XSL templates from the server.
	 * @return The given XSL templates as a JSON document
	 */
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
	
}
