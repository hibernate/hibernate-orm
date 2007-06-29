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

<!-- ==================================================================== -->

<xsl:template match="set">
  <div class="{name(.)}">
    <xsl:call-template name="language.attribute"/>
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="set.titlepage"/>

    <xsl:call-template name="make.lots">
      <xsl:with-param name="toc.params">
        <xsl:call-template name="find.path.params">
          <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="toc">
        <xsl:call-template name="set.toc"/>
      </xsl:with-param>
    </xsl:call-template>

    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="set/setinfo"></xsl:template>
<xsl:template match="set/title"></xsl:template>
<xsl:template match="set/titleabbrev"></xsl:template>
<xsl:template match="set/subtitle"></xsl:template>

<!-- ==================================================================== -->

<xsl:template match="book">
  <div class="{name(.)}">
    <xsl:call-template name="language.attribute"/>
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="book.titlepage"/>

    <xsl:apply-templates select="dedication" mode="dedication"/>

    <xsl:call-template name="make.lots">
      <xsl:with-param name="toc.params">
        <xsl:call-template name="find.path.params">
          <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
        </xsl:call-template>
      </xsl:with-param>
      <xsl:with-param name="toc">
        <xsl:call-template name="division.toc"/>
      </xsl:with-param>
    </xsl:call-template>

    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="book/bookinfo"></xsl:template>
<xsl:template match="book/title"></xsl:template>
<xsl:template match="book/titleabbrev"></xsl:template>
<xsl:template match="book/subtitle"></xsl:template>

<!-- ==================================================================== -->

<xsl:template match="part">
  <div class="{name(.)}">
    <xsl:call-template name="language.attribute"/>
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="part.titlepage"/>

    <xsl:variable name="toc.params">
      <xsl:call-template name="find.path.params">
        <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="not(partintro) and contains($toc.params, 'toc')">
      <xsl:call-template name="division.toc"/>
    </xsl:if>
    <xsl:apply-templates/>
  </div>
</xsl:template>

<xsl:template match="part" mode="make.part.toc">
  <xsl:call-template name="division.toc"/>
</xsl:template>

<xsl:template match="reference" mode="make.part.toc">
  <xsl:call-template name="division.toc"/>
</xsl:template>

<xsl:template match="part/docinfo"></xsl:template>
<xsl:template match="part/partinfo"></xsl:template>
<xsl:template match="part/title"></xsl:template>
<xsl:template match="part/titleabbrev"></xsl:template>
<xsl:template match="part/subtitle"></xsl:template>

<xsl:template match="partintro">
  <div class="{name(.)}">
    <xsl:call-template name="language.attribute"/>
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="partintro.titlepage"/>
    <xsl:apply-templates/>

    <xsl:variable name="toc.params">
      <xsl:call-template name="find.path.params">
        <xsl:with-param name="node" select="parent::*"/>
        <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
      </xsl:call-template>
    </xsl:variable>
    <xsl:if test="contains($toc.params, 'toc')">
      <!-- not ancestor::part because partintro appears in reference -->
      <xsl:apply-templates select="parent::*" mode="make.part.toc"/>
    </xsl:if>
    <xsl:call-template name="process.footnotes"/>
  </div>
</xsl:template>

<xsl:template match="partintro/title"></xsl:template>
<xsl:template match="partintro/titleabbrev"></xsl:template>
<xsl:template match="partintro/subtitle"></xsl:template>

<xsl:template match="partintro/title" mode="partintro.title.mode">
  <h2>
    <xsl:apply-templates/>
  </h2>
</xsl:template>

<xsl:template match="partintro/subtitle" mode="partintro.title.mode">
  <h3>
    <i><xsl:apply-templates/></i>
  </h3>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="book" mode="division.number">
  <xsl:number from="set" count="book" format="1."/>
</xsl:template>

<xsl:template match="part" mode="division.number">
  <xsl:number from="book" count="part" format="I."/>
</xsl:template>

</xsl:stylesheet>

