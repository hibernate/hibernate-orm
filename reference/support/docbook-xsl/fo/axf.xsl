<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:axf="http://www.antennahouse.com/names/XSL/Extensions"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ******************************************************************** -->

<xsl:template name="axf-document-information">

    <xsl:if test="//author[1]">
      <xsl:element name="axf:document-info">
        <xsl:attribute name="name">author</xsl:attribute>
        <xsl:attribute name="value">
          <xsl:call-template name="person.name">
            <xsl:with-param name="node" select="//author[1]"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

    <xsl:variable name="title">
      <xsl:apply-templates select="/*[1]" mode="label.markup"/>
      <xsl:apply-templates select="/*[1]" mode="title.markup"/>
    </xsl:variable>

    <axf:document-info name="title" value="{$title}"/>

    <xsl:if test="//keyword">
      <xsl:element name="axf:document-info">
        <xsl:attribute name="name">keywords</xsl:attribute>
        <xsl:attribute name="value">
          <xsl:for-each select="//keyword">
            <xsl:value-of select="."/>
            <xsl:if test="position() != last()">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

    <xsl:if test="//subjectterm">
      <xsl:element name="axf:document-info">
        <xsl:attribute name="name">subject</xsl:attribute>
        <xsl:attribute name="value">
          <xsl:for-each select="//subjectterm">
            <xsl:value-of select="."/>
            <xsl:if test="position() != last()">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

</xsl:template>

</xsl:stylesheet>
