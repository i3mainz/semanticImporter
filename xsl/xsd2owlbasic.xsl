<?xml version='1.0' encoding='utf-8'?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsd="http://www.w3.org/2001/XMLSchema"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:owl="http://www.w3.org/2002/07/owl#">

    <xsl:output method="xml" encoding="utf-8" indent="yes"/>

    <xsl:template match="//xsd:schema">
      <rdf:RDF>
        <xsl:for-each select=".//xsd:element">
          <xsl:apply-templates select="."/>
        </xsl:for-each>
      </rdf:RDF>
    </xsl:template>

    <xsl:template match="xsd:element">
      <xsl:if test="./@name">
        <xsl:element name="owl:Class">
          <xsl:attribute name="rdf:about">
            <xsl:value-of select="./@name"/>
          </xsl:attribute>
        </xsl:element>
        <xsl:for-each select=".//xsd:element">
          <xsl:element name="owl:ObjectProperty">
            <xsl:attribute name="rdf:about">
              has<xsl:value-of     select="./@ref"/>
            </xsl:attribute>
            <xsl:element name="rdf:Range">
              <xsl:attribute name="rdf:resource">
                 <xsl:value-of select="./@ref"/>
              </xsl:attribute>
            </xsl:element>
          </xsl:element>
        </xsl:for-each>
      </xsl:if>
    </xsl:template>

</xsl:stylesheet>