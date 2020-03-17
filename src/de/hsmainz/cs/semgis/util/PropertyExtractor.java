package de.hsmainz.cs.semgis.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;

import org.apache.jena.ontology.AnnotationProperty;
import org.apache.jena.ontology.DatatypeProperty;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntProperty;
import org.apache.jena.rdf.model.ModelFactory;
import org.json.JSONObject;

public class PropertyExtractor {
	
	OntModel model;
	
	public PropertyExtractor(OntModel model) {
		this.model=model;
	}
	
	public JSONObject getPropertyList() {
		JSONObject result=new JSONObject();
		Iterator<OntProperty> propit=model.listAllOntProperties();
		while(propit.hasNext()) {
			OntProperty prop=propit.next();
			String label=prop.getLabel("en");
			if(label!=null) {
				result.put(label,prop.getURI());
			}else {
				if(prop.getURI().contains("#")) {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('#')+1),prop.getURI());
				}else {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('/')+1),prop.getURI());
				}
			}
		}
		return result;
	}
	
	public JSONObject getObjPropertyList() {
		JSONObject result=new JSONObject();
		Iterator<ObjectProperty> propit=model.listObjectProperties();
		while(propit.hasNext()) {
			ObjectProperty prop=propit.next();
			String label=prop.getLabel("en");
			if(label!=null) {
				result.put(label,prop.getURI());
			}else {
				if(prop.getURI().contains("#")) {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('#')+1),prop.getURI());
				}else {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('/')+1),prop.getURI());
				}
			}
		}
		return result;
	}
	
	public JSONObject getDataPropertyList() {
		JSONObject result=new JSONObject();
		Iterator<DatatypeProperty> propit=model.listDatatypeProperties();
		while(propit.hasNext()) {
			DatatypeProperty prop=propit.next();
			String label=prop.getLabel("en");
			if(label!=null) {
				result.put(label,prop.getURI());
			}else {
				if(prop.getURI().contains("#")) {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('#')+1),prop.getURI());
				}else {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('/')+1),prop.getURI());
				}
			}
		}
		return result;
	}
	
	public JSONObject getAnnotationPropertyList() {
		JSONObject result=new JSONObject();
		Iterator<AnnotationProperty> propit=model.listAnnotationProperties();
		while(propit.hasNext()) {
			AnnotationProperty prop=propit.next();
			String label=prop.getLabel("en");
			if(label!=null) {
				result.put(label,prop.getURI());
			}else {
				if(prop.getURI().contains("#")) {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('#')+1),prop.getURI());
				}else {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('/')+1),prop.getURI());
				}
			}
		}
		return result;
	}
	
	public JSONObject getConceptList() {
		JSONObject result=new JSONObject();
		Iterator<OntClass> propit=model.listClasses();
		while(propit.hasNext()) {
			OntClass prop=propit.next();
			String label=prop.getLabel("en");
			if(label!=null) {
				result.put(label,prop.getURI());
			}else {
				if(prop.getURI()!=null && prop.getURI().contains("#")) {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('#')+1),prop.getURI());
				}else if(prop.getURI()!=null) {
					result.put(prop.getURI().substring(prop.getURI().lastIndexOf('/')+1),prop.getURI());
				}
			}
		}
		return result;
	}
	
	public JSONObject getAll() {
		JSONObject result=new JSONObject();
		result.put("objproperties",this.getObjPropertyList());
		result.put("dataproperties",this.getDataPropertyList());
		result.put("classes",this.getConceptList());
		return result;
	}
	
	public static void main(String[] args) throws IOException {
		OntModel model=ModelFactory.createOntologyModel();
		model.read("C:\\Users\\timo.homburg\\git\\SemGISOntologies\\AAA-Data\\aaa7.ttl");
		PropertyExtractor propex=new PropertyExtractor(model);
		JSONObject extracted=propex.getAll();
		FileWriter writer=new FileWriter(new File("aaa7.json"));
		writer.write(extracted.toString(2));
		writer.close();
	}

}
