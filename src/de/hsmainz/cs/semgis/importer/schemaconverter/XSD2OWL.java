package de.hsmainz.cs.semgis.importer.schemaconverter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.ext.DefaultHandler2;

import de.hsmainz.cs.semgis.util.Extension;
import net.sf.saxon.TransformerFactoryImpl;

import org.apache.jena.iri.IRI;
import org.apache.jena.iri.IRIFactory;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.FileManager;

import tr.com.srdc.ontmalizer.XML2OWLMapper;
import tr.com.srdc.ontmalizer.XSD2OWLMapper;

public class XSD2OWL extends AbstractTransformer {

	public enum XMLTypes {
		XPLAN, XPLAN4, AAA, XPLAN5,XKATASTROPHENHILFE
	};

	public Integer transformercounter;

	@Override
	public void transform(String source, String destination, String xsldoc, String baseURI, String superClass,
			String superClass2, String commentlang) throws FileNotFoundException, TransformerConfigurationException,
					TransformerFactoryConfigurationError, TransformerException {
		System.out.println("Source: " + source + " Destination: " + destination+" BaseURI: "+baseURI);
		try {
			URI uri=new URI(baseURI);
		
		File fIn = new File(source);
		InputStreamReader input = new InputStreamReader(new FileInputStream(fIn), StandardCharsets.UTF_8);
		TransformerFactory tFactory = new net.sf.saxon.TransformerFactoryImpl();
		Source xmlSource = new StreamSource(input);
		Source xslSource = new StreamSource(new File(xsldoc));
		Transformer transformer = tFactory.newTransformer(xslSource);
		transformer.setParameter("ontologyid", superClass != null ? superClass : superClass2);
		transformer.setParameter("baseURI", uri.toString());
		transformer.setParameter("commentlang", commentlang);
		transformer.setParameter("superClass", superClass);
		transformer.setParameter("superClass2", superClass2);
		File fOut = new File(destination + ".owl");

		transformer.transform(xmlSource, new StreamResult(fOut));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void integrateCodeList(String codeListPath, String modelPath, String uriPrefix, String superClass)
			throws SAXException, IOException, ParserConfigurationException, TransformerException {
		File fIn = new File(codeListPath);

		InputStreamReader input = new InputStreamReader(new FileInputStream(fIn), StandardCharsets.UTF_8);

		TransformerFactory tFactory = new net.sf.saxon.TransformerFactoryImpl();
		Source xmlSource = new StreamSource(input);
		Source xslSource = new StreamSource(new File("xsl/xplancodelist2owl.xsl"));
		Transformer transformer = tFactory.newTransformer(xslSource);

		File fOut = new File("temp.xml");

		transformer.transform(xmlSource, new StreamResult(fOut));
		this.mergeCodeListAndTransform(modelPath, "temp.xml", modelPath, superClass, uriPrefix);
	}

	/**
	 * Integrates two different namespaces within the same owl file by creating
	 * sameAs, equivalentProperty, equivalentClass and subPropertyOf as well as
	 * subClassOf relations in the document
	 * 
	 * @param firstXPlanOWL
	 * @param resultFile
	 * @param firstNameSpace
	 * @param secondNameSpace
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws XPathExpressionException
	 */
	public void integrateDifferentXPlanVersions(String firstXPlanOWL, String resultFile, String firstNameSpace,
			String secondNameSpace, String superClass1, String superClass2) throws SAXException, IOException,
					ParserConfigurationException, TransformerException, XPathExpressionException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		Document doc1;
		doc1 = builder.parse(new File(firstXPlanOWL));
		Map<String, Node> firstNameSpaceMap = new TreeMap<String, Node>();
		Map<String, Node> secondNameSpaceMap = new TreeMap<String, Node>();
		NodeList enumIndividials = (NodeList) xPath
				.evaluate(
						"//*[local-name()='NamedIndividual' and @*[local-name()='about' and contains(.,'"
								+ firstNameSpace + "') or contains(.,'" + secondNameSpace + "')]]",
						doc1, XPathConstants.NODESET);

		for (int i = 0; i < enumIndividials.getLength(); i++) {
			System.out.println("i: " + i + " - " + secondNameSpaceMap.size() + " - " + firstNameSpaceMap.size() + " "
					+ enumIndividials.item(i).getAttributes().getNamedItem("rdf:about").getTextContent());
			if (enumIndividials.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
					.startsWith(secondNameSpace)) {
				secondNameSpaceMap.put(
						enumIndividials.item(i).getAttributes().getNamedItem("rdf:about")
								.getTextContent().substring(enumIndividials.item(i).getAttributes()
										.getNamedItem("rdf:about").getTextContent().indexOf('#') + 1),
						enumIndividials.item(i));
			}
			if (enumIndividials.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
					.startsWith(firstNameSpace)) {
				firstNameSpaceMap.put(
						enumIndividials.item(i).getAttributes().getNamedItem("rdf:about")
								.getTextContent().substring(enumIndividials.item(i).getAttributes()
										.getNamedItem("rdf:about").getTextContent().indexOf('#') + 1),
						enumIndividials.item(i));
			}
		}
		for (String key : firstNameSpaceMap.keySet()) {
			if (secondNameSpaceMap.containsKey(key)) {
				Element elem = doc1.createElement("owl:sameAs");
				elem.setAttribute("rdf:resource", secondNameSpace + key);
				firstNameSpaceMap.get(key).appendChild(elem);
				System.out.println(firstNameSpace + key + " sameAs " + secondNameSpace + key);
			}
		}
		firstNameSpaceMap.clear();
		secondNameSpaceMap.clear();
		NodeList objProps = (NodeList) xPath
				.evaluate(
						"//*[local-name()='ObjectProperty' and @*[local-name()='about' and contains(.,'"
								+ firstNameSpace + "') or contains(.,'" + secondNameSpace + "')]]",
						doc1, XPathConstants.NODESET);
		for (int i = 0; i < objProps.getLength(); i++) {
			System.out.println("i: " + i + " - " + secondNameSpaceMap.size() + " - " + firstNameSpaceMap.size() + " "
					+ objProps.item(i).getAttributes().getNamedItem("rdf:about").getTextContent());
			if (objProps.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
					.startsWith(secondNameSpace)) {
				secondNameSpaceMap.put(
						objProps.item(i).getAttributes()
								.getNamedItem("rdf:about").getTextContent().substring(objProps.item(i).getAttributes()
										.getNamedItem("rdf:about").getTextContent().indexOf('#') + 1),
						objProps.item(i));
			}
			if (objProps.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
					.startsWith(firstNameSpace)) {
				firstNameSpaceMap.put(
						objProps.item(i).getAttributes()
								.getNamedItem("rdf:about").getTextContent().substring(objProps.item(i).getAttributes()
										.getNamedItem("rdf:about").getTextContent().indexOf('#') + 1),
						objProps.item(i));
			}
		}
		for (String key : firstNameSpaceMap.keySet()) {
			if (secondNameSpaceMap.containsKey(key)) {
				NodeList ranges = (NodeList) xPath.evaluate("/*[local-name()='range']/@*[local-name()='resource']",
						firstNameSpaceMap.get(key), XPathConstants.NODESET);
				NodeList ranges2 = (NodeList) xPath.evaluate("/*[local-name()='range']/@*[local-name()='resource']",
						secondNameSpaceMap.get(key), XPathConstants.NODESET);
				int matchesToFirst = 0;
				for (int i = 0; i < ranges.getLength(); i++) {
					for (int j = 0; j < ranges2.getLength(); j++) {
						if (ranges.item(i).getAttributes().getNamedItem("rdf:resource").getTextContent().equals(
								ranges2.item(j).getAttributes().getNamedItem("rdf:resource").getTextContent())) {
							matchesToFirst++;
							break;
						}
					}
				}
				if (matchesToFirst == ranges.getLength() && matchesToFirst == ranges2.getLength()) {
					Element elem = doc1.createElement("owl:equivalentProperty");
					elem.setAttribute("rdf:resource", secondNameSpace + key);
					firstNameSpaceMap.get(key).appendChild(elem);
					System.out.println(firstNameSpace + key + "  equivalentProperty " + secondNameSpace + key);
				} else if (matchesToFirst == ranges.getLength() && matchesToFirst < ranges2.getLength()) {
					Element elem = doc1.createElement("rdfs:subPropertyOf");
					elem.setAttribute("rdf:resource", firstNameSpace + key);
					secondNameSpaceMap.get(key).appendChild(elem);
					System.out.println(secondNameSpace + key + "  subPropertyOf " + firstNameSpace + key);
				}

			}
		}
		firstNameSpaceMap.clear();
		secondNameSpaceMap.clear();
		NodeList classes = (NodeList) xPath
				.evaluate("//*[local-name()='Class' and @*[local-name()='about' and contains(.,'" + firstNameSpace
						+ "') or contains(.,'" + secondNameSpace + "')]]", doc1, XPathConstants.NODESET);
		for (int i = 0; i < classes.getLength(); i++) {
			System.out.println("i: " + i + " - " + secondNameSpaceMap.size() + " - " + firstNameSpaceMap.size() + " "
					+ classes.item(i).getAttributes().getNamedItem("rdf:about").getTextContent());
			if (classes.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
					.startsWith(secondNameSpace)) {
				secondNameSpaceMap.put(
						classes.item(i)
								.getAttributes().getNamedItem("rdf:about").getTextContent().substring(classes.item(i)
										.getAttributes().getNamedItem("rdf:about").getTextContent().indexOf('#') + 1),
						classes.item(i));
			}
			if (classes.item(i).getAttributes().getNamedItem("rdf:about").getTextContent().startsWith(firstNameSpace)) {
				firstNameSpaceMap.put(
						classes.item(i)
								.getAttributes().getNamedItem("rdf:about").getTextContent().substring(classes.item(i)
										.getAttributes().getNamedItem("rdf:about").getTextContent().indexOf('#') + 1),
						classes.item(i));
			}
		}
		for (String key : firstNameSpaceMap.keySet()) {
			if (secondNameSpaceMap.containsKey(key)) {
				NodeList subClasses1 = (NodeList) xPath.evaluate(
						"/*[local-name()='subClassOf']/@*[local-name()='subClassOf']", firstNameSpaceMap.get(key),
						XPathConstants.NODESET);
				NodeList subClasses2 = (NodeList) xPath.evaluate(
						"/*[local-name()='subClassOf']/@*[local-name()='resource']", secondNameSpaceMap.get(key),
						XPathConstants.NODESET);
				int matchesToFirst = 0;
				for (int i = 0; i < subClasses1.getLength(); i++) {
					if (subClasses1.item(i).getAttributes().getNamedItem("rdf:resource").getTextContent()
							.equals(superClass1)) {
						matchesToFirst++;
						continue;
					}
					for (int j = 0; j < subClasses2.getLength(); j++) {
						if (subClasses1.item(i).getAttributes().getNamedItem("rdf:resource").getTextContent().equals(
								subClasses2.item(j).getAttributes().getNamedItem("rdf:resource").getTextContent())) {
							matchesToFirst++;
							break;
						}
					}
				}
				boolean subClassMatch = false;
				boolean subClassMoreMatch = false;
				if (matchesToFirst == subClasses1.getLength() && matchesToFirst == subClasses2.getLength()) {
					subClassMatch = true;
				} else if (matchesToFirst == subClasses1.getLength() && matchesToFirst < subClasses2.getLength()) {
					subClassMoreMatch = true;
				}
				if (!subClassMatch && !subClassMoreMatch)
					continue;
				NodeList restrictions1 = (NodeList) xPath.evaluate("descendant::*[local-name()='Restriction']/*",
						firstNameSpaceMap.get(key), XPathConstants.NODESET);
				NodeList restrictions2 = (NodeList) xPath.evaluate("descendant::*[local-name()='Restriction']/*",
						secondNameSpaceMap.get(key), XPathConstants.NODESET);
				System.out.println("restriction1: " + restrictions1.getLength() + " - " + restrictions2.getLength());
				int correct = 0;
				for (int i = 0; i < restrictions1.getLength(); i++) {
					Node current = restrictions1.item(i);
					boolean correcta = false;
					for (int j = 0; j < restrictions2.getLength(); j++) {
						Node current2 = restrictions2.item(j);
						System.out.println(current.getNodeName() + " - " + current2.getNodeName());
						if (current.getNodeName().equals(current2.getNodeName())) {

							for (int k = 0; k < current.getAttributes().getLength(); k++) {
								System.out.println(current.getAttributes()
										.getNamedItem(current.getAttributes().item(k).getNodeName()) + " - "
										+ current2.getAttributes()
												.getNamedItem(current.getAttributes().item(k).getNodeName()));
								String currentlocalname = current.getAttributes()
										.getNamedItem(current.getAttributes().item(k).getNodeName()).getTextContent()
										.contains(
												firstNameSpace)
														? current.getAttributes()
																.getNamedItem(
																		current.getAttributes().item(k).getNodeName())
																.getTextContent()
																.substring(current.getAttributes()
																		.getNamedItem(current.getAttributes().item(k)
																				.getNodeName())
																		.getTextContent().indexOf('#') + 1)
														: current.getAttributes()
																.getNamedItem(
																		current.getAttributes().item(k).getNodeName())
																.getTextContent();
								if (current2.getAttributes()
										.getNamedItem(current.getAttributes().item(k).getNodeName()) != null) {
									String currentlocalname2 = current2.getAttributes()
											.getNamedItem(current2.getAttributes().item(k).getNodeName())
											.getTextContent().contains(secondNameSpace)
													? current2.getAttributes()
															.getNamedItem(
																	current2.getAttributes().item(k).getNodeName())
															.getTextContent()
															.substring(current2.getAttributes()
																	.getNamedItem(current2.getAttributes().item(k)
																			.getNodeName())
																	.getTextContent().indexOf('#') + 1)
													: current.getAttributes()
															.getNamedItem(current.getAttributes().item(k).getNodeName())
															.getTextContent();
									if (!currentlocalname.equals(currentlocalname2)) {
										correcta = true;
									}

								}

							}

						}
					}
					if (correcta)
						correct++;
				}
				System.out.println("restrictionMatch: " + correct + " " + restrictions1.getLength() + " "
						+ restrictions2.getLength());
				boolean restrictionMatch = false;
				boolean restrictionMoreMatch = false;
				if (correct == restrictions1.getLength() && correct == restrictions2.getLength()) {
					restrictionMatch = true;
				} else if (correct == restrictions1.getLength() && correct < restrictions2.getLength()) {
					restrictionMoreMatch = true;
				}
				if (!restrictionMatch && !restrictionMoreMatch)
					continue;
				NodeList enum1 = (NodeList) xPath.evaluate("descendant::*[local-name()='Description']",
						firstNameSpaceMap.get(key), XPathConstants.NODESET);
				NodeList enum2 = (NodeList) xPath.evaluate("descendant::*[local-name()='Description']",
						secondNameSpaceMap.get(key), XPathConstants.NODESET);
				int matchesToFirst2 = 0;
				for (int i = 0; i < enum1.getLength(); i++) {
					// System.out.println(enum1.item(i).getNodeName()+"
					// "+enum1.item(i).getAttributes().getNamedItem("rdf:about")!=null?enum1.item(i).getAttributes().getNamedItem("rdf:about").getTextContent():"");
					if (enum1.item(i).getAttributes().getNamedItem("rdf:about") == null)
						continue;
					String aboutitem = enum1.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
							.substring(enum1.item(i).getAttributes().getNamedItem("rdf:about").getTextContent()
									.indexOf('#') + 1);
					for (int j = 0; j < enum2.getLength(); j++) {
						String aboutitem2 = enum2.item(j).getAttributes().getNamedItem("rdf:about").getTextContent()
								.substring(enum2.item(j).getAttributes().getNamedItem("rdf:about").getTextContent()
										.indexOf('#') + 1);
						// System.out.println(aboutitem+" - "+aboutitem2+" -
						// "+aboutitem.equals(aboutitem2));
						if (aboutitem.equals(aboutitem2)) {
							matchesToFirst2++;
							break;
						}
					}
				}
				System.out.println(
						"matchToFirst2: " + matchesToFirst2 + " " + enum1.getLength() + " " + enum2.getLength());
				boolean enumMatch = false;
				boolean enumMoreMatch = false;
				if (matchesToFirst2 == enum1.getLength() && matchesToFirst2 == enum2.getLength()) {
					enumMatch = true;
				} else if (matchesToFirst2 == enum1.getLength() && matchesToFirst2 < enum2.getLength()) {
					enumMoreMatch = true;
				}
				System.out.println("sub: " + subClassMatch + " - " + subClassMoreMatch);
				System.out.println("res: " + restrictionMatch + " - " + restrictionMoreMatch);
				System.out.println("enum: " + enumMatch + " - " + enumMoreMatch);
				if (subClassMatch && restrictionMatch && ((!enumMoreMatch && !enumMatch) || enumMatch)) {
					Element elem = doc1.createElement("owl:equivalentClass");
					elem.setAttribute("rdf:resource", secondNameSpace + key);
					firstNameSpaceMap.get(key).appendChild(elem);
					System.out.println(firstNameSpace + key + "  equivalentClass " + secondNameSpace + key);
				} else if (enumMoreMatch || subClassMoreMatch || restrictionMoreMatch) {
					Element elem = doc1.createElement("rdfs:subClassOf");
					elem.setAttribute("rdf:resource", firstNameSpace + key);
					secondNameSpaceMap.get(key).appendChild(elem);
					System.out.println(secondNameSpace + key + "  subClassOf " + firstNameSpace + key);
				}
			}
		}
		// write out the modified document to a new file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(doc1);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void integrateCodeList(File codeListPath, String modelPath, String uriPrefix, String xsldoc, XMLTypes type,
			String superClass) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		File fOut = new File("temp.xml");
		if (codeListPath.isDirectory()) {
			for (File file : codeListPath.listFiles()) {
				System.out.println("File: " + file.toString());
				/*
				 * InputStreamReader input = new InputStreamReader(new
				 * FileInputStream(file), StandardCharsets.UTF_8);
				 * TransformerFactory tFactory = new
				 * net.sf.saxon.TransformerFactoryImpl(); Source xmlSource = new
				 * StreamSource(input); Source xslSource = new StreamSource(new
				 * File(xsldoc)); Transformer transformer =
				 * tFactory.newTransformer(xslSource);
				 * transformer.transform(xmlSource, new StreamResult(new
				 * FileWriter(fOut)));
				 */
				switch (type) {
				case AAA:
					try {
						this.mergeAAACodeListAndTransform(modelPath, file.toString(), modelPath, superClass, uriPrefix);
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				case XPLAN:
					try {
						this.mergeXPlanCodeListAndTransform(modelPath, file.toString(), modelPath, superClass,
								uriPrefix);
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				case XPLAN4:
					try {
						this.mergeXPlan4CodeListAndTransform(modelPath, file.toString(), modelPath, superClass,
								uriPrefix);
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				default:
					try {
						this.mergeXPlan4CodeListAndTransform(modelPath, codeListPath.toString(), modelPath, superClass,
								uriPrefix);
					} catch (XPathExpressionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}

			}
		} else {
			File fIn = codeListPath;
			InputStreamReader input = new InputStreamReader(new FileInputStream(fIn), StandardCharsets.UTF_8);
			TransformerFactory tFactory = new net.sf.saxon.TransformerFactoryImpl();
			Source xmlSource = new StreamSource(input);
			Source xslSource = new StreamSource(new File(xsldoc));
			Transformer transformer = tFactory.newTransformer(xslSource);
			transformer.transform(xmlSource, new StreamResult(fOut));
			switch (type) {
			case AAA:
				try {
					this.mergeAAACodeListAndTransform(modelPath, codeListPath.toString(), modelPath, superClass,
							uriPrefix);
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case XPLAN:
				try {
					this.mergeXPlanCodeListAndTransform(modelPath, codeListPath.toString(), modelPath, superClass,
							uriPrefix);
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			case XPLAN4:
				try {
					this.mergeXPlan4CodeListAndTransform(modelPath, codeListPath.toString(), modelPath, superClass,
							uriPrefix);
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			default:
				try {
					this.mergeXPlan4CodeListAndTransform(modelPath, codeListPath.toString(), modelPath, superClass,
							uriPrefix);
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		//
	}

	public void mergeCodeListAndTransform(String file1, String file2, String resultFile, String superClass,
			String uriPrefix) throws SAXException, IOException, ParserConfigurationException, TransformerException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();

		// build DOMs
		Document doc1 = builder.parse(new File(file1));
		Document doc2 = builder.parse(new File(file2));

		// get all node_ids from doc2 and iterate
		NodeList list = doc2.getElementsByTagName("owl:NamedIndividual");
		NodeList nodelist = doc1.getElementsByTagName("rdf:RDF");
		Node node = nodelist.item(0);
		for (int i = 0; i < list.getLength(); i++) {
			Node n = list.item(i);
			node.appendChild(doc1.importNode(n, true));
		}

		// write out the modified document to a new file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(doc1);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void mergeXPlan4CodeListAndTransform(String resultOWL, String aaaXML, String resultFile, String superClass,
			String uriPrefix) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
					TransformerException {
		System.out.println("ResultOWL: " + resultOWL + " - aaaXML: " + aaaXML + " - resultFile: " + resultFile);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		// build DOMs
		Document resultOWLdoc = builder.parse(new File(resultOWL));
		Document aaaxmldoc = builder.parse(new File(aaaXML));

		// get all node_ids from doc2 and iterate
		NodeList allDescriptionsFromXSD = aaaxmldoc.getElementsByTagName("description");
		NodeList owlClassNamesFromXSD = (NodeList) xPath.evaluate("//TypeDefinition/name/text()",
				aaaxmldoc.getDocumentElement(), XPathConstants.NODESET);
		NodeList owlClassDescsFromXSD = (NodeList) xPath.evaluate("//TypeDefinition/description/text()",
				aaaxmldoc.getDocumentElement(), XPathConstants.NODESET);
		NodeList owlClassFromXSD = (NodeList) xPath.evaluate("//TypeDefinition", aaaxmldoc.getDocumentElement(),
				XPathConstants.NODESET);
		NodeList listedvaluelist = (NodeList) xPath.evaluate("//PropertyDefinition", aaaxmldoc, XPathConstants.NODESET);
		System.out.println("Propdefs: " + listedvaluelist.getLength());
		// NodeList objectPropertyList=(NodeList)
		// xPath.evaluate("//*[local-name()='ObjectProperty' and
		// @ID='"+className+"']",
		// resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
		for (int n = 0; n < listedvaluelist.getLength(); n++) {
			System.out.println("PropertyName: " + n + "/" + listedvaluelist.getLength());
			NodeList dictEntryidentifier = (NodeList) xPath.evaluate(
					"dictionaryEntry/ListedValueDefinition/identifier/text()", listedvaluelist.item(n),
					XPathConstants.NODESET);
			NodeList dictentryText = (NodeList) xPath.evaluate("dictionaryEntry/ListedValueDefinition/name/text()",
					listedvaluelist.item(n), XPathConstants.NODESET);
			NodeList descriptionentryText = (NodeList) xPath.evaluate(
					"dictionaryEntry/ListedValueDefinition/description/text()", listedvaluelist.item(n),
					XPathConstants.NODESET);
			NodeList propDesc = (NodeList) xPath.evaluate("description/text()", listedvaluelist.item(n),
					XPathConstants.NODESET);
			NodeList propNm = (NodeList) xPath.evaluate("name/text()", listedvaluelist.item(n),
					XPathConstants.NODESET);
			NodeList valueTypeName = (NodeList) xPath.evaluate("valueTypeName/text()", listedvaluelist.item(n),
					XPathConstants.NODESET);
			NodeList addCommentProp = (NodeList) xPath.evaluate(
					"//*[local-name()='ObjectProperty' and @*[local-name()='ID']='" + propNm.item(0).getTextContent() + "']",
					resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
			System.out.println("//*[local-name()='ObjectProperty' and @*[local-name()='ID']='" + propNm.item(0).getTextContent() + "']");
			if(addCommentProp.getLength()>0 && propDesc.getLength()>0){
			Element comment=resultOWLdoc.createElement("rdfs:comment");
			comment.setTextContent(propDesc.item(0).getTextContent());
			addCommentProp.item(0).appendChild(comment);
			}
			if(valueTypeName.getLength()>0){
			addCommentProp = (NodeList) xPath.evaluate(
					"//*[local-name()='Class' and @*[local-name()='ID']='" + valueTypeName.item(0).getTextContent() + "']",
					resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
			System.out.println("//*[local-name()='Class' and @*[local-name()='ID']='" + valueTypeName.item(0).getTextContent() + "']");
			if(addCommentProp.getLength()>0 && propDesc.getLength()>0){
			Element comment=resultOWLdoc.createElement("rdfs:comment");
			comment.setTextContent(propDesc.item(0).getTextContent());
			addCommentProp.item(0).appendChild(comment);
			}
			}
			for (int j = 0; j < dictEntryidentifier.getLength(); j++) {
				// System.out.println("DictEntryIdentifier:
				// "+dictEntryidentifier.item(j).toString());
				String id = dictEntryidentifier.item(j).getTextContent()
						.substring(dictEntryidentifier.item(j).getTextContent().lastIndexOf(':') + 1);
				/*
				 * String
				 * temp=dictEntryidentifier.item(j).getTextContent().substring(0
				 * ,dictEntryidentifier.item(j).getTextContent().lastIndexOf(':'
				 * )); String name=temp.substring(temp.lastIndexOf(':')+1);
				 */
				// System.out.println("Individual XPath:
				// "+"//*[local-name()='NamedIndividual' and
				// @ID='"+propName+"_"+id+"']");
				// String[] propSplit=propName.split("_");
				NodeList enumIndividials;
				/*
				 * if(propSplit.length>1){
				 * System.out.println(propSplit[0]+"_"+WordUtils.capitalize(name
				 * )+"_"+propSplit[1]+"_"+id); System.out.println(
				 * "//*[local-name()='NamedIndividual' and @*[local-name()='ID']='"
				 * +valueTypeName.item(0).getTextContent()+"_"+id+"']");
				 * enumIndividials=(NodeList) xPath.evaluate(
				 * "//*[local-name()='NamedIndividual' and @*[local-name()='ID']='"
				 * +valueTypeName.item(0).getTextContent()+"_"+id+"']",
				 * resultOWLdoc, XPathConstants.NODESET); }else{
				 */
				String valType=(valueTypeName.item(0).getTextContent().contains(":")?
						valueTypeName.item(0).getTextContent().substring(valueTypeName.item(0).getTextContent().indexOf(':')+1)
						:valueTypeName.item(0).getTextContent());
				enumIndividials = (NodeList) xPath.evaluate(
						"//*[local-name()='NamedIndividual' and @*[local-name()='ID']='"
								+ valType + "_" + id + "']",
						resultOWLdoc, XPathConstants.NODESET);
				// }
				System.out.println("EnumInds: " + enumIndividials.getLength());
				if (enumIndividials.getLength() > 0) {
					for (int l = 0; l < enumIndividials.getLength(); l++) {
						// System.out.println("Listeditemlist(k):
						// "+listedvaluelist.item(k).getTextContent());
						System.out.println(
								"Adding comment to Individual: " + valType + "_" + id);
						// System.out.println("EnumIndividuals:
						// "+enumIndividials.item(l).toString()+" -
						// "+enumIndividials.getLength());
						Element comment = resultOWLdoc.createElement("rdfs:label");
						comment.setAttribute("xml:lang", "de");
						comment.setTextContent(dictentryText.item(j).getTextContent());
						enumIndividials.item(l).appendChild(comment);
						if (descriptionentryText.getLength() > j) {
							comment = resultOWLdoc.createElement("rdfs:comment");
							comment.setTextContent(descriptionentryText.item(j).getTextContent());
							enumIndividials.item(l).appendChild(comment);
						}
					}
				} else {
					Element classs = resultOWLdoc.createElement("owl:Class");
					// classs.setAttribute("rdf:ID", );
				}

			}
		}

		if (owlClassFromXSD.getLength() > 0) {

			for (int m = 0; m < owlClassFromXSD.getLength(); m++) {
				System.out.println("PropertyName: " + m + "/" + owlClassNamesFromXSD.getLength());
				NodeList XSDpropertieslist = (NodeList) xPath.evaluate("/*[local-name()=PropertyDefinition]/name/text()",
						owlClassFromXSD.item(m), XPathConstants.NODESET);
				NodeList cardinalitylist = (NodeList) xPath.evaluate("/*[local-name()=PropertyDefinition]/cardinality/text()",
						owlClassFromXSD.item(m), XPathConstants.NODESET);
				NodeList propdescs = (NodeList) xPath.evaluate("/*[local-name()=PropertyDefinition]/description/text()",
						owlClassFromXSD.item(m), XPathConstants.NODESET);
				listedvaluelist = (NodeList) xPath.evaluate("//PropertyDefinition", owlClassNamesFromXSD.item(m),
						XPathConstants.NODESET);
				NodeList propNames = (NodeList) xPath.evaluate(
						"name/text()",
						owlClassFromXSD.item(m), XPathConstants.NODESET);
				String propName = propNames.item(0).getTextContent();
				if(propName==null)
					continue;
				NodeList owlClassesAlreadyExistingWithNameFromXSD = (NodeList) xPath.evaluate(
						"//*[local-name()='Class' and @*[local-name()='ID']='" + propName + "']",
						resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
				NodeList propDescs = (NodeList) xPath.evaluate(
						"description/text()",
						owlClassFromXSD.item(m), XPathConstants.NODESET);
				String propDesc=null;
				if(propDescs.getLength()>0)
					propDesc=propDescs.item(0).getTextContent();
				System.out.println("PropName: "+propName+" - "+propDesc);
				// NodeList namedIndividuals=(NodeList)
				// xPath.evaluate("//*[local-name()='NamedIndividual']",
				// resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
				if (owlClassesAlreadyExistingWithNameFromXSD.getLength() > 0) {
					String className = owlClassesAlreadyExistingWithNameFromXSD.item(0).getTextContent();
					if(propDesc!=null){
					 Element comment=resultOWLdoc.createElement("rdfs:comment");
					 comment.setAttribute("xml:lang", "de");
					 comment.setTextContent(propDesc);
					 owlClassesAlreadyExistingWithNameFromXSD.item(0).appendChild(comment);
					}

					/*
					 * for(int i = 0 ; i< allDescriptionsFromXSD.getLength() ;
					 * i++){ //System.out.println("Descriptions: "
					 * +descriptionlist.item(i).getTextContent());
					 * 
					 * //System.out.println("InsertNode: "
					 * +classesList.item(0).getTextContent()); for(int
					 * j=0;j<owlClassesAlreadyExistingWithNameFromXSD.getLength(
					 * );j++){ Element
					 * comment=resultOWLdoc.createElement("rdfs:comment");
					 * comment.setAttribute("xml:lang", "de");
					 * comment.setTextContent(allDescriptionsFromXSD.item(i).
					 * getTextContent());
					 * owlClassesAlreadyExistingWithNameFromXSD.item(j).
					 * appendChild(comment); } }
					 */

					//for (int k = 0; k < owlClassesAlreadyExistingWithNameFromXSD.getLength(); k++) {
						for (int i = 0; i < XSDpropertieslist.getLength(); i++) {
							// System.out.println("Properties:
							// "+XSDpropertieslist.item(i).getTextContent());
							Element subclassof = resultOWLdoc.createElement("rdfs:subClassOf");
							Element restriction = resultOWLdoc.createElement("owl:Restriction");
							Element onProperty = resultOWLdoc.createElement("owl:onProperty");
							onProperty.setAttribute("rdf:resource", "#" + XSDpropertieslist.item(i).getTextContent());
							restriction.appendChild(onProperty);
							subclassof.appendChild(restriction);
							String cardinality = cardinalitylist.item(i).getTextContent();
							if (cardinality.contains("..")) {
								Element minCard = resultOWLdoc.createElement("owl:minCardinality");
								minCard.setAttribute("rdf:datatype",
										"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
								minCard.setTextContent(cardinality.substring(0, cardinality.indexOf('.')));
								restriction.appendChild(minCard);
								if (!cardinality.substring(cardinality.lastIndexOf('.') + 1).equals("*")) {
									Element maxCard = resultOWLdoc.createElement("owl:maxCardinality");
									maxCard.setAttribute("rdf:datatype",
											"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
									maxCard.setTextContent(cardinality.substring(cardinality.lastIndexOf('.') + 1));
									restriction.appendChild(maxCard);
								}
							} else {
								Element minCard = resultOWLdoc.createElement("owl:cardinality");
								minCard.setAttribute("rdf:datatype",
										"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
								minCard.setTextContent(cardinality);
								restriction.appendChild(minCard);
							}
							owlClassesAlreadyExistingWithNameFromXSD.item(0).appendChild(subclassof);

						//}
					}
				}
			}
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(resultOWLdoc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);

		/*
		 * System.out.println("ResultOWL: "+resultOWL+" - aaaXML: "+aaaXML+
		 * " - resultFile: "+resultFile); DocumentBuilder builder =
		 * DocumentBuilderFactory.newInstance().newDocumentBuilder(); XPath
		 * xPath = XPathFactory.newInstance().newXPath(); //build DOMs Document
		 * resultOWLdoc = builder.parse(new File(resultOWL)); Document aaaxmldoc
		 * = builder.parse(new File(aaaXML)); NodeList dictMemberIds =
		 * (NodeList) xPath.evaluate("//*[local-name()='identifier']",
		 * aaaxmldoc, XPathConstants.NODESET); NodeList dictMemberDescs =
		 * (NodeList) xPath.evaluate("//*[local-name()='description']/text()",
		 * aaaxmldoc, XPathConstants.NODESET); NodeList dictMemberNames =
		 * (NodeList) xPath.evaluate("//*[local-name()='name']/text()",
		 * aaaxmldoc, XPathConstants.NODESET); NodeList individuals = (NodeList)
		 * xPath.evaluate("//*[local-name()='NamedIndividual']", resultOWLdoc,
		 * XPathConstants.NODESET); Integer descriptioncounter=0; for(int
		 * j=0;j<dictMemberIds.getLength();j++){ System.out.println(
		 * "DictMember: "+dictMemberIds.item(j).getTextContent()+" matches "
		 * +dictMemberIds.item(j).getTextContent().matches(".*[0-9]+$"));
		 * if(dictMemberIds.item(j).getTextContent().matches(".*[0-9]+$")){
		 * String
		 * number=dictMemberIds.item(j).getTextContent().substring(dictMemberIds
		 * .item(j).getTextContent().lastIndexOf(':')+1); String
		 * wonumber=dictMemberIds.item(j).getTextContent().substring(0,
		 * dictMemberIds.item(j).getTextContent().lastIndexOf(':')); String
		 * wonumber2=wonumber.substring(0,wonumber.lastIndexOf(':')); NodeList
		 * enumIndividials=(NodeList) xPath.evaluate(
		 * "//*[local-name()='NamedIndividual' and @rdf:ID='"+
		 * wonumber2.substring(wonumber2.lastIndexOf(':')+1)+"_"+number+"']",
		 * resultOWLdoc, XPathConstants.NODESET);
		 * 
		 * System.out.println("//*[local-name()='NamedIndividual' and @rdf:ID='"
		 * + wonumber2.substring(wonumber2.lastIndexOf(':')+1)+"_"+number+"' "
		 * +enumIndividials.getLength()); if(enumIndividials.getLength()>0){
		 * System.out.println("//*[local-name()='NamedIndividual' and @ID='"
		 * +dictMemberIds.item(j).getTextContent()+"']");
		 * System.out.println(dictMemberDescs.item(j).getTextContent()); Element
		 * comment=resultOWLdoc.createElement("rdfs:label");
		 * comment.setAttribute("xml:lang", "de");
		 * if(StringUtils.isNumeric(dictMemberNames.item(j).getTextContent()) &&
		 * dictMemberDescs.getLength()>descriptioncounter+1){
		 * while(dictMemberDescs.item(descriptioncounter).getTextContent().
		 * contains("Stand:") ||
		 * dictMemberDescs.item(descriptioncounter).getTextContent().contains(
		 * "__XPlanGML") ||
		 * dictMemberDescs.item(descriptioncounter).getTextContent().contains(
		 * "'XPlanGML'")) descriptioncounter++;
		 * comment.setTextContent(dictMemberDescs.item(descriptioncounter++).
		 * getTextContent());
		 * //System.out.println(dictMemberIds.item(j).getTextContent()+": "
		 * +dictMemberDescs.item(descriptioncounter++).getTextContent()); }else{
		 * comment.setTextContent(dictMemberNames.item(j).getTextContent());
		 * //System.out.println(dictMemberIds.item(j).getTextContent()+": "
		 * +dictMemberNames.item(j).getTextContent()); }
		 * enumIndividials.item(0).appendChild(comment); }else{ Element
		 * comment=resultOWLdoc.createElement("rdfs:label");
		 * comment.setAttribute("xml:lang", "de"); } } //} }
		 * resultOWLdoc=this.cleanUpUsingDOMTree(resultOWLdoc,aaaxmldoc,
		 * superClass,uriPrefix); //write out the modified document to a new
		 * file TransformerFactory tFactory = TransformerFactory.newInstance();
		 * Transformer transformer = tFactory.newTransformer(); Source source =
		 * new DOMSource(resultOWLdoc); Result output = new StreamResult(new
		 * File(resultFile)); transformer.transform(source, output);
		 */
	}

	public void mergeXPlanCodeListAndTransform(String resultOWL, String aaaXML, String resultFile, String superClass,
			String uriPrefix) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
					TransformerException {
		System.out.println("ResultOWL: " + resultOWL + " - aaaXML: " + aaaXML + " - resultFile: " + resultFile);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		// build DOMs
		Document resultOWLdoc = builder.parse(new File(resultOWL));
		Document aaaxmldoc = builder.parse(new File(aaaXML));
		/*
		 * NodeList dictEntrylist = (NodeList)
		 * xPath.evaluate("//*[local-name()='dictionaryEntry']",
		 * aaaxmldoc.getDocumentElement(), XPathConstants.NODESET);
		 * System.out.println(dictEntrylist.getLength());
		 */
		/*
		 * for(int i=0;i<dictEntrylist.getLength();i++){
		 * System.out.println(dictEntrylist.item(i)); NodeList dictMembers =
		 * (NodeList) xPath.evaluate("//*[local-name()='definitionMember']",
		 * dictEntrylist.item(i), XPathConstants.NODESET);
		 */
		NodeList dictMemberIds = (NodeList) xPath.evaluate("//@*[local-name()='id']", aaaxmldoc,
				XPathConstants.NODESET);
		NodeList dictMemberDescs = (NodeList) xPath.evaluate("//*[local-name()='description']/text()", aaaxmldoc,
				XPathConstants.NODESET);
		NodeList dictMemberNames = (NodeList) xPath.evaluate("//*[local-name()='name']/text()", aaaxmldoc,
				XPathConstants.NODESET);
		Integer descriptioncounter = 0;
		for (int j = 0; j < dictMemberIds.getLength(); j++) {

			if (dictMemberIds.item(j).getTextContent().matches(".*_[0-9]+")) {
				NodeList enumIndividials = (NodeList) xPath.evaluate(
						"//*[local-name()='NamedIndividual' and @ID='" + dictMemberIds.item(j).getTextContent() + "']",
						resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
				if (enumIndividials.getLength() > 0) {
					// System.out.println("//*[local-name()='NamedIndividual'
					// and @ID='"+dictMemberIds.item(j).getTextContent()+"']");
					// System.out.println(dictMemberDescs.item(j).getTextContent());
					Element comment = resultOWLdoc.createElement("rdfs:label");
					comment.setAttribute("xml:lang", "de");
					if (StringUtils.isNumeric(dictMemberNames.item(j).getTextContent())
							&& dictMemberDescs.getLength() > descriptioncounter + 1) {
						while (dictMemberDescs.item(descriptioncounter).getTextContent().contains("Stand:")
								|| dictMemberDescs.item(descriptioncounter).getTextContent().contains("__XPlanGML")
								|| dictMemberDescs.item(descriptioncounter).getTextContent().contains("'XPlanGML'"))
							descriptioncounter++;
						comment.setTextContent(dictMemberDescs.item(descriptioncounter++).getTextContent());
						// System.out.println(dictMemberIds.item(j).getTextContent()+":
						// "+dictMemberDescs.item(descriptioncounter++).getTextContent());
					} else {
						comment.setTextContent(dictMemberNames.item(j).getTextContent());
						// System.out.println(dictMemberIds.item(j).getTextContent()+":
						// "+dictMemberNames.item(j).getTextContent());
					}
					enumIndividials.item(0).appendChild(comment);
				}
			}
			// }
		}
		resultOWLdoc = this.cleanUpUsingDOMTree(resultOWLdoc, aaaxmldoc, superClass, uriPrefix);
		// write out the modified document to a new file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(resultOWLdoc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void justCleanUp(String resultOWL, String xsddoc, String resultFile, String superClass, String uriPrefix)
			throws SAXException, IOException, ParserConfigurationException, XPathExpressionException,
			TransformerException {
		System.out.println("ResultOWL: " + resultOWL + " - resultFile: " + resultFile);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		// build DOMs
		Document resultOWLdoc = builder.parse(new File(resultOWL));
		Document xsddocc = builder.parse(new File(xsddoc));
		resultOWLdoc = this.cleanUpUsingDOMTree(resultOWLdoc, xsddocc, superClass, uriPrefix);
		// write out the modified document to a new file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(resultOWLdoc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void enrichDocWithGMLTypes(String resultOWLs, String xsdfiles, String uriPrefix)
			throws XPathExpressionException, DOMException, SAXException, IOException, ParserConfigurationException,
			TransformerException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		// build DOMs
		Document resultOWL = builder.parse(new File(resultOWLs));
		Document xsdfile = builder.parse(new File(xsdfiles));
		NodeList allValuesFrom = (NodeList) xPath.evaluate("//*[local-name()='allValuesFrom']", resultOWL,
				XPathConstants.NODESET);
		for (int i = 0; i < allValuesFrom.getLength(); i++) {
			Node current = allValuesFrom.item(i);
			if (current.getAttributes().getNamedItem("rdf:resource").getTextContent().contains("replace_")) {
				// System.out.println(current.getAttributes().getNamedItem("rdf:resource").getTextContent());
				NodeList ref = (NodeList) xPath.evaluate(
						"//*[local-name()='element' and @name='" + current.getAttributes().getNamedItem("rdf:resource")
								.getTextContent().replace("replace_", "") + "']/@type",
						xsdfile, XPathConstants.NODESET);
				// System.out.println("//xsd:element[@name='"+current.getAttributes().getNamedItem("rdf:resource").getTextContent().replace("replace_",
				// "")+"']/@type");
				if (ref.getLength() > 0)
					current.getAttributes().getNamedItem("rdf:resource").setTextContent(uriPrefix
							+ ref.item(0).getTextContent().substring(ref.item(0).getTextContent().indexOf(':') + 1));
				else
					current.getParentNode().removeChild(current);
			}
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(resultOWL);
		Result output = new StreamResult(new File(resultOWLs));
		transformer.transform(source, output);
	}

	public void domMerger(File files, String regex, String origindoc, String resultFile)
			throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
			TransformerException {
		System.out.println("ResultOWL: " + origindoc + " - resultFile: " + resultFile);
		File resfile = new File(resultFile);
		if (resfile.exists())
			resfile.delete();
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document origindocc = builder.parse(new File(origindoc));
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList rdfNodeorigin = (NodeList) xPath.evaluate("//*[local-name()='RDF']", origindocc,
				XPathConstants.NODESET);
		Node rootnode = rdfNodeorigin.item(0);
		for (File file : files.listFiles()) {
			System.out
					.println("Matches: " + file.getAbsolutePath().matches(regex) + " " + file.getName() + " " + regex);
			if (file.getName().matches(regex) && !file.getAbsolutePath().contains(resultFile)) {
				Document currentdoc = builder.parse(file);
				NodeList rdfNode = (NodeList) xPath.evaluate("//*[local-name()='RDF']/*", currentdoc,
						XPathConstants.NODESET);
				for (int i = 0; i < rdfNode.getLength(); i++) {
					Node newNode = origindocc.importNode(rdfNode.item(i), true);
					rootnode.appendChild(newNode);
				}
			}
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(origindocc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void integrateGMXCodelist(File owlfile, String codelistFile, String resultFile, String uriPrefix)
			throws ParserConfigurationException, SAXException, IOException, TransformerException,
			XPathExpressionException {
		System.out.println("ResultOWL: " + owlfile + " - resultFile: " + resultFile);
		XPath xPath = XPathFactory.newInstance().newXPath();
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document origindocc = builder.parse(owlfile);
		Document codelist = builder.parse(new File(codelistFile));
		NodeList objectProps = (NodeList) xPath.evaluate("//*[local-name()='CodeListDictionary']", codelist,
				XPathConstants.NODESET);
		Node rdfNode = origindocc.getElementsByTagName("rdf:RDF").item(0);
		for (int i = 0; i < objectProps.getLength(); i++) {
			Element outerclass = origindocc.createElement("owl:Class");
			outerclass.setAttribute("rdf:about",
					uriPrefix.substring(0, uriPrefix.length() - 3)
							+ objectProps.item(i).getAttributes().getNamedItem("gml:id").getTextContent()
									.substring(0, 2).toLowerCase()
							+ "#" + objectProps.item(i).getAttributes().getNamedItem("gml:id").getTextContent());
			Element subClassOf = origindocc.createElement("rdfs:subClassOf");
			subClassOf.setAttribute("rdf:resource", uriPrefix + "Enumeration");
			outerclass.appendChild(subClassOf);
			rdfNode.appendChild(outerclass);

			Element oneof = origindocc.createElement("owl:oneOf");
			oneof.setAttribute("rdf:parseType", "Collection");
			outerclass.appendChild(oneof);
			NodeList individualids = (NodeList) xPath.evaluate(
					"codeEntry/CodeDefinition/*[local-name()='identifier']/text()", objectProps.item(i),
					XPathConstants.NODESET);
			NodeList individualcomments = (NodeList) xPath.evaluate(
					"codeEntry/CodeDefinition/*[local-name()='description']/text()", objectProps.item(i),
					XPathConstants.NODESET);
			for (int j = 0; j < individualids.getLength(); j++) {
				Element indi = origindocc.createElement("owl:NamedIndividual");
				indi.setAttribute("rdf:ID", objectProps.item(i).getAttributes().getNamedItem("gml:id").getTextContent()
						+ "_" + individualids.item(j).getTextContent());
				Element comment = origindocc.createElement("rdfs:comment");
				comment.setTextContent(individualcomments.item(j).getTextContent());
				Element label = origindocc.createElement("rdfs:label");
				label.setTextContent(individualids.item(j).getTextContent());
				indi.appendChild(label);
				Element rdftype = origindocc.createElement("rdf:type");
				rdftype.setAttribute("rdf:resource",
						uriPrefix.substring(0, uriPrefix.length() - 3)
								+ objectProps.item(i).getAttributes().getNamedItem("gml:id").getTextContent()
										.substring(0, 2).toLowerCase()
								+ "#" + objectProps.item(i).getAttributes().getNamedItem("gml:id").getTextContent());
				indi.appendChild(label);
				indi.appendChild(comment);
				indi.appendChild(rdftype);
				oneof.appendChild(indi);
			}
		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(origindocc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void integrateINSPIRECodeListEnums(File owlfile, String resultFile, String uriPrefix)
			throws XPathExpressionException, SAXException, IOException, ParserConfigurationException,
			TransformerException {
		System.out.println("ResultOWL: " + owlfile + " - resultFile: " + resultFile);
		XPath xPath = XPathFactory.newInstance().newXPath();
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document origindocc = builder.parse(owlfile);
		NodeList objectProps = (NodeList) xPath.evaluate("//*[local-name()='ObjectProperty']", origindocc,
				XPathConstants.NODESET);
		for (int i = 0; i < objectProps.getLength(); i++) {
			Node current = objectProps.item(i);
			String currentname = current.getAttributes().getNamedItem("rdf:about").getTextContent()
					.substring(current.getAttributes().getNamedItem("rdf:about").getTextContent().lastIndexOf('#') + 1);
			System.out.println("Current Property: " + currentname);
			NodeList currentchildren = current.getChildNodes();
			for (int j = 0; j < currentchildren.getLength(); j++) {
				Node currentrange = currentchildren.item(j);
				if (currentrange.getNodeName()
						.contains(
								"range")
						&& currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent()
								.contains(
										"Reference")
						&& ((boolean) xPath.evaluate("//@*[local-name()='about']='" + uriPrefix
								+ StringUtils.capitalize(currentname) + "Value'", origindocc,
								XPathConstants.BOOLEAN))) {
					System.out.println("CurrentRange: "
							+ currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent());
					if (currentname.endsWith("Value")) {
						((Element) currentrange).setAttribute("rdf:resource",
								uriPrefix + StringUtils.capitalize(currentname));
					} else {
						((Element) currentrange).setAttribute("rdf:resource",
								uriPrefix + StringUtils.capitalize(currentname) + "Value");
					}
				} else
					if (currentrange.getNodeName()
							.contains(
									"range")
							&& currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent()
									.contains("Reference")
							&& ((boolean) xPath
									.evaluate(
											"//@*[local-name()='about']='" + uriPrefix
													+ StringUtils.capitalize(currentname) + "TypeValue'",
											origindocc, XPathConstants.BOOLEAN))) {
					System.out.println("CurrentRange: "
							+ currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent());
					if (currentname.endsWith("Value")) {
						((Element) currentrange).setAttribute("rdf:resource",
								uriPrefix + StringUtils.capitalize(currentname));
					} else {
						((Element) currentrange).setAttribute("rdf:resource",
								uriPrefix + StringUtils.capitalize(currentname) + "Value");
					}
				}
			}
		}
		NodeList allValuesFrom = (NodeList) xPath.evaluate("//*[local-name()='Restriction']", origindocc,
				XPathConstants.NODESET);
		for (int j = 0; j < allValuesFrom.getLength(); j++) {
			NodeList currentrangechilds = allValuesFrom.item(j).getChildNodes();
			String currentProp = null;
			for (int i = 0; i < currentrangechilds.getLength(); i++) {
				Node currentrange = currentrangechilds.item(i);
				if (currentrange != null && currentrange.getNodeName() != null
						&& currentrange.getNodeName().contains("onProperty")) {
					currentProp = currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent().substring(
							currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent().lastIndexOf('#')
									+ 1);
				}
				if (currentProp != null && currentrange.getNodeName().contains("allValuesFrom") && currentrange
						.getAttributes().getNamedItem("rdf:resource").getTextContent().contains("Reference")) {
					System.out.println("CurrentRange: "
							+ currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent());
					System.out.println("//@*[local-name()='about']='" + uriPrefix + StringUtils.capitalize(currentProp)
							+ "Value'");
					if ((boolean) xPath.evaluate(
							"//@*[local-name()='about']='" + uriPrefix + StringUtils.capitalize(currentProp) + "Value'",
							origindocc, XPathConstants.BOOLEAN)) {
						currentProp = currentProp.replace("Type", "");
						if (currentProp.endsWith("Value")) {
							((Element) currentrange).setAttribute("rdf:resource",
									uriPrefix + StringUtils.capitalize(currentProp));
						} else {
							((Element) currentrange).setAttribute("rdf:resource",
									uriPrefix + StringUtils.capitalize(currentProp) + "Value");
						}
					} else if (currentrange.getNodeName()
							.contains(
									"range")
							&& currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent()
									.contains("Reference")
							&& ((boolean) xPath
									.evaluate(
											"//@*[local-name()='about']='" + uriPrefix
													+ StringUtils.capitalize(currentProp) + "TypeValue'",
											origindocc, XPathConstants.BOOLEAN))) {
						System.out.println("CurrentRange: "
								+ currentrange.getAttributes().getNamedItem("rdf:resource").getTextContent());
						if (currentProp.endsWith("Value")) {
							((Element) currentrange).setAttribute("rdf:resource",
									uriPrefix + StringUtils.capitalize(currentProp));
						} else {
							((Element) currentrange).setAttribute("rdf:resource",
									uriPrefix + StringUtils.capitalize(currentProp) + "Value");
						}
					}
				}
			}

		}
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(origindocc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);
	}

	public void transformINSPIRECodeList(String resultOWL, String xsddoc, String resultFile, String superClass,
			String uriPrefix) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException,
					TransformerException {
		System.out.println("ResultOWL: " + resultOWL + " - resultFile: " + resultFile);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		// build DOMs
		Document resultOWLdoc = builder.parse(new File(resultOWL));

		NodeList rdfNode = (NodeList) xPath.evaluate("//*[local-name()='RDF']", resultOWLdoc, XPathConstants.NODESET);
		Element inspireclass = resultOWLdoc.createElement("owl:Class");
		inspireclass.setAttribute("rdf:about",
				uriPrefix + resultOWL.substring(resultOWL.lastIndexOf('/') + 1, resultOWL.lastIndexOf('_')));
		Element outerclass = resultOWLdoc.createElement("owl:Class");
		outerclass.setAttribute("rdf:about",
				uriPrefix + resultOWL.substring(resultOWL.indexOf('_') + 1, resultOWL.indexOf('.')));
		Element label = resultOWLdoc.createElement("rdfs:label");
		label.setAttribute("xml:lang", "en");
		label.setTextContent(
				((NodeList) xPath.evaluate("//*[local-name()='prefLabel']", resultOWLdoc, XPathConstants.NODESET))
						.item(0).getTextContent());
		outerclass.appendChild(label);
		Element comment = resultOWLdoc.createElement("rdfs:comment");
		comment.setTextContent(
				((NodeList) xPath.evaluate("//*[local-name()='definition']", resultOWLdoc, XPathConstants.NODESET))
						.item(0).getTextContent());
		outerclass.appendChild(comment);
		Element subClassOf = resultOWLdoc.createElement("rdfs:subClassOf");
		subClassOf.setAttribute("rdf:resource",
				uriPrefix + resultOWL.substring(resultOWL.lastIndexOf('/') + 1, resultOWL.lastIndexOf('_')));
		outerclass.appendChild(subClassOf);
		subClassOf = resultOWLdoc.createElement("rdfs:subClassOf");
		subClassOf.setAttribute("rdf:resource", uriPrefix + "Enumeration");
		outerclass.appendChild(subClassOf);
		rdfNode.item(0).appendChild(outerclass);
		Element equiv = resultOWLdoc.createElement("owl:equivalentClass");
		outerclass.appendChild(equiv);
		Element innerclass = resultOWLdoc.createElement("owl:Class");
		equiv.appendChild(innerclass);
		Element oneof = resultOWLdoc.createElement("owl:oneOf");
		oneof.setAttribute("rdf:parseType", "Collection");
		innerclass.appendChild(oneof);
		NodeList dictMemberNames = (NodeList) xPath.evaluate("//*[local-name()='Description']", resultOWLdoc,
				XPathConstants.NODESET);
		Set<String> added = new TreeSet<String>();
		for (int i = 0; i < dictMemberNames.getLength(); i++) {
			// System.out.println("Length: "+dictMemberNames.getLength()+"i:
			// "+i);
			Node current = dictMemberNames.item(i);
			String currentatt = dictMemberNames.item(i).getAttributes().getNamedItem("rdf:about").getTextContent();
			System.out.println("Currentatt: " + currentatt + " - "
					+ resultOWL.substring(resultOWL.indexOf('_') + 1, resultOWL.indexOf('.')));
			if (!currentatt.matches(
					"http://.*/" + resultOWL.substring(resultOWL.indexOf('_') + 1, resultOWL.indexOf('.')) + "/.*/$")
					&& !currentatt.endsWith(resultOWL.substring(resultOWL.indexOf('_') + 1, resultOWL.indexOf('.')))
					&& !currentatt.substring(currentatt.lastIndexOf('/')).contains(".")
					&& !added.contains(currentatt)) {
				Element desc = resultOWLdoc.createElement("rdf:Description");
				desc.setAttribute("rdf:about", currentatt);
				oneof.appendChild(desc);
				Element rdftype = resultOWLdoc.createElement("rdf:type");
				rdftype.setAttribute("rdf:resource",
						uriPrefix + resultOWL.substring(resultOWL.indexOf('_') + 1, resultOWL.indexOf('.')));
				desc.appendChild(rdftype);
				oneof.appendChild(desc);
				added.add(currentatt);
			}

		}
		// resultOWLdoc=this.cleanUpUsingDOMTree(resultOWLdoc,xsddocc,superClass,uriPrefix);
		// write out the modified document to a new file
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		Source source = new DOMSource(resultOWLdoc);
		Result output = new StreamResult(new File(resultFile));
		transformer.transform(source, output);

	}

	public Document cleanUpUsingDOMTree(Document cleanupdoc, Document xsddoc, String superClass, String prefix)
			throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();
		NodeList dictMemberNames = (NodeList) xPath.evaluate("//*[local-name()='subClassOf']", cleanupdoc,
				XPathConstants.NODESET);
		for (int i = 0; i < dictMemberNames.getLength(); i++) {
			// System.out.println("Length: "+dictMemberNames.getLength()+"i:
			// "+i);
			Node current = dictMemberNames.item(i);
			if (!current.hasAttributes() && !current.hasChildNodes()) {
				current.getParentNode().removeChild(current);
			}
		}
		dictMemberNames = (NodeList) xPath.evaluate("//*[local-name()='Class']", cleanupdoc, XPathConstants.NODESET);
		for (int i = 0; i < dictMemberNames.getLength(); i++) {
			// System.out.println("Length: "+dictMemberNames.getLength()+"i:
			// "+i);
			Node current = dictMemberNames.item(i);
			if (!current.hasAttributes() && !current.hasChildNodes()) {
				current.getParentNode().removeChild(current);
			}
		}
		// Fix allValuesFrom with ref statements
		dictMemberNames = (NodeList) xPath.evaluate("//*[local-name()='allValuesFrom']", cleanupdoc,
				XPathConstants.NODESET);
		for (int i = 0; i < dictMemberNames.getLength(); i++) {
			// System.out.println("Length: "+dictMemberNames.getLength()+"i:
			// "+i);
			Node currentNode = dictMemberNames.item(i);
			if (currentNode.getAttributes().getLength() > 0) {
				String currentString = currentNode.getAttributes().getNamedItem("rdf:resource").getTextContent();
				if (currentString.contains("replaceref_")) {
					System.out.println("//xsd:element[@name='" + currentString.substring(currentString.indexOf('_') + 1)
							+ "']/@type");
					if (((NodeList) xPath.evaluate(
							"//*[local-name()='element' and @name='"
									+ currentString.substring(currentString.indexOf('_') + 1) + "']/@type",
							xsddoc, XPathConstants.NODESET)).getLength() > 0) {
						String toset = ((NodeList) xPath.evaluate(
								"//*[local-name()='element' and @name='"
										+ currentString.substring(currentString.indexOf('_') + 1) + "']/@type",
								xsddoc, XPathConstants.NODESET)).item(0).getTextContent();
						toset = toset.substring(toset.indexOf(':') + 1);
						toset = toset.replace("Type", "").replace("Property", "");
						if (toset.endsWith("_")) {
							toset = toset.substring(0, toset.length() - 1);
						}
						/*
						 * if(toset.endsWith("Type")){
						 * 
						 * }
						 */
						currentNode.getAttributes().getNamedItem("rdf:resource").setTextContent(prefix + toset);
					}
				}
			}
		}
		NodeList classes = (NodeList) xPath.evaluate("//*[local-name()='Class']", cleanupdoc, XPathConstants.NODESET);
		for (int i = 0; i < classes.getLength(); i++) {
			NodeList subClasses = (NodeList) xPath.evaluate(
					"child::*[local-name()='subClassOf'][contains(@resource,'" + prefix + "')]", classes.item(i),
					XPathConstants.NODESET);
			NodeList subClasses2 = (NodeList) xPath.evaluate("child::*[local-name()='subClassOf']", classes.item(i),
					XPathConstants.NODESET);
			// System.out.println("How many resoures? "+subClasses.getLength());
			if (subClasses.getLength() > 1) {
				for (int j = 0; j < subClasses2.getLength(); j++) {
					Node current = subClasses2.item(j);
					if (current.hasChildNodes())
						continue;
					// System.out.println("Current:
					// "+current.getAttributes().getNamedItem("rdf:resource").getTextContent());
					if (current.getAttributes().getNamedItem("rdf:resource").getTextContent().contains(superClass)) {
						// System.out.println("Removing:
						// "+current.getAttributes().getNamedItem("rdf:resource").getTextContent());
						current.getParentNode().removeChild(current);
					}
				}
			}
		}
		return cleanupdoc;
	}

	public void mergeAAACodeListAndTransform(String resultOWL, String aaaXML, String resultFile, String superClass,
			String uriPrefix) throws SAXException, IOException, ParserConfigurationException, TransformerException,
					XPathExpressionException {
		System.out.println("ResultOWL: " + resultOWL + " - aaaXML: " + aaaXML + " - resultFile: " + resultFile);
		DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		XPath xPath = XPathFactory.newInstance().newXPath();
		// build DOMs
		Document resultOWLdoc = builder.parse(new File(resultOWL));
		Document aaaxmldoc = builder.parse(new File(aaaXML));

		// get all node_ids from doc2 and iterate
		NodeList descriptionlist = aaaxmldoc.getElementsByTagName("description");
		NodeList propertyName = (NodeList) xPath.evaluate("/TypeDefinition/name/text()", aaaxmldoc.getDocumentElement(),
				XPathConstants.NODESET);
		if (propertyName.getLength() > 0) {
			System.out.println("PropertyName: " + propertyName.item(0).getTextContent());
			String propName = propertyName.item(0).getTextContent();
			NodeList classesList = (NodeList) xPath.evaluate("//*[local-name()='Class' and @ID='" + propName + "']",
					resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
			if (classesList.getLength() > 0) {
				String className = classesList.item(0).getTextContent();
				for (int i = 0; i < descriptionlist.getLength(); i++) {
					System.out.println("Descriptions: " + descriptionlist.item(i).getTextContent());

					System.out.println("InsertNode: " + classesList.item(0).getTextContent());
					for (int j = 0; j < classesList.getLength(); j++) {
						Element comment = resultOWLdoc.createElement("rdfs:comment");
						comment.setAttribute("xml:lang", "de");
						comment.setTextContent(descriptionlist.item(i).getTextContent());
						classesList.item(j).appendChild(comment);
					}
				}
				NodeList propertieslist = (NodeList) xPath.evaluate(
						"//*[local-name()='PropertyDefinition']/name/text()", aaaxmldoc.getDocumentElement(),
						XPathConstants.NODESET);
				NodeList cardinalitylist = (NodeList) xPath.evaluate(
						"//*[local-name()='PropertyDefinition']/cardinality/text()", aaaxmldoc.getDocumentElement(),
						XPathConstants.NODESET);
				NodeList listedvaluelist = (NodeList) xPath.evaluate("//*[local-name()='PropertyDefinition']",
						aaaxmldoc.getDocumentElement(), XPathConstants.NODESET);
				NodeList objectPropertyList = (NodeList) xPath.evaluate(
						"//*[local-name()='ObjectProperty' and @ID='" + className + "']",
						resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
				for (int k = 0; k < classesList.getLength(); k++) {
					for (int i = 0; i < propertieslist.getLength(); i++) {
						System.out.println("Properties: " + propertieslist.item(i).getTextContent());
						Element subclassof = resultOWLdoc.createElement("rdfs:subClassOf");
						Element restriction = resultOWLdoc.createElement("owl:Restriction");
						Element onProperty = resultOWLdoc.createElement("owl:onProperty");
						onProperty.setAttribute("rdf:resource", "#" + propertieslist.item(i).getTextContent());
						restriction.appendChild(onProperty);
						subclassof.appendChild(restriction);
						String cardinality = cardinalitylist.item(i).getTextContent();
						if (cardinality.contains("..")) {
							Element minCard = resultOWLdoc.createElement("owl:minCardinality");
							minCard.setAttribute("rdf:datatype", "http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
							minCard.setTextContent(cardinality.substring(0, cardinality.indexOf('.')));
							restriction.appendChild(minCard);
							if (!cardinality.substring(cardinality.lastIndexOf('.') + 1).equals("*")) {
								Element maxCard = resultOWLdoc.createElement("owl:maxCardinality");
								maxCard.setAttribute("rdf:datatype",
										"http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
								maxCard.setTextContent(cardinality.substring(cardinality.lastIndexOf('.') + 1));
								restriction.appendChild(maxCard);
							}
						} else {
							Element minCard = resultOWLdoc.createElement("owl:cardinality");
							minCard.setAttribute("rdf:datatype", "http://www.w3.org/2001/XMLSchema#nonNegativeInteger");
							minCard.setTextContent(cardinality);
							restriction.appendChild(minCard);
						}
						classesList.item(k).appendChild(subclassof);

					}
					NodeList dictEntryidentifier = (NodeList) xPath.evaluate(
							"//dictionaryEntry/ListedValueDefinition/identifier/text()", listedvaluelist.item(0),
							XPathConstants.NODESET);
					NodeList dictentryText = (NodeList) xPath.evaluate(
							"//dictionaryEntry/ListedValueDefinition/name/text()", listedvaluelist.item(0),
							XPathConstants.NODESET);
					NodeList descriptionentryText = (NodeList) xPath.evaluate(
							"//dictionaryEntry/ListedValueDefinition/description/text()", listedvaluelist.item(0),
							XPathConstants.NODESET);
					NodeList valueTypeName = (NodeList) xPath.evaluate("valueTypeName/text()", listedvaluelist.item(0),
							XPathConstants.NODESET);
					for (int j = 0; j < dictEntryidentifier.getLength(); j++) {
						// System.out.println("DictEntryIdentifier:
						// "+dictEntryidentifier.item(j).toString());
						String id = dictEntryidentifier.item(j).getTextContent()
								.substring(dictEntryidentifier.item(j).getTextContent().lastIndexOf(':') + 1);
						NodeList enumIndividials;
						enumIndividials = (NodeList) xPath.evaluate(
								"//*[local-name()='NamedIndividual' and @ID='" + valueTypeName + "_" + id + "']",
								resultOWLdoc.getDocumentElement(), XPathConstants.NODESET);
						for (int l = 0; l < enumIndividials.getLength(); l++) {
							System.out.println("EnumIndividuals: " + enumIndividials.item(l).toString() + " - "
									+ enumIndividials.getLength());
							Element comment = resultOWLdoc.createElement("rdfs:label");
							comment.setAttribute("xml:lang", "de");
							comment.setTextContent(dictentryText.item(j).getTextContent());
							enumIndividials.item(l).appendChild(comment);
							if (descriptionentryText.getLength() > j) {
								comment = resultOWLdoc.createElement("rdfs:comment");
								comment.setTextContent(descriptionentryText.item(j).getTextContent());
								enumIndividials.item(l).appendChild(comment);
							}
						}
					}

				}
				for (int j = 0; j < objectPropertyList.getLength(); j++) {
					System.out.println("InsertNode: " + objectPropertyList.item(0).getTextContent());
					Element comment = resultOWLdoc.createElement("rdfs:domain");
					comment.setAttribute("rdf:resource", propName);
					objectPropertyList.item(j).appendChild(comment);
				}
			}
			resultOWLdoc = this.cleanUpUsingDOMTree(resultOWLdoc, aaaxmldoc, superClass, uriPrefix);

			// write out the modified document to a new file
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer = tFactory.newTransformer();
			Source source = new DOMSource(resultOWLdoc);
			Result output = new StreamResult(new File(resultFile));
			transformer.transform(source, output);
		}
	}

	public OntModel useOntMalizer(String filepath, String format, Extension ending) {
		OntModel result;
		XSD2OWLMapper mapping = new XSD2OWLMapper(new File(filepath.toString()));
		mapping.setObjectPropPrefix("");
		mapping.setDataTypePropPrefix("");
		mapping.convertXSD2OWL();
		if (ending != Extension.XSD) {
			XML2OWLMapper generator = new XML2OWLMapper(new File(filepath.toString()), mapping);
			generator.convertXML2OWL();
		}

		// This part prints the ontology to the specified file.
		FileOutputStream ont;
		try {
			File f = new File(filepath.toString().substring(0, filepath.toString().lastIndexOf(".")) + ending);
			f.getParentFile().mkdirs();
			ont = new FileOutputStream(f);
			mapping.writeOntology(ont, format);
			ont.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		// BrokerOntologyConnection.log.debug("OntModel: "+result.toString());
		return null;
	}

	private void transformWaterML2() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/waterml/2.0.2/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/waterml/2.0.2/watermlx_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					xsdowl.transform("xsd/waterml/2.0.2/" + filepath, owlfile, "xsl/xsd2owl4.xsl",
							"http://www.opengis.net/waterml/2.0#",
							filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.')), "WaterML2",
							"en");
					xsdowl.justCleanUp(owlfile + ".owl", "xsd/waterml/2.0.2/" + filepath, owlfile + ".owl", "WaterML2",
							"http://www.opengis.net/waterml/2.0#");
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void transformXPlanung2() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		xsdowl.transformercounter = 0;
		String owlfile = "xsd/XPlanung2/xplanung2_testxx";
		String xsdfile = "xsd/XPlanung2/XPlanGml.xsd";
		xsdowl.transform(xsdfile, owlfile, "xsl/xs2owl2.0.10.xsl", "http://www.xplanung.de/xplangml#", "XPlanung2", null, "de");
		xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung2", "http://www.xplanung.de/xplangml#");
		xsdowl.integrateCodeList(new File("xsd/XPlanung2/XPlanGml_CodeLists.xml"), owlfile + ".owl",
				"http://www.xplanung.de/xplangml#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN, "XPlanung2");
	}
	
	private void transformXErleben2() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
	TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
XSD2OWL xsdowl = new XSD2OWL();
xsdowl.transformercounter = 0;
//String owlfile = "xsd/xerleben/xerleben2_testxx";
//String xsdfile = "xsd/xerleben/xe_basic.xsd";
//"xsl/xs2owl2.0.10.xsl"
//xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl4.xsl", "http://www.xerleben.de/schema/2.0_1/", "XErleben2", null, "de");
File file = new File("xsd/xerleben/");
xsdowl.transformercounter = 0;

for (String filepath : file.list()) {
	if (filepath.endsWith(".xsd")) {
		try {
			String owlfile = "xsd/xerleben/xerleben2_"
					+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
			String xsdfile = "xsd/xerleben/" + filepath;
			String namespace = "http://www.xerleben.de/schema/2.0_1/";
			xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "XErleben2", null, "de");
			xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XErleben2", namespace);
			// TODO Integrate Codelists
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
File folder = new File("xsd/xerleben");
String start = "";
for (File filee : folder.listFiles()) {
	if (filee.getName().startsWith("xerleben2_")) {
		start = filee.getAbsolutePath();
		break;
	}
}
xsdowl.domMerger(new File("xsd/xerleben/"), ".*xerleben2_.*$", start, "xsd/xerleben/xerleben2xmerged.owl");


//xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung2", "http://www.xplanung.de/xplangml#");
//xsdowl.integrateCodeList(new File("xsd/XPlanung2/XPlanGml_CodeLists.xml"), owlfile + ".owl",
		//"http://www.xplanung.de/xplangml#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN, "XPlanung2");
}

	private void transformXPlanung3() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		xsdowl.transformercounter = 0;
		String owlfile = "xsd/XPlanung3/xplanung3_testx";
		String xsdfile = "xsd/XPlanung3/XPlanGML.xsd";
		xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl4.xsl", "http://www.xplanung.de/xplangml/3/0#", "XPlanung3", null, "de");
		xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung3", "http://www.xplanung.de/xplangml/3/0#");
		xsdowl.integrateCodeList(new File("xsd/XPlanung3/XPlanGML_CodeLists.xml"), owlfile + ".owl",
				"http://www.xplanung.de/xplangml/3/0#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN, "XPlanung3");
	}

	private void transformKML23() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		xsdowl.transformercounter = 0;
		String owlfile = "xsd/kml23/kml23_";
		String xsdfile = "xsd/kml23/ogckml23.xsd";
		xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl4.xsl", "http://www.opengis.net/kml/2.2#", "KML23", null, "en");
		xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "KML23", "http://www.opengis.net/kml/2.2#");
	}

	private void transformSWE() throws XPathExpressionException, ParserConfigurationException, SAXException,
			IOException, TransformerException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/sweCommon/2.0.1/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/sweCommon/2.0.1/swe_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/sweCommon/2.0.1/" + filepath;
					String namespace = "http://www.isotc211.org/2005/swe#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "SWE", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "SWE", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/sweCommon/2.0.1/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("swe_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/sweCommon/2.0.1/"), "^.+[swe]_.+\\.owl$", start,
				"xsd/sweCommon/2.0.1/swemerged.owl");
	}

	private void transformGMD() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/iso/19139/20070417/gmd/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/iso/19139/20070417/gmd/gmd_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/iso/19139/20070417/gmd/" + filepath;
					String namespace = "http://www.isotc211.org/2005/gmd#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GMD", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GMD", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/XPlanung4/iso/19139/20070417/gmd/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gmd_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/XPlanung4/iso/19139/20070417/gmd/"), "^.+[gmd]_.+\\.owl$", start,
				"xsd/XPlanung4/iso/19139/20070417/gmd/gmdmerged.owl");

	}

	private void transformGCO() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/iso/19139/20070417/gco/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/iso/19139/20070417/gco/gco_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/iso/19139/20070417/gco/" + filepath;
					String namespace = "http://www.isotc211.org/2005/gco#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GCO", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GCO", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/XPlanung4/iso/19139/20070417/gco/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gco_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/XPlanung4/iso/19139/20070417/gco/"), "^.+[gco]_.+\\.owl$", start,
				"xsd/XPlanung4/iso/19139/20070417/gco/gcomerged.owl");

	}

	private void transformGMX() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/iso/19139/20070417/gmx/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/iso/19139/20070417/gmx/gmx_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/iso/19139/20070417/gmx/" + filepath;
					String namespace = "http://www.isotc211.org/2005/gmx#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GMX", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GMX", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/XPlanung4/iso/19139/20070417/gmx/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gmx_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/XPlanung4/iso/19139/20070417/gmx/"), "^.+[gmx]_.+\\.owl$", start,
				"xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged.owl");
		xsdowl.integrateGMXCodelist(new File("xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged.owl"),
				"xsd/XPlanung4/iso/19139/20070417/gmx/gmxCodelists.xml",
				"xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged2.owl", "http://www.isotc211.org/2005/gmx#");
		xsdowl.integrateGMXCodelist(new File("xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged2.owl"),
				"xsd/XPlanung4/iso/19139/20070417/gmx/gmiCodelists.xml",
				"xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged2.owl", "http://www.isotc211.org/2005/gmx#");
		xsdowl.integrateGMXCodelist(new File("xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged2.owl"),
				"xsd/XPlanung4/iso/19139/20070417/gmx/ML_gmxCodelists.xml",
				"xsd/XPlanung4/iso/19139/20070417/gmx/gmxmerged2.owl", "http://www.isotc211.org/2005/gmx#");

	}

	private void transformGSR() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/iso/19139/20070417/gsr/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/iso/19139/20070417/gsr/gsr_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/iso/19139/20070417/gsr/" + filepath;
					String namespace = "http://www.isotc211.org/2005/gsr#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GSR", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GSR", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/XPlanung4/iso/19139/20070417/gsr/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gsr_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/XPlanung4/iso/19139/20070417/gsr/"), "^.+[gsr]_.+\\.owl$", start,
				"xsd/XPlanung4/iso/19139/20070417/gsr/gsrmerged.owl");

	}

	private void transformGSS() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/iso/19139/20070417/gss/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/iso/19139/20070417/gss/gss_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/iso/19139/20070417/gss/" + filepath;
					String namespace = "http://www.isotc211.org/2005/gss#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GSS", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GSS", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/XPlanung4/iso/19139/20070417/gss/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gss_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/XPlanung4/iso/19139/20070417/gss/"), "^.+[gss]_.+\\.owl$", start,
				"xsd/XPlanung4/iso/19139/20070417/gss/gssmerged.owl");

	}

	private void transformGTS() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/iso/19139/20070417/gts/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/iso/19139/20070417/gts/gts_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/iso/19139/20070417/gts/" + filepath;
					String namespace = "http://www.isotc211.org/2005/gts#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GTS", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GTS", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/XPlanung4/iso/19139/20070417/gts/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gts_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/XPlanung4/iso/19139/20070417/gts/"), "^.+[gts]_.+\\.owl$", start,
				"xsd/XPlanung4/iso/19139/20070417/gts/gtsmerged.owl");

	}

	private void transformGML3() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		xsdowl.transformercounter = 0;
		String owlfile = "xsd/XPlanung2/gml3nasx";
		String xsdfile = "xsd/XPlanung2/gml/3.0.0/base/gml3nas.xsd";
		xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl4.xsl", "http://www.semgis.de/gml3#", "GML3", null, "en");
		xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GML3", "http://www.semgis.de/gml3#");
	}

	private void transformGML32() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		xsdowl.transformercounter = 0;
		String owlfile = "xsd/XPlanung4/gmlProfilexplanx";
		String xsdfile = "xsd/XPlanung4/gmlProfile/gmlProfilexplan.xsd";
		xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl4.xsl", "http://www.opengis.net/gml/3.2#", "GML32", null, "en");
		xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GML32", "http://www.opengis.net/gml/3.2#");
	}

	private void transformGML32_2() throws TransformerConfigurationException, TransformerFactoryConfigurationError,
			TransformerException, XPathExpressionException, SAXException, IOException, ParserConfigurationException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/gml32/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/gml32/gml32_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/gml32/" + filepath;
					String namespace = "http://www.opengis.net/gml/3.2#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "GML32", null, "en");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "GML32", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		File folder = new File("xsd/gml32/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("gml32_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/gml32/"), "^.+[gml32]_.+\\.owl$", start, "xsd/gml32/gml32merged.owl");
	}

	private void transformXPlanung4() throws XPathExpressionException, ParserConfigurationException, SAXException,
			IOException, TransformerException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung4/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung4/xplan4x_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung4/" + filepath;
					String namespace = "http://www.xplanung.de/xplangml/4/0#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "XPlanung4", null, "de");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung4", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		File folder = new File("xsd/XPlanung4");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("xplan4x_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		xsdowl.domMerger(new File("xsd/XPlanung4/"), ".*xplan4x_.*$", start, "xsd/XPlanung4/xplan4xmerged.owl");
		xsdowl.integrateCodeList(new File("xsd/XPlanung4/index.xplan.definitions.xml"),
				"xsd/XPlanung4/xplan4xmerged.owl", "http://www.xplanung.de/xplangml/4/0#", "xsl/aaacodelist2owl.xsl",
				XMLTypes.XPLAN4, "XPlanung4");
		xsdowl.integrateCodeList(new File("xsd/XPlanung4/definitions_ADE_FHH.xml"), "xsd/XPlanung4/xplan4xmerged.owl",
				"http://semgis.de/xplan4#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN4, "XPlanung4");
	}
	
	private void transformXPlanung5() throws XPathExpressionException, ParserConfigurationException, SAXException,
	IOException, TransformerException {
XSD2OWL xsdowl = new XSD2OWL();
File file = new File("xsd/XPlanung5/");
xsdowl.transformercounter = 0;
/*
 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
 * "INSPIRE4","base");
 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
 * "http://www.semgis.de/inspire4#");
 */
for (String filepath : file.list()) {
	if (filepath.endsWith(".xsd")) {
		try {
			/*
			 * String
			 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
			 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
			 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
			 * owlfile,"xsl/xsd2owl4.xsl",
			 * "http://www.opengis.net/swe/2.0#","SWE2",null);
			 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
			 * "http://www.opengis.net/swe/2.0#");
			 */
			String owlfile = "xsd/XPlanung5/xplan5x_"
					+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
			String xsdfile = "xsd/XPlanung5/" + filepath;
			String namespace = "http://www.xplanung.de/xplangml/5/0#";
			xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "XPlanung5", null, "de");
			xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung5", namespace);
			// TODO Integrate Codelists
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
File folder = new File("xsd/XPlanung5");
String start = "";
for (File filee : folder.listFiles()) {
	if (filee.getName().startsWith("xplan5x_")) {
		start = filee.getAbsolutePath();
		break;
	}
}
xsdowl.domMerger(new File("xsd/XPlanung5/"), ".*xplan5x_.*$", start, "xsd/XPlanung5/xplan5xmerged.owl");
xsdowl.integrateCodeList(new File("xsd/XPlanung5/index.xplan.definitions.xml"),
		"xsd/XPlanung5/xplan5xmerged.owl", "http://www.xplanung.de/xplangml/5/0#", "xsl/aaacodelist2owl.xsl",
		XMLTypes.XPLAN5, "XPlanung5");
xsdowl.integrateCodeList(new File("xsd/XPlanung5/definitions_ADE_FHH.xml"), "xsd/XPlanung5/xplan5xmerged.owl",
		"http://semgis.de/xplan5#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN5, "XPlanung5");
}
	
	private void transformXPlanung52() throws XPathExpressionException, ParserConfigurationException, SAXException,
	IOException, TransformerException {
XSD2OWL xsdowl = new XSD2OWL();
File file = new File("xsd/XPlanung52/");
xsdowl.transformercounter = 0;
/*
 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
 * "INSPIRE4","base");
 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
 * "http://www.semgis.de/inspire4#");
 */
for (String filepath : file.list()) {
	if (filepath.endsWith(".xsd")) {
		try {
			/*
			 * String
			 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
			 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
			 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
			 * owlfile,"xsl/xsd2owl4.xsl",
			 * "http://www.opengis.net/swe/2.0#","SWE2",null);
			 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
			 * "http://www.opengis.net/swe/2.0#");
			 */
			String owlfile = "xsd/XPlanung52/xplan52x_"
					+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
			String xsdfile = "xsd/XPlanung52/" + filepath;
			String namespace = "http://www.xplanung.de/xplangml/5/2#";
			xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "XPlanung52", null, "de");
			xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung52", namespace);
			// TODO Integrate Codelists
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
File folder = new File("xsd/XPlanung52");
String start = "";
for (File filee : folder.listFiles()) {
	if (filee.getName().startsWith("xplan52x_")) {
		start = filee.getAbsolutePath();
		break;
	}
}
xsdowl.domMerger(new File("xsd/XPlanung52/"), ".*xplan52x_.*$", start, "xsd/XPlanung52/xplan52xmerged.owl");
xsdowl.integrateCodeList(new File("xsd/XPlanung52/index.xplan.definitions.xml"),
		"xsd/XPlanung52/xplan52xmerged.owl", "http://www.xplanung.de/xplangml/5/2#", "xsl/aaacodelist2owl.xsl",
		XMLTypes.XPLAN5, "XPlanung52");
xsdowl.integrateCodeList(new File("xsd/XPlanung52/definitions_ADE_FHH.xml"), "xsd/XPlanung5/xplan52xmerged.owl",
		"http://semgis.de/xplan52#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN5, "XPlanung52");
}

	private void transformXPlanung41() throws XPathExpressionException, ParserConfigurationException, SAXException,
			IOException, TransformerException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/XPlanung41/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/XPlanung41/xplan41_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/XPlanung41/" + filepath;
					String namespace = "http://semgis.de/xplan41#";
					System.out.println("Transform: "+xsdfile+" - "+owlfile);
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", namespace, "XPlanung41", null, "de");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "XPlanung41", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		File folder = new File("xsd/XPlanung41");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("xplan41_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		xsdowl.domMerger(new File("xsd/XPlanung41/"), ".*xplan41_.*$", start, "xsd/XPlanung41/xplan41merged.owl");
		xsdowl.integrateCodeList(new File("xsd/XPlanung41/CodeLists/"), "xsd/XPlanung41/xplan41merged.owl",
				"http://semgis.de/xplan41#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN4, "XPlanung41");
	}

	private void transformAAA() throws XPathExpressionException, ParserConfigurationException, SAXException,
			IOException, TransformerException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/NAS_6.0/schema");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		for (String filepath : file.list()) {
			if (filepath.endsWith(".xsd")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/NAS_6.0/schema/aaa6x_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
					String xsdfile = "xsd/NAS_6.0/schema/" + filepath;
					String namespace = "http://semgis.de/aaa6#";
					xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", "http://semgis.de/aaa6#", "AAA6", null,
							"de");
					xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "AAA6", namespace);
					// TODO Integrate Codelists
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		File folder = new File("xsd/NAS_6.0/schema/");
		String start = "";
		for (File filee : folder.listFiles()) {
			if (filee.getName().startsWith("aaa6x_")) {
				start = filee.getAbsolutePath();
				break;
			}
		}
		xsdowl.domMerger(new File("xsd/NAS_6.0/schema/"), "^.+[aaa6x]_.+\\.owl$", start,
				"xsd/NAS_6.0/schema/aaa6xmerged.owl");
		xsdowl.integrateCodeList(new File("xsd/NAS_6.0/definitions"), "xsd/NAS_6.0/schema/aaa6xmerged.owl",
				"http://semgis.de/aaa6#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN4, "AAA6");
		// xsdowl.justCleanUp("xsd/NAS_6.0/schema/aaa6xmerged.owl","xsd/NAS_6.0/schema/AAA-Basisschema.xsd","xsd/NAS_6.0/schema/aaa6xmerged.owl","AAA6","http://semgis.de/aaa6#");
	}
	
	private void transformAAA7() throws XPathExpressionException, ParserConfigurationException, SAXException,
	IOException, TransformerException {
XSD2OWL xsdowl = new XSD2OWL();
File file = new File("xsd/NAS_7.0.3/");
xsdowl.transformercounter = 0;
/*
 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
 * "INSPIRE4","base");
 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
 * "http://www.semgis.de/inspire4#");
 */
for (String filepath : file.list()) {
	if (filepath.endsWith(".xsd")) {
		try {
			/*
			 * String
			 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
			 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
			 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
			 * owlfile,"xsl/xsd2owl4.xsl",
			 * "http://www.opengis.net/swe/2.0#","SWE2",null);
			 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
			 * "http://www.opengis.net/swe/2.0#");
			 */
			String owlfile = "xsd/NAS_7.0.3/aaa7x_"
					+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
			String xsdfile = "xsd/NAS_7.0.3/" + filepath;
			String namespace = "http://www.adv-online.de/namespaces/adv/gid/7.0#";
			xsdowl.transform(xsdfile, owlfile, "xsl/xsd2owl5.xsl", "http://www.adv-online.de/namespaces/adv/gid/7.0#", "AAA7", null,
					"de");
			xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", "AAA7", namespace);
			// TODO Integrate Codelists
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
File folder = new File("xsd/NAS_7.0.3/");
String start = "";
for (File filee : folder.listFiles()) {
	if (filee.getName().startsWith("aaa7x_")) {
		start = filee.getAbsolutePath();
		break;
	}
}
xsdowl.domMerger(new File("xsd/NAS_7.0.3/"), "^.+[aaa7x]_.+\\.owl$", start,
		"xsd/NAS_7.0.3/aaa7xmerged.owl");
xsdowl.integrateCodeList(new File("xsd/NAS_7.0.3/definitions"), "xsd/NAS_7.0.3/aaa7xmerged.owl",
		"http://www.adv-online.de/namespaces/adv/gid/7.0#", "xsl/aaacodelist2owl.xsl", XMLTypes.AAA, "AAA7");
// xsdowl.justCleanUp("xsd/NAS_6.0/schema/aaa6xmerged.owl","xsd/NAS_6.0/schema/AAA-Basisschema.xsd","xsd/NAS_6.0/schema/aaa6xmerged.owl","AAA6","http://semgis.de/aaa6#");
}

	private void transformINSPIRE4() throws XPathExpressionException, ParserConfigurationException, SAXException,
			IOException, TransformerException {
		XSD2OWL xsdowl = new XSD2OWL();
		File file = new File("xsd/INSPIRE4/");
		xsdowl.transformercounter = 0;
		/*
		 * String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#");
		 */
		/*
		 * for(String filepath:file.list()){ if(filepath.endsWith(".xsd")){ try{
		 * /*String
		 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(filepath.
		 * lastIndexOf('/')+1,filepath.lastIndexOf('.'));
		 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.opengis.net/swe/2.0#","SWE2",
		 * null); xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
		 * "http://www.opengis.net/swe/2.0#"); String
		 * owlfile="xsd/INSPIRE4/inspire4x_"+filepath.substring(filepath.
		 * lastIndexOf('/')+1,filepath.lastIndexOf('.')); String
		 * xsdfile="xsd/INSPIRE4/"+filepath; String
		 * namespace="http://semgis.de/inspire4#"; xsdowl.transform(xsdfile,
		 * owlfile,"xsl/xsd2owl4.xsl",
		 * namespace,filepath.substring(filepath.lastIndexOf('/')+1,filepath.
		 * lastIndexOf('.')),"INSPIRE4","en");
		 * xsdowl.justCleanUp(owlfile+".owl",xsdfile,owlfile+".owl","INSPIRE4",
		 * namespace); xsdowl.transformINSPIRECodeList(owlfile, owlfile,
		 * owlfile, "INSPIRE4", namespace); }catch(Exception e){
		 * e.printStackTrace(); } } }
		 */
		file = new File("xsd/INSPIRE4/CodeListsen/");
		for (String filepath : file.list()) {
			if (filepath.endsWith(".rdf")) {
				try {
					/*
					 * String
					 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(
					 * filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.'));
					 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
					 * owlfile,"xsl/xsd2owl4.xsl",
					 * "http://www.opengis.net/swe/2.0#","SWE2",null);
					 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
					 * "http://www.opengis.net/swe/2.0#");
					 */
					String owlfile = "xsd/INSPIRE4/CodeListsen/inspire4x_"
							+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.')) + ".owl";
					String xsdfile = "xsd/INSPIRE4/CodeListsen/" + filepath;
					String namespace = "http://semgis.de/inspire4#";
					xsdowl.transformINSPIRECodeList(xsdfile, owlfile, owlfile, "INSPIRE4", namespace);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		// xsdowl.domMerger(new File("xsd/INSPIRE4/"),".owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		xsdowl.domMerger(new File("xsd/INSPIRE4/CodeListsen/"), "^.*\\.owl$",
				"xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
				"xsd/INSPIRE4/CodeListsen/merged.owl");
	}

	public static void main(String[] args)
			throws TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException,
			SAXException, IOException, ParserConfigurationException, XPathExpressionException {
		System.setProperty("file.encoding", "UTF-8");
		XSD2OWL xsdowl = new XSD2OWL();
		xsdowl.transformAAA7();
		//xsdowl.transformXPlanung52();
		//xsdowl.transformSubFolders("xsd/Xplanung52/", "xsl/xsd2owl5.xsl");
		//xsdowl.transformAAA();
		/*
		 * xsdowl.transformGMD(); xsdowl.transformGCO(); xsdowl.transformGMX();
		 * xsdowl.transformGSR(); xsdowl.transformGSS(); xsdowl.transformGTS();
		 */
		// xsdowl.transformKML23();
		// xsdowl.transformSWE();
		// xsdowl.transformGML32_2();
		// xsdowl.integrateDifferentXPlanVersions("xsd/xplanversionmerge.owl",
		// "xsd/xplanversionmerge2.owl", "http://semgis.de/xplan2#",
		// "http://semgis.de/xplan3#","http://semgis.de/xplan2#XPlanung2","http://semgis.de/xplan3#XPlanung3");
		// xsdowl.domMerger(new File("xsd/INSPIRE4/CodeListsen/"),"^.*\\.owl$",
		// "xsd/INSPIRE4/CodeListsen/inspire4x_ad_GeometryMethodValue.en.owl",
		// "xsd/INSPIRE4/CodeListsen/merged.owl");
		// xsdowl.integrateINSPIRECodeListEnums(new File("xsd/mergertest.owl"),
		// "xsd/mergertest2.owl","http://semgis.de/inspire4#");
		/*
		 * File file=new File("xsd/INSPIRE4/"); xsdowl.transformercounter=0;
		 * 
		 * /*String owlfile="xsd/INSPIRE4/inspire4_basetypes_test";
		 * xsdowl.transform("xsd/INSPIRE4/BaseTypes.xsd",
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.semgis.de/inspire4#",
		 * "INSPIRE4","base");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://www.semgis.de/inspire4#"); for(String filepath:file.list()){
		 * if(filepath.endsWith(".xsd")){ try{ /*String
		 * owlfile="xsd/sweCommon/2.0.1/swe_"+filepath.substring(filepath.
		 * lastIndexOf('/')+1,filepath.lastIndexOf('.'));
		 * xsdowl.transform("xsd/sweCommon/2.0.1/"+filepath,
		 * owlfile,"xsl/xsd2owl4.xsl","http://www.opengis.net/swe/2.0#","SWE2",
		 * null); xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","SWE2",
		 * "http://www.opengis.net/swe/2.0#"); String
		 * owlfile="xsd/INSPIRE4/inspire4_"+filepath.substring(filepath.
		 * lastIndexOf('/')+1,filepath.lastIndexOf('.'));
		 * xsdowl.transform("xsd/INSPIRE4/"+filepath,
		 * owlfile,"xsl/xsd2owl4.xsl","http://semgis.de/inspire4#",filepath.
		 * substring(filepath.lastIndexOf('/')+1,filepath.lastIndexOf('.')),
		 * "INSPIRE4","en");
		 * xsdowl.justCleanUp(owlfile+".owl",owlfile+".owl","INSPIRE4",
		 * "http://semgis.de/inspire4#"); }catch(Exception e){
		 * e.printStackTrace(); } } }
		 */
		/*
		 * try { OWLOntology result=manager.createOntology(); } catch
		 * (OWLOntologyCreationException e1) { // TODO Auto-generated catch
		 * block e1.printStackTrace(); }
		 */
		/*
		 * for(File filepath:file.listFiles()){
		 * if(filepath.getAbsolutePath().endsWith(".owl") &&
		 * filepath.getAbsolutePath().contains("_")){ try { OWLOntology
		 * ontology=manager.loadOntologyFromOntologyDocument(filepath); merger.
		 * } catch (OWLOntologyCreationException e) { // TODO Auto-generated
		 * catch block e.printStackTrace(); } } }
		 */

		/*
		 * xsdowl.transform("xsd/XPlanung4/gmlProfile/gmlProfilexplan.xsd",
		 * "xsd/XPlanung4/gmlProfile/gmlProfilexplan","xsl/xsd2owl4.xsl");
		 * xsdowl.enrichDocWithGMLTypes(
		 * "xsd/XPlanung4/gmlProfile/gmlProfilexplan.owl",
		 * "xsd/XPlanung4/gmlProfile/gmlProfilexplan.xsd",
		 * "http://www.opengis.net/gml/3.2#");
		 * xsdowl.justCleanUp("xsd/XPlanung4/gmlProfile/gmlProfilexplan.owl",
		 * "xsd/XPlanung4/gmlProfile/gmlProfilexplan.owl", "GML32",
		 * "http://www.opengis.net/gml/3.2#");
		 */
		// xsdowl.integrateCodeList(new
		// File("xsd/XPlanung3/XPlanGML_CodeLists.xml"),
		// "xsd/XPlanung3/xplan3testnew.owl",
		// "http://semgis.de/xplan3#","xsl/aaacodelist2owl.xsl",XMLTypes.XPLAN,"XPlanung3");
		// xsdowl.mergeAAACodeListAndTransform("xsd/NAS_6.0/schema/aaa_fachschema.owl",
		// "xsd/NAS_6.0/definitions/AX_LagebezeichnungMitHausnummer.definitions.xml",
		// "xsd/NAS_6.0/schema/aaa_fachschema2.owl");
	}



	public OntModel importDataSource(URI uri, Extension ext, Boolean evaluateOSM) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	
	public void transformSubFolders(String folder,String xslfile) throws XPathExpressionException, ParserConfigurationException, SAXException,
	IOException, TransformerException {
		String foldername=folder.substring(0,folder.length()-1);
				foldername=foldername.substring(foldername.lastIndexOf('/')+1);
XSD2OWL xsdowl = new XSD2OWL();
File file = new File(folder);
xsdowl.transformercounter = 0;
for (String filepath : file.list()) {
	if (filepath.endsWith(".xsd")) {
		try {
			String owlfile = "owl/"+foldername+"/"+folder.substring(folder.lastIndexOf('/')+1)+foldername+"_"
					+ filepath.substring(filepath.lastIndexOf('/') + 1, filepath.lastIndexOf('.'));
			String xsdfile = folder + filepath;
			String namespace = "http://www.xplanung.de/xplangml/4/0#";
			xsdowl.transform(xsdfile, owlfile, xslfile, namespace, foldername, "", "de");
			xsdowl.justCleanUp(owlfile + ".owl", xsdfile, owlfile + ".owl", foldername, namespace);
			// TODO Integrate Codelists
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
String start = "";
File outputfolder=new File("owl/"+foldername+"/");
for (File filee : outputfolder.listFiles()) {
	System.out.println(filee.getAbsolutePath());
	if (filee.getName().startsWith(foldername)) {
		start = filee.getAbsolutePath();
		break;
	}
}
xsdowl.domMerger(new File("owl/"+foldername+"/"), ".*"+foldername+"_.*$", start, "owl/"+foldername+"/"+foldername+"merged.owl");
File codelistfolder=new File(folder+"/codelist/");
for(File codelist:codelistfolder.listFiles()) {
	xsdowl.integrateCodeList(codelist, folder+"/"+foldername+"merged.owl", "http://www.xplanung.de/xplangml/4/0#", "xsl/aaacodelist2owl.xsl",
			XMLTypes.XPLAN4, foldername);
}
/*xsdowl.integrateCodeList(new File(folder+"/codelist/index.xplan.definitions.xml"),
		folder+"/"+foldername+"merged.owl", "http://www.xplanung.de/xplangml/4/0#", "xsl/aaacodelist2owl.xsl",
		XMLTypes.XPLAN4, foldername);
xsdowl.integrateCodeList(new File(folder+"/codelist/definitions_ADE_FHH.xml"), folder+"/"+foldername+"merged.owl",
		"http://semgis.de/xplan4#", "xsl/aaacodelist2owl.xsl", XMLTypes.XPLAN4, foldername);*/
}
	
}

