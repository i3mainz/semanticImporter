package de.hsmainz.cs.semgis.importer.geoimporter.connector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.jena.ontology.DatatypeProperty;
//TODO: refactor
import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.ObjectProperty;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;



public class TripleStoreConnector {

    String prefixList;

    public TripleStoreConnector(String prefixFilename)
    {
        try {
            byte[] encoded;
            encoded = Files.readAllBytes(Paths.get(prefixFilename));
            prefixList = new String(encoded, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            // TODO Auto-generated catch block
            System.out.println(ex.getMessage());
        }
       
    }
    
    public void addOntModelToTripleStore(OntModel model,String triplestore) {
    	RDFConnection connection=RDFConnectionFactory.connect(triplestore);
    	connection.load(model);
    	connection.close();
    }

    //Remarks: The query result is expected to consist of exactly one data row and data column.
	public String executeSPARQLQuery(String queryFormatString, String endpoint, Map<String, String> properties) {
        QueryExecution qexec = null;
        try
        {
            String queryString  = new StrSubstitutor(properties, "%%", "%%").replace(queryFormatString);
            //System.out.println("Property Query: "+queryString);
            //String url = URLEncoder.encode(endpoint, StandardCharsets.UTF_8.toString());
            qexec = QueryExecutionFactory.sparqlService(endpoint, prefixList + queryString);
		    org.apache.jena.query.ResultSet results = qexec.execSelect();
            QuerySolution resultSet = results.next();
            String columnName = resultSet.varNames().next().toString();
			return resultSet.get(columnName).toString();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			return null;
		} finally {
            if (null != qexec) 
                qexec.close();
		}
    }
    
    //TODO refactoring: move control logic part to "import heuristics"
    public class GeographicalResult
    {
        public String ind;
        public String classType;
        public Literal label;
    }

    public List<GeographicalResult> getConceptsGeoGraphicallyNearToWithAddress(Double lat, Double lon,Double radius, String endpoint, String classType) {
        QueryExecution qexec = null;
        try
        {
            //String url = URLEncoder.encode(endpoint, StandardCharsets.UTF_8.toString());

            StringBuilder classOfResourceQuery=new StringBuilder();
            classOfResourceQuery.append(prefixList);
            //classOfResourceQuery.append("SELECT DISTINCT ?class ?label ?s WHERE { "+System.lineSeparator());
            classOfResourceQuery.append("SELECT DISTINCT ?label ?s WHERE { "+System.lineSeparator());
            classOfResourceQuery.append("?s rdf:type <" + classType + "> ."+System.lineSeparator());
            classOfResourceQuery.append("?s rdfs:label ?label ."+System.lineSeparator());
            classOfResourceQuery.append("?s geom:geometry ?geom ."+System.lineSeparator());
            classOfResourceQuery.append("?geom ogc:asWKT ?g ."+System.lineSeparator());
            //classOfResourceQuery.append("Filter(bif:st_intersects (?g, bif:st_point ("+lon+","+lat+"), "+radius+")) ");
            //classOfResourceQuery.append("Filter(bif:st_contains (?g, bif:st_point ("+lon+","+lat+"),  "+radius+")) ");
            //classOfResourceQuery.append("Filter(bif:st_contains (?g, bif:st_point ("+lon+","+lat+"))) ");
            classOfResourceQuery.append("Filter(bif:st_contains (bif:st_point ("+lon+","+lat+"), ?g)) ");
            
           //classOfResourceQuery.append("FILTER NOT EXISTS { ?x rdfs:subClassOf ?class. FILTER (?x != ?class) }}");
            classOfResourceQuery.append("}");
            //System.out.println("Query: "+classOfResourceQuery.toString());
            qexec = new QueryEngineHTTP(endpoint, classOfResourceQuery.toString());
            org.apache.jena.query.ResultSet results = qexec.execSelect();
            
            List<GeographicalResult> result=new LinkedList<GeographicalResult>();
            while(results.hasNext()){
                QuerySolution solu=results.next();
                //System.out.println("Class? "+solu.get("?class"));
                //if(solu.get("?class").toString().contains(this.prefix.iterator().next().toString())){
                    //result.add(solu.get("?class").toString(),"",solu.get("?s").toString(),solu.get("?label").toString(),indid.toString(),solu.get("?g").toString()));
                    
                    GeographicalResult r = new GeographicalResult();
                    r.ind = solu.get("?s").toString();
                    r.label = solu.getLiteral("?label");
                    r.classType = classType; //solu.get("?class").toString();
                    result.add(r);

                    /*
                    System.out.println("Valid Class? "+solu.get("?class"));
                    System.out.println("Label? "+solu.get("?label"));
                    System.out.println("Resource: "+solu.get("?s"));
                    System.out.println("Geometry: "+solu.get("?g"));*/
                //}
            }
            return result;
        }
        catch (Exception ex) {
			System.out.println(ex.getMessage());
			return null;
		} finally {
            if (null != qexec) 
                qexec.close();
        }
    }
    
    public void createWDOntologyFromInd(String endpoint, Resource ind, OntModel model,String superClass, String defaultNamespace) {
        QueryExecution qexec = null;
        try
        {
            StringBuilder classOfResourceQuery=new StringBuilder();
            classOfResourceQuery.append(prefixList);
            classOfResourceQuery.append("SELECT DISTINCT ?obj ?pred ?class ?datobj ?predobLabel ?objLabel ?classLabel WHERE { "+System.lineSeparator());
            classOfResourceQuery.append("<"+ind.getURI()+"> ?pred ?obj."+System.lineSeparator());
            classOfResourceQuery.append("?pred rdf:type ?datobj ."+System.lineSeparator());	
            classOfResourceQuery.append("?predob wikibase:directClaim ?pred ."+System.lineSeparator());	
            classOfResourceQuery.append("OPTIONAL {?obj wdt:P31 ?class .}"+System.lineSeparator());
            classOfResourceQuery.append("SERVICE wikibase:label { bd:serviceParam wikibase:language \"en\". }}"+System.lineSeparator());
            
            //System.out.println(classOfResourceQuery.toString());
            qexec = new QueryEngineHTTP(endpoint, classOfResourceQuery.toString());
            ResultSet results = qexec.execSelect();
            OntClass superCls=model.createClass(defaultNamespace + superClass);
            while(results.hasNext()){
                QuerySolution solu=results.next();
                //System.out.println(solu.get("?pred")+" - "+solu.get("?obj")+" - "+solu.get("?datobj")+" - "+solu.get("?class"));
                if(solu.get("?datobj").asResource().getURI().contains("ObjectProperty")) {
                    OntClass cls;
                    if(solu.get("?class") != null)
                    {
                        cls=model.createClass(solu.get("?class").asResource().getURI());
                        cls.addLabel(solu.getLiteral("?classLabel"));
                    }
                    else
                    {
                        cls=model.createClass(defaultNamespace + "Unresolved");
                    }
                    cls.addSuperClass(superCls);
                    Individual indi=cls.createIndividual(solu.get("?obj").asResource().getURI());
                    indi.addLabel(solu.getLiteral("?objLabel"));	
                    ObjectProperty oprop=model.createObjectProperty(solu.get("?pred").asResource().getURI());
                    oprop.addLabel(solu.get("?predobLabel").asLiteral());
                    ind.addProperty(oprop, indi);
                }else if(solu.get("?datobj").asResource().getURI().contains("DatatypeProperty")){
                    DatatypeProperty dprop = model.createDatatypeProperty(solu.get("?pred").asResource().getURI());
                    dprop.addLabel(solu.get("?predobLabel").asLiteral());
                    ind.addProperty(dprop, solu.get("?obj"));
                }
            }
        }catch(Exception e) {
        	e.printStackTrace();
        }
        finally {
            if (null != qexec) 
                qexec.close();
        }
	}
	
	public void createOntologyFromInd(String endpoint, Individual ind, OntModel model,String superClass, String defaultNamespace) {
        QueryExecution qexec = null;
        try
        {
            StringBuilder classOfResourceQuery=new StringBuilder();
            classOfResourceQuery.append(prefixList);
            classOfResourceQuery.append("SELECT DISTINCT ?pred ?obj ?class WHERE { "+System.lineSeparator());
            classOfResourceQuery.append("<"+ind.getURI()+"> ?pred ?obj."+System.lineSeparator());
            classOfResourceQuery.append("OPTIONAL {?obj rdf:type ?class .}"+System.lineSeparator());
        
            //classOfResourceQuery.append("?pred rdf:type ?datobj ."+System.lineSeparator());	
            /*classOfResourceQuery.append("OPTIONAL {?pred rdfs:label ?predLabel .}"+System.lineSeparator());	
                classOfResourceQuery.append("OPTIONAL {?obj rdfs:label ?objLabel .}"+System.lineSeparator());
            classOfResourceQuery.append("OPTIONAL {?class rdfs:label ?classLabel .}"+System.lineSeparator());
            */classOfResourceQuery.append("}"+System.lineSeparator());
            
            //System.out.println(classOfResourceQuery.toString());
            qexec = new QueryEngineHTTP(endpoint, classOfResourceQuery.toString());
            ResultSet results = qexec.execSelect();
            OntClass superCls=model.createClass(defaultNamespace + superClass);
            while(results.hasNext()){
                QuerySolution solu=results.next();
                //System.out.println(solu.get("?pred")+" - "+solu.get("?obj")+" - "+solu.get("?datobj")+" - "+solu.get("?class"));
                if(solu.get("?obj").isResource()) {
                    OntClass cls;
                    if(solu.get("?pred").toString().startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#")) {
                        cls=model.createClass(solu.get("?obj").asResource().getURI());
                        Property oprop=model.createProperty(solu.get("?pred").asResource().getURI());
                        ind.addProperty(oprop, cls);
                    }else {
                        if(solu.get("?class")!=null) {
                            cls=model.createClass(solu.get("?class").asResource().getURI());
                            if(solu.get("?classLabel")!=null)
                                cls.addLabel(solu.getLiteral("?classLabel"));
                        }else {
                            cls=model.createClass(defaultNamespace + "Unresolved");
                        }
                        Individual indi=cls.createIndividual(solu.get("?obj").asResource().getURI());
                        if(solu.getLiteral("?objLabel")!=null)
                            indi.addLabel(solu.getLiteral("?objLabel"));	
                        ObjectProperty oprop=model.createObjectProperty(solu.get("?pred").asResource().getURI());
                        if(solu.getLiteral("?predLabel")!=null)
                            oprop.addLabel(solu.get("?predLabel").asLiteral());
                        ind.addProperty(oprop, indi);
                    }
                    cls.addSuperClass(superCls);
                    
                }else if (solu.get("?pred").toString().startsWith("http://www.w3.org/1999/02/22-rdf-syntax-ns#")
                        || solu.get("?pred").toString().startsWith("http://www.w3.org/2000/01/rdf-schema#")){
                    ind.addProperty(model.createAnnotationProperty(solu.get("?pred").asResource().getURI()), solu.getLiteral("?obj"));

                }else {
                    ind.addProperty(model.createDatatypeProperty(solu.get("?pred").asResource().getURI()), solu.getLiteral("?obj"));
                }
            }
        }catch(Exception e) {
        	e.printStackTrace();
        }
        finally {
            if (null != qexec) 
                qexec.close();
        }
	}

}
	
