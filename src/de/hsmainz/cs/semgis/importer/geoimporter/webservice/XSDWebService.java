package de.hsmainz.cs.semgis.importer.geoimporter.webservice;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.json.JSONArray;
import org.xml.sax.SAXException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL;
import de.hsmainz.cs.semgis.importer.schemaconverter.XSD2OWL.XMLTypes;

@Path("/service")
public class XSDWebService {

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
