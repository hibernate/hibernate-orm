<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://nwalsh.com/xsl/documentation/1.0"
                xmlns:date="http://exslt.org/dates-and-times"
                exclude-result-prefixes="doc date"
                extension-element-prefixes="date"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     This file contains general templates for processing processing
     instructions common to both the HTML and FO versions of the
     DocBook stylesheets.
     ******************************************************************** -->

<!-- Process PIs also on title pages -->
<xsl:template match="processing-instruction()" mode="titlepage.mode">
  <xsl:apply-templates select="."/>
</xsl:template>

<xsl:template match="processing-instruction('dbtimestamp')">
  <xsl:variable name="format">
    <xsl:variable name="pi-format">
      <xsl:call-template name="pi-attribute">
        <xsl:with-param name="pis" select="."/>
        <xsl:with-param name="attribute">format</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$pi-format != ''">
        <xsl:value-of select="$pi-format"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'datetime'"/>
          <xsl:with-param name="name" select="'format'"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>  

  <xsl:variable name="padding">
    <xsl:variable name="pi-padding">
      <xsl:call-template name="pi-attribute">
        <xsl:with-param name="pis" select="."/>
        <xsl:with-param name="attribute">padding</xsl:with-param>
      </xsl:call-template>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="$pi-padding != ''">
        <xsl:value-of select="$pi-padding"/>
      </xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="date">
    <xsl:if test="function-available('date:date-time')">
      <xsl:value-of select="date:date-time()"/>
    </xsl:if>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="function-available('date:date-time')">
      <xsl:call-template name="datetime.format">
        <xsl:with-param name="date" select="$date"/>
        <xsl:with-param name="format" select="$format"/>
        <xsl:with-param name="padding" select="$padding"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message>
        Timestamp processing requires XSLT processor with EXSLT date support.
      </xsl:message>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>

<xsl:template name="datetime.format">
  <xsl:param name="date"/>
  <xsl:param name="format"/>
  <xsl:param name="padding" select="1"/>
  
  <xsl:if test="$format != ''">
    <xsl:variable name="char" select="substring($format,1,1)"/>

    <xsl:choose>
      <xsl:when test="$char = 'a'">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'datetime-abbrev'"/>
          <xsl:with-param name="name" select="date:day-abbreviation($date)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$char = 'A'">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'datetime-full'"/>
          <xsl:with-param name="name" select="date:day-name($date)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$char = 'b'">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'datetime-abbrev'"/>
          <xsl:with-param name="name" select="date:month-abbreviation($date)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$char = 'c'">
        <xsl:value-of select="date:date($date)"/>
        <xsl:text> </xsl:text>
        <xsl:value-of select="date:time($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'B'">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'datetime-full'"/>
          <xsl:with-param name="name" select="date:month-name($date)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$char = 'd'">
        <xsl:if test="$padding = 1 and string-length(date:day-in-month($date)) = 1">0</xsl:if>
        <xsl:value-of select="date:day-in-month($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'H'">
        <xsl:if test="$padding = 1 and string-length(date:hour-in-day($date)) = 1">0</xsl:if>
        <xsl:value-of select="date:hour-in-day($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'j'">
        <xsl:value-of select="date:day-in-year($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'm'">
        <xsl:if test="$padding = 1 and string-length(date:month-in-year($date)) = 1">0</xsl:if>
        <xsl:value-of select="date:month-in-year($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'M'">
        <xsl:if test="string-length(date:minute-in-hour($date)) = 1">0</xsl:if>
        <xsl:value-of select="date:minute-in-hour($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'S'">
        <xsl:if test="string-length(date:second-in-minute($date)) = 1">0</xsl:if>
        <xsl:value-of select="date:second-in-minute($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'U'">
        <xsl:value-of select="date:week-in-year($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'w'">
        <xsl:value-of select="date:day-in-week($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'x'">
        <xsl:value-of select="date:date($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'X'">
        <xsl:value-of select="date:time($date)"/>
      </xsl:when>
      <xsl:when test="$char = 'Y'">
        <xsl:value-of select="date:year($date)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$char"/>
      </xsl:otherwise>
    </xsl:choose>
    
    <!-- Process rest of format specifier -->
    <xsl:call-template name="datetime.format">
      <xsl:with-param name="date" select="$date"/>
      <xsl:with-param name="format" select="substring($format,2)"/>
      <xsl:with-param name="padding" select="$padding"/>
    </xsl:call-template>
  </xsl:if>

</xsl:template>

</xsl:stylesheet>
