<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<!-- ********************************************************************
     $Id: htmltbl.xsl 3890 2004-06-03 16:31:32Z steveebersole $
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:template match="colgroup" mode="htmlTable">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="htmlTable"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="col" mode="htmlTable">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="caption" mode="htmlTable">
  <xsl:copy>
    <xsl:copy-of select="@*"/>

    <xsl:apply-templates select=".." mode="object.title.markup">
      <xsl:with-param name="allow-anchors" select="1"/>
    </xsl:apply-templates>

  </xsl:copy>
</xsl:template>

<xsl:template match="thead|tbody|tgroup|tr" mode="htmlTable">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates mode="htmlTable"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="th|td" mode="htmlTable">
  <xsl:copy>
    <xsl:copy-of select="@*"/>
    <xsl:apply-templates/> <!-- *not* mode=htmlTable -->
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
