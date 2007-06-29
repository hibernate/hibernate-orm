<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="1.0">
 <xsl:output method="text"/>

  <xsl:template match="sect1|sect2|sect3">
    <xsl:if test="not(@id)">
        <xsl:message>No identifier in chapter '<xsl:value-of select="ancestor::chapter/@id"/>', section '<xsl:value-of select="title"/>'
        </xsl:message>
    </xsl:if>  
      <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="*|@*|text()">
    <xsl:apply-templates/>
  </xsl:template>

</xsl:stylesheet>
