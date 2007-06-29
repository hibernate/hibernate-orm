<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<xsl:template name="admon.graphic.width">
  <xsl:param name="node" select="."/>
  <xsl:text>25</xsl:text>
</xsl:template>

<xsl:template match="note|important|warning|caution|tip">
  <xsl:choose>
    <xsl:when test="$admon.graphics != 0">
      <xsl:call-template name="graphical.admonition"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="nongraphical.admonition"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="admon.graphic">
  <xsl:param name="node" select="."/>
  <xsl:value-of select="$admon.graphics.path"/>
  <xsl:choose>
    <xsl:when test="local-name($node)='note'">note</xsl:when>
    <xsl:when test="local-name($node)='warning'">warning</xsl:when>
    <xsl:when test="local-name($node)='caution'">caution</xsl:when>
    <xsl:when test="local-name($node)='tip'">tip</xsl:when>
    <xsl:when test="local-name($node)='important'">important</xsl:when>
    <xsl:otherwise>note</xsl:otherwise>
  </xsl:choose>
  <xsl:value-of select="$admon.graphics.extension"/>
</xsl:template>

<xsl:template name="graphical.admonition">
  <xsl:variable name="admon.type">
    <xsl:choose>
      <xsl:when test="local-name(.)='note'">Note</xsl:when>
      <xsl:when test="local-name(.)='warning'">Warning</xsl:when>
      <xsl:when test="local-name(.)='caution'">Caution</xsl:when>
      <xsl:when test="local-name(.)='tip'">Tip</xsl:when>
      <xsl:when test="local-name(.)='important'">Important</xsl:when>
      <xsl:otherwise>Note</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <div class="{name(.)}">
    <xsl:if test="$admon.style != ''">
      <xsl:attribute name="style">
        <xsl:value-of select="$admon.style"/>
      </xsl:attribute>
    </xsl:if>

    <table border="0">
      <xsl:attribute name="summary">
        <xsl:value-of select="$admon.type"/>
        <xsl:if test="title">
          <xsl:text>: </xsl:text>
          <xsl:value-of select="title"/>
        </xsl:if>
      </xsl:attribute>
      <tr>
        <td rowspan="2" align="center" valign="top">
          <xsl:attribute name="width">
            <xsl:call-template name="admon.graphic.width"/>
          </xsl:attribute>
          <img alt="[{$admon.type}]">
            <xsl:attribute name="src">
              <xsl:call-template name="admon.graphic"/>
            </xsl:attribute>
          </img>
        </td>
        <th align="left">
          <xsl:call-template name="anchor"/>
          <xsl:if test="$admon.textlabel != 0 or title">
            <xsl:apply-templates select="." mode="object.title.markup"/>
          </xsl:if>
        </th>
      </tr>
      <tr>
        <td colspan="2" align="left" valign="top">
          <xsl:apply-templates/>
        </td>
      </tr>
    </table>
  </div>
</xsl:template>

<xsl:template name="nongraphical.admonition">
  <div class="{name(.)}">
    <xsl:if test="$admon.style">
      <xsl:attribute name="style">
        <xsl:value-of select="$admon.style"/>
      </xsl:attribute>
    </xsl:if>

    <h3 class="title">
      <xsl:call-template name="anchor"/>
      <xsl:if test="$admon.textlabel != 0 or title">
        <xsl:apply-templates select="." mode="object.title.markup"/>
      </xsl:if>
    </h3>

    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="note/title"></xsl:template>
<xsl:template match="important/title"></xsl:template>
<xsl:template match="warning/title"></xsl:template>
<xsl:template match="caution/title"></xsl:template>
<xsl:template match="tip/title"></xsl:template>

</xsl:stylesheet>
