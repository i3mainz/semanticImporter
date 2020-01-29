<?xml version="1.0"?>

<!-- 
 $Author: brischniz $
 $Header: /cvsroot/xml2owl/xsl/xsd2owl.xsl,v 1.3 2005/10/23 14:33:56 brischniz Exp $
 $Date: 2005/10/23 14:33:56 $
 $Id: xsd2owl.xsl,v 1.3 2005/10/23 14:33:56 brischniz Exp $
 $Revision: 1.3 $
 -->
 
<!-- 
     XSD2OWL.XSL
 
    This collection of stylesheets transformes given XML or XML Schema files to an ontology in the format of OWL (web
    ontology language). We focus on data oriented XML, especially on XML exports from relational databases 
    like MySQL or Firebird, but all other XML data should be converted without problems, too.
    
    Every stylesheet of the collection belongs to a specific level of the transformation process. Each of them is controlled 
    by the main stylesheet and entry point 'xsd2owl.xsl' (this one).
    
    The others:
    
    # xml2xsd.xsl - this stylesheet extracts an XML Schema out of an XML instance file
    # createOWLModel.xsl - this stylesheet creates the OWL model out of an XML Schema
    # createStylesheet.xsl - this stylesheet creates a configured stylesheet for OWL instances
    # createInstances.xsl - this is the created stylesheet for the mapping of the XML instance data to OWL instances
    
    
    The created ontology can be loaded into ontology-editors like http://protege.stanford.edu or 
    http://powl.sf.net/". There are several Use Cases available on the project homepage.
 
    I use some extensions from 'http://www.exslt.org' for dynamic purposes:
    
    # dyn - for composing XPath expressions dynamically
    # exslt - for generating multiple result documents and converting back a result-tree-fragment into a node-set
    # set - for extracting unique elements out of a set, not using the slow way over the preceding-sibling-axis
    # str - for some string processing
    
    I also include another extension library from 'http://xsltsl.org'. It consist of a set of stylesheets, so no special
    XSLT processor is needed. This library is used for the camelCase functionality. The stylesheets must be available
    in the xsl/ directory of the xml2owl-package.
    
    For more detailed describtion, please consider our project homepage at 
    'http://www.semanticscripting.org/XML2OWL_XSLT'
-->
<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:xs="http://www.w3.org/2001/XMLSchema" 
    xmlns:xsd="http://www.w3.org/2001/XMLSchema" 
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"    
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#" 
    xmlns:owl="http://www.w3.org/2002/07/owl#"
    xmlns:exslt="http://exslt.org/common"     
    xmlns:dyn="http://exslt.org/dynamic" 
    xmlns:set="http://exslt.org/sets" 
    xmlns:str="http://exslt.org/strings"     
    xmlns:xslt="http://xslt"
    extension-element-prefixes="str exslt dyn set">
    
    <xsl:output method="xml" indent="yes"/>
    <xsl:namespace-alias stylesheet-prefix="xslt" result-prefix="xsl"/>
    <xsl:strip-space elements="*"/>
    
    <!-- 
        The stylesheets can be configured with a few parameters listed below:
    
        Parameter 'input': (mandatory)
        'xsd' - The stylesheet assumes, that there is a XML Schema file and will create an OWL model and a configured stylesheet for instance processing
        'xml' - An XML Schema will be created out of the given XML instance file. This Schema is the base for the OWL model        
    -->
    <xsl:param name="input"/>
    
    <!-- 
        Parameter 'dtpPrefix': (optional)
        A prefix for owl:DatatypeProperties can be determined to distinguish classes and properties with the same name. Default value is "dtp".
    -->
    <xsl:param name="dtpPrefix" select="'dtp'"/>
    
    <!-- 
        Parameter 'opPrefix': (optional)
        A prefix for owl:ObjectProperties can be determined. Default value is "has".
    -->
    <xsl:param name="opPrefix" select="'has'"/>
    
     <!-- 
        Parameter 'modelName': (optional)
        Determines the filename of the model.
     -->
    <xsl:param name="modelName" select="'model.owl'"/>
    
     <!-- 
       Parameter 'instancesName': (optional)
       Determines the filename of the OWL instances document.
     -->
    <xsl:param name="instancesName" select="'instances.owl'"/>
    
    <!-- 
        Parameter 'namespace_uri':
    -->
    <xsl:param name="namespace_uri"/>
    
    <!-- 
        Parameter 'importURIsForModel': (optional)
        Is a '|' separated string, which contains URIs. These URIs will be used for the owl:import mechanism
        in the ontology header of the OWL model file. It can be used, to import other ontologies.
    
        Note: not implemented yet!
    -->
    <xsl:param name="importURIsForModel"/>
    
    <!-- 
        Parameter 'camelCase': (optional)
        Forces the stylesheet to use classnames and property names with camelCase format.
        !!! experimental !!!
    -->
    <xsl:param name="camelCase" select="'false'"/>
    
    <!-- 
        Parameter 'ontologyLabel': (optional)
        This parameter contains a string, which is to be inserted in the ontology header as label.
    -->
    <xsl:param name="ontologyLabel" select="'This is a automatically by xml2owl generated ontology.'"/>
    
    <!-- 
        Parameter 'ontologyComment': (optional)
        This parameter contains a string, which is to be inserted in the ontology header as comment.
    -->
    <xsl:param name="ontologyComment" select="'This is a automatically by xml2owl generated ontology.'"/>
    
    <!-- 
        Parameter 'targetPath': (optional)
        This parameter defines the path, where the result documents will be placed. This parameter is optional,
        but it is recommended to specify a path.
    -->
    <xsl:param name="targetPath" select="'.'"/>
    
    
    <!-- 
        Parameter 'pathToXSLTSLlib': (optional)
        This parameter defines the path, where the XSLTSL library can be found by the generated 
        stylesheet that processes the XML instances to OWL instances in the last step.
        It is necessary, if the result documents are not in the same directory like the framework stylesheets
        (should be normally the case!)
    -->
    <xsl:param name="pathToXSLTSLlib" select="'../libs/xsltsl-1.2.1/stdlib.xsl'"/>
    
    <!-- 
      Parameter 'toggleFunctionalPropertySupport': (optional)
        If set 'true', this parameter lets the stylesheet 'createOWLmodel.xsl' create owl:FunctionalProperties. Every xsd:attribute
        will become a functional property and if other owl:DatatypeProperties have minCardinality = maxCardinality = 1. 
    -->
    <xsl:param name="toggleFunctionalPropertySupport" select="'true'"/>
    
    <!-- 
        Parameter 'debug': (optional)
        With this parameter a debug level can be set. Following values are possible:
        0 - quiet, this is the default value
        1 - little output of the transformation process
        2 - includes '1', but outputs additionally warnings, if undefined (but not critical) situations occur
        3 - outputs all debugging stuff
    -->
    <xsl:param name="debug" select="'0'"/>
    
    <!-- 
        Is the internal variable with the value of the debug level.
      -->
    <xsl:variable name="deb">        
          <!-- test whether the user gave a stupid value -->
        <xsl:choose>
            <xsl:when test="string(number($debug)) != 'NaN' and number($debug) &gt;= 0 and number($debug) &lt; 4">
                <xsl:value-of select="$debug"/>
            </xsl:when>
            <xsl:otherwise><xsl:text>0</xsl:text></xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    
      <!-- 
        Internal variable contains the URI of the model.
      -->
    <xsl:variable name="model_uri">
        <xsl:choose>
            <xsl:when test="$namespace_uri = ''">
                <xsl:text>file:</xsl:text>
                <xsl:value-of select="$targetPath"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="$modelName"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$namespace_uri"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="$modelName"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    
      <!-- 
        Internal variable contains the URI of the instances.
      -->
    <xsl:variable name="instances_uri">
        <xsl:choose>
            <xsl:when test="$namespace_uri = ''">
                <xsl:text>file:</xsl:text>
                <xsl:value-of select="$targetPath"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="$instancesName"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$namespace_uri"/>
                <xsl:text>/</xsl:text>
                <xsl:value-of select="$instancesName"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
  
      <!-- 
        The following variables contain path information about all created documents.
      -->
    
    <xsl:variable name="pathToSchema">
        <xsl:value-of select="$targetPath"/>
        <xsl:text>/schema.xsd</xsl:text>
    </xsl:variable>
    
    <xsl:variable name="pathToOWLModel">
        <xsl:value-of select="$targetPath"/>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="str:tokenize($modelName, '/')[position() = last()]"/>
    </xsl:variable>
    
    <xsl:variable name="pathToInstancesStylesheet">
        <xsl:value-of select="$targetPath"/>
        <xsl:text>/createInstances.xsl</xsl:text>
    </xsl:variable>
    
    <xsl:variable name="pathToOWLInstances">
        <xsl:value-of select="$targetPath"/>
        <xsl:text>/</xsl:text>
        <xsl:value-of select="str:tokenize($instancesName, '/')[position() = last()]"/>
    </xsl:variable>
  
      <!-- INCLUDE SECTION -->
    
    <!-- ///////////////////////// Includes the processing for schema extracting \\\\\\\\\\\\\\\\\\\\\\\ -->
    <xsl:include href="xml2xsd.xsl"/>
    <!-- ////////////////////////////////////////////|\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ -->
    
    <!-- ////////////////////////////// Includes the generation of the model \\\\\\\\\\\\\\\\\\\\\\\\\\\ -->
    <xsl:include href="createOWLModel.xsl"/>
    <!-- ////////////////////////////////////////////|\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ -->    
    
    <!-- ////////////////////////////// Includes the generation of the stylesheet \\\\\\\\\\\\\\\\\\\\\\\\ -->
    <xsl:include href="createStylesheet.xsl"/>
    <!-- ////////////////////////////////////////////|\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ -->        
    
    <!-- 
        If the stylesheet is processing xsd-files, it tries to find out the namespaceprefix (xs, xsd, or sth. else) for xml-schema of the input document. 
        The prefix will be stored in the global variable 'xsdPrefix'. 
    
        @TODO: extend, so that every prefix can be used
    -->
    <xsl:variable name="xsdPrefix">
        <xsl:value-of select="substring-before(name(node()), local-name(node()))"/>
    </xsl:variable>    
    
    <!-- === Starting point =================================================== -->
    <!-- 
        This is the controller template, which directs the stylesheet to the right processing mode, 
        depending on the parameter 'mode'.  
    -->
    <xsl:template match="/">
        <xsl:if test="$deb &gt; 0">
            <xsl:message>DebugLevel set to <xsl:value-of select="$deb"/></xsl:message>
        </xsl:if>
        
        <!-- chooses between the 2 operational modes (xml/xsd) -->
        <xsl:choose>
            <xsl:when test="$input = 'xml'">
                <xsl:if test="$deb &gt; 0">
                    <xsl:message>Start creating Schema from instance data ... </xsl:message>
                    <xsl:message>Outputfilename will be: '<xsl:value-of select="$pathToSchema"/>'</xsl:message>
                </xsl:if>
                
                <!-- the beginning of the RDF document -->
                <exslt:document href="{$pathToSchema}" indent="yes">
                    <xsl:text disable-output-escaping="yes">&lt;xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" </xsl:text>
                    <xsl:for-each select="set:distinct(//namespace::*)">
                        <xsl:text>xmlns:</xsl:text>
                        <xsl:value-of select="name()"/>
                        <xsl:text>="</xsl:text>
                        <xsl:value-of select="."/>
                        <xsl:text>" </xsl:text>
                    </xsl:for-each>
                    <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
                    
                    <xsl:apply-templates select="//*"/>
                    
                    <xsl:text disable-output-escaping="yes">&lt;/xsd:schema&gt;</xsl:text>
                </exslt:document>                        
                
                <xsl:if test="$deb &gt; 0">
                    <xsl:message>XML Schema created</xsl:message>
                </xsl:if>
            </xsl:when>
            <xsl:when test="$input = 'xsd'">    
                <!-- 
                    Test, whether an XML Schema prefix is a specific string                    
                -->                
                <xsl:if test="$xsdPrefix != 'xs:' and $xsdPrefix != 'xsd:'">
                    <xsl:message terminate="yes">!!! ABORT: unsupported XML-Schema prefix found: '<xsl:value-of select="$xsdPrefix"/>' Please use one of these prefixes: 'xs or xsd'.</xsl:message>
                </xsl:if>
                
                <xsl:if test="$deb &gt; 0">
                    <xsl:message>Start creating OWL Model and XSL Stylesheet from XML Schema ... </xsl:message>
                </xsl:if>
                
                <xsl:variable name="owlModel">
                    <owlModel>
                        <!-- creating the RDF document with all important namespaces -->
                        <xsl:text disable-output-escaping="yes">&lt;rdf:RDF xmlns:owl="http://www.w3.org/2002/07/owl#"
                            xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                            xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" </xsl:text>
                        <xsl:for-each select="set:distinct(//namespace::*)">
                            <xsl:text>xmlns:</xsl:text>
                            <xsl:value-of select="name()"/>
                            <xsl:text>="</xsl:text>
                            <xsl:value-of select="."/>
                            <xsl:text>" </xsl:text>
                        </xsl:for-each>
                        <xsl:text>xmlns="</xsl:text>    
                        <xsl:value-of select="$model_uri"/>
                        <xsl:text>#" xml:base="</xsl:text>
                        <xsl:value-of select="$model_uri"/>
                        <xsl:text>#"</xsl:text>
                        <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
                        
                        <xsl:call-template name="createOWLModel"/>
                        
                        <xsl:text disable-output-escaping="yes">&lt;/rdf:RDF&gt;</xsl:text>
                    </owlModel>                            
                </xsl:variable>
                
                <exslt:document href="{$pathToOWLModel}" indent="yes">
                    <!-- creating needed XML entities -->
                    <xsl:text disable-output-escaping="yes">&lt;!DOCTYPE rdf:RDF [</xsl:text>
                    <xsl:for-each select="set:distinct(//namespace::*[name() != ''])">
                        <xsl:text disable-output-escaping="yes">&lt;!ENTITY </xsl:text>
                        <xsl:value-of select="name()"/>
                        <xsl:text> "</xsl:text>
                        <xsl:value-of select="."/>
                        <xsl:text>"</xsl:text>
                        <xsl:text disable-output-escaping="yes">&gt;</xsl:text>
                        <xsl:if test="$deb &gt; 0">
                            <xsl:message>Namespace found: <xsl:value-of select="name()"/>:<xsl:value-of select="."/></xsl:message>                    
                        </xsl:if>
                    </xsl:for-each>
                    <xsl:text disable-output-escaping="yes">]&gt;</xsl:text>
                    
                    <xsl:for-each select="exslt:node-set($owlModel)/*">
                        <xsl:copy-of select="node()"/>
                    </xsl:for-each>
                    
                </exslt:document>
                
                <!-- 
                    The next parameters will be included in the stylesheet for the instance data. Each of them contains 
                    a list of either all classes, ObjectProperties, DatatypeProperties. There is one special list, containing domains  for the 
                    artificial DatatypeProperty (dtpContent), which is mapped from a leaf xsd:element with literal content and 
                    attributes.                    
                -->                
                <xsl:variable name="classesParam">
                    <xsl:for-each select="exslt:node-set($owlModel)/*/owl:Class"><xsl:value-of select="@rdf:ID"/><xsl:value-of select="@rdf:about"/>|</xsl:for-each>
                </xsl:variable>
                <xsl:variable name="opParam">
                    <xsl:for-each select="exslt:node-set($owlModel)/*/owl:ObjectProperty"><xsl:value-of select="substring-after(@rdf:ID, $opPrefix)"/>|</xsl:for-each>
                </xsl:variable>
                <xsl:variable name="dtpParam">
                    <xsl:for-each select="exslt:node-set($owlModel)/*/owl:DatatypeProperty"><xsl:value-of select="substring-after(@rdf:ID, $dtpPrefix)"/><xsl:value-of select="@rdf:about"/>|</xsl:for-each>
                </xsl:variable>
                <xsl:variable name="dtpContentDomain">
                    <xsl:for-each select="exslt:node-set($owlModel)/*/owl:DatatypeProperty[@rdf:ID = 'dtpContent']/rdfs:domain//@rdf:about"><xsl:value-of select="substring-after(., '#')"/>|</xsl:for-each>
                </xsl:variable>
                
                <!-- 
                    the stylesheet for the processing of the instances will be created and configured using the
                    parameters determined above.
                -->                
                <exslt:document href="{$pathToInstancesStylesheet}" indent="yes">
                    <xsl:call-template name="createStylesheet">
                        <xsl:with-param name="classesParam" select="$classesParam"/>
                        <xsl:with-param name="opParam" select="$opParam"/>
                        <xsl:with-param name="dtpParam" select="$dtpParam"/>
                        <xsl:with-param name="dtpContentDomain" select="$dtpContentDomain"/>
                    </xsl:call-template>
                </exslt:document>
                
                <xsl:if test="$deb &gt; 0">
                    <xsl:message>OWL Model and XSL Stylesheet created ...</xsl:message>    
                </xsl:if>
            </xsl:when>        
            <xsl:otherwise>
                <xsl:message>!!! ABORT: Wrong or missing value for parameter 'input'</xsl:message>        
                <xsl:call-template name="help"/>
            </xsl:otherwise>
        </xsl:choose>
        
    </xsl:template>
    
    <!-- =============================================================== -->
    <!-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! -->
    <!-- =============================================================== -->
    
    <!-- 
        Empty text-nodes will not be copied to the result tree. 
        
        (possibly deprecated...)
    -->
    <xsl:template match="text()">
        <xsl:if test="string(.) = ''"/>
    </xsl:template>
    
    <!-- =============================================================== -->
    <!-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! -->
    <!-- =============================================================== -->
    
    <!-- 
        Template for comment-processing.
        This templates generates a rdfs:comment element. Only pass the string you want to become a comment.
    -->
    <xsl:template name="comments">
        <xsl:param name="comment"/>
        
        <xsl:element name="rdfs:comment">
            <xsl:if test="$deb &gt; 1">
                <xsl:message>Comment found and processed.</xsl:message>
            </xsl:if>
            <xsl:value-of select="normalize-space($comment)"/>
        </xsl:element>
      
    </xsl:template>    
    
    <!-- =============================================================== -->
    <!-- !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!! -->
    <!-- =============================================================== -->
    
    <!-- 
        Template that gives back a help for users. 
        This function makes only sense, if the stylesheets are called from a command line interface.
    -->
    <xsl:template name="help">
        <xsl:message> This is the help-page for xml2owl. Following options are available:
            
  input (mandatory)    - defines the input format {xsd|xml}
  namespace_uri        - defines the default namespace for the ontology (normally take the current path of the working directory)    
  modelName            - name of the OWL model (if missing it will be model.owl)
  instancesName          - name of the OWL instances document (if missing it will be instances.owl)
  importURIsForModel  - a list with '|' separated URIs for inporting
  camelCase                - toggles CamelCase functionality {true|false}
  ontologyLabel            - a string for the label field in the ontology header
  ontologyComment      - a string for the comment field in the ontology header
  targetPath                 - the path relative to this stylesheet where the result documents will be placed
  pathToXSLTSLlib        - contains the path to the external XSLTSL library
  toggleFunctionalPropertySupport  - toggles support for functional properties {true|false}
  opPrefix            - determines the prefix for owl:ObjectProperties (default value is 'has')
  dtpPrefix        - determines the prefix for owl:DatatypeProperty (default value is 'DTP')
  debug        - {0|1|2|3} - determines the verbosity level (0 = quiet, 3 = talkative)
            
        </xsl:message>
    </xsl:template>
</xsl:stylesheet>
