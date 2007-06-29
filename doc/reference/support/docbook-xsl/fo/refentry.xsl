<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:axf="http://www.antennahouse.com/names/XSL/Extensions"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:template match="reference">
   <xsl:if test="not(partintro)">
    <xsl:variable name="id">
      <xsl:call-template name="object.id"/>
    </xsl:variable>
    <xsl:variable name="master-reference">
      <xsl:call-template name="select.pagemaster"/>
    </xsl:variable>

    <fo:page-sequence hyphenate="{$hyphenate}"
                      master-reference="{$master-reference}">
      <xsl:attribute name="language">
        <xsl:call-template name="l10n.language"/>
      </xsl:attribute>
      <xsl:attribute name="format">
        <xsl:call-template name="page.number.format"/>
      </xsl:attribute>
      <xsl:if test="$double.sided != 0">
        <xsl:attribute name="initial-page-number">auto-odd</xsl:attribute>
      </xsl:if>

      <xsl:attribute name="hyphenation-character">
        <xsl:call-template name="gentext">
          <xsl:with-param name="key" select="'hyphenation-character'"/>
        </xsl:call-template>
      </xsl:attribute>
      <xsl:attribute name="hyphenation-push-character-count">
        <xsl:call-template name="gentext">
          <xsl:with-param name="key" select="'hyphenation-push-character-count'"/>
        </xsl:call-template>
      </xsl:attribute>
      <xsl:attribute name="hyphenation-remain-character-count">
        <xsl:call-template name="gentext">
          <xsl:with-param name="key" select="'hyphenation-remain-character-count'"/>
        </xsl:call-template>
      </xsl:attribute>

      <xsl:apply-templates select="." mode="running.head.mode">
        <xsl:with-param name="master-reference" select="$master-reference"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="." mode="running.foot.mode">
        <xsl:with-param name="master-reference" select="$master-reference"/>
      </xsl:apply-templates>

      <fo:flow flow-name="xsl-region-body">
        <fo:block id="{$id}">
          <xsl:call-template name="reference.titlepage"/>
        </fo:block>
      </fo:flow>
    </fo:page-sequence>
  </xsl:if>
  <xsl:apply-templates select="partintro|refentry"/>
</xsl:template>

<xsl:template match="reference" mode="reference.titlepage.mode">
  <xsl:call-template name="reference.titlepage"/>
</xsl:template>

<xsl:template match="reference/partintro">
  <xsl:variable name="id">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="ancestor::reference"/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="master-reference">
    <xsl:call-template name="select.pagemaster"/>
  </xsl:variable>

  <fo:page-sequence hyphenate="{$hyphenate}"
                    master-reference="{$master-reference}">
    <xsl:attribute name="language">
      <xsl:call-template name="l10n.language"/>
    </xsl:attribute>
    <xsl:attribute name="format">
      <xsl:call-template name="page.number.format"/>
    </xsl:attribute>
    <xsl:if test="$double.sided != 0">
      <xsl:attribute name="initial-page-number">auto-odd</xsl:attribute>
    </xsl:if>

    <xsl:attribute name="hyphenation-character">
      <xsl:call-template name="gentext">
        <xsl:with-param name="key" select="'hyphenation-character'"/>
      </xsl:call-template>
    </xsl:attribute>
    <xsl:attribute name="hyphenation-push-character-count">
      <xsl:call-template name="gentext">
        <xsl:with-param name="key" select="'hyphenation-push-character-count'"/>
      </xsl:call-template>
    </xsl:attribute>
    <xsl:attribute name="hyphenation-remain-character-count">
      <xsl:call-template name="gentext">
        <xsl:with-param name="key" select="'hyphenation-remain-character-count'"/>
      </xsl:call-template>
    </xsl:attribute>

    <xsl:apply-templates select="." mode="running.head.mode">
      <xsl:with-param name="master-reference" select="$master-reference"/>
    </xsl:apply-templates>
    <xsl:apply-templates select="." mode="running.foot.mode">
      <xsl:with-param name="master-reference" select="$master-reference"/>
    </xsl:apply-templates>

    <fo:flow flow-name="xsl-region-body">
      <xsl:apply-templates select=".." mode="reference.titlepage.mode"/>
      <xsl:if test="title">
        <fo:block id="{$id}">
          <xsl:call-template name="partintro.titlepage"/>
        </fo:block>
      </xsl:if>
      <xsl:apply-templates/>
    </fo:flow>
  </fo:page-sequence>
</xsl:template>

<xsl:template match="reference/docinfo|refentry/refentryinfo"></xsl:template>
<xsl:template match="reference/title"></xsl:template>
<xsl:template match="reference/subtitle"></xsl:template>

<!-- ==================================================================== -->

<xsl:template match="refentry">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="master-reference">
    <xsl:call-template name="select.pagemaster"/>
  </xsl:variable>

  <xsl:variable name="refentry.content">
    <fo:block id="{$id}">
      <xsl:apply-templates/>
    </fo:block>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="not(parent::*) or 
                    parent::reference or 
                    parent::part">
      <!-- make a page sequence -->
      <fo:page-sequence hyphenate="{$hyphenate}"
                        master-reference="{$master-reference}">
        <xsl:attribute name="language">
          <xsl:call-template name="l10n.language"/>
        </xsl:attribute>
        <xsl:attribute name="format">
          <xsl:call-template name="page.number.format"/>
        </xsl:attribute>
        <xsl:if test="$double.sided != 0">
          <xsl:attribute name="initial-page-number">auto-odd</xsl:attribute>
        </xsl:if>

        <xsl:attribute name="hyphenation-character">
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'hyphenation-character'"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:attribute name="hyphenation-push-character-count">
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'hyphenation-push-character-count'"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:attribute name="hyphenation-remain-character-count">
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'hyphenation-remain-character-count'"/>
          </xsl:call-template>
        </xsl:attribute>

        <xsl:apply-templates select="." mode="running.head.mode">
          <xsl:with-param name="master-reference" select="$master-reference"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="." mode="running.foot.mode">
          <xsl:with-param name="master-reference" select="$master-reference"/>
        </xsl:apply-templates>

        <fo:flow flow-name="xsl-region-body">
          <xsl:copy-of select="$refentry.content"/>
        </fo:flow>
      </fo:page-sequence>
    </xsl:when>
    <xsl:otherwise>
      <fo:block>
        <xsl:if test="$refentry.pagebreak != 0">
          <xsl:attribute name="break-before">page</xsl:attribute>
        </xsl:if>
        <xsl:copy-of select="$refentry.content"/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="refmeta">
</xsl:template>

<xsl:template match="manvolnum">
  <xsl:if test="$refentry.xref.manvolnum != 0">
    <xsl:text>(</xsl:text>
    <xsl:apply-templates/>
    <xsl:text>)</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="refmiscinfo">
</xsl:template>

<xsl:template match="refentrytitle">
  <xsl:call-template name="inline.charseq"/>
</xsl:template>

<xsl:template match="refnamediv">
  <xsl:variable name="section.level">
    <xsl:call-template name="refentry.level">
      <xsl:with-param name="node" select="ancestor::refentry"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="reftitle">
    <xsl:choose>
      <xsl:when test="$refentry.generate.name != 0">
        <xsl:call-template name="gentext">
          <xsl:with-param name="key" select="'RefName'"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="$refentry.generate.title != 0">
        <xsl:choose>
          <xsl:when test="../refmeta/refentrytitle">
            <xsl:apply-templates select="../refmeta/refentrytitle"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="refname[1]"/>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
    </xsl:choose>
  </xsl:variable>

  <!-- xsl:use-attribute-sets takes only a Qname, not a variable -->
  <fo:block>
    <xsl:choose>
      <xsl:when test="$section.level = 1">
        <fo:block xsl:use-attribute-sets="refentry.title.properties">
          <fo:block xsl:use-attribute-sets="section.title.level1.properties">
            <xsl:value-of select="$reftitle"/>
          </fo:block>
        </fo:block>
      </xsl:when>
      <xsl:when test="$section.level = 2">
        <fo:block xsl:use-attribute-sets="refentry.title.properties">
          <fo:block xsl:use-attribute-sets="section.title.level2.properties">
            <xsl:value-of select="$reftitle"/>
          </fo:block>
        </fo:block>
      </xsl:when>
      <xsl:when test="$section.level = 3">
        <fo:block xsl:use-attribute-sets="refentry.title.properties">
          <fo:block xsl:use-attribute-sets="section.title.level3.properties">
            <xsl:value-of select="$reftitle"/>
          </fo:block>
        </fo:block>
      </xsl:when>
      <xsl:when test="$section.level = 4">
        <fo:block xsl:use-attribute-sets="refentry.title.properties">
          <fo:block xsl:use-attribute-sets="section.title.level4.properties">
            <xsl:value-of select="$reftitle"/>
          </fo:block>
        </fo:block>
      </xsl:when>
      <xsl:when test="$section.level = 5">
        <fo:block xsl:use-attribute-sets="refentry.title.properties">
          <fo:block xsl:use-attribute-sets="section.title.level5.properties">
            <xsl:value-of select="$reftitle"/>
          </fo:block>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <fo:block xsl:use-attribute-sets="refentry.title.properties">
          <fo:block xsl:use-attribute-sets="section.title.level6.properties">
            <xsl:value-of select="$reftitle"/>
          </fo:block>
        </fo:block>
      </xsl:otherwise>
    </xsl:choose>

    <fo:block space-after="1em">
      <xsl:choose>
        <xsl:when test="../refmeta/refentrytitle">
          <xsl:apply-templates select="../refmeta/refentrytitle"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="refname[1]"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="refpurpose"/>
    </fo:block>

    <fo:block>
      <xsl:for-each select="refname">
        <xsl:apply-templates select="."/>
        <xsl:if test="following-sibling::refname">
          <xsl:text>, </xsl:text>
        </xsl:if>
      </xsl:for-each>
    </fo:block>
  </fo:block>
</xsl:template>


<xsl:template match="refname">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="refpurpose">
  <xsl:text> </xsl:text>
  <xsl:call-template name="dingbat">
    <xsl:with-param name="dingbat">em-dash</xsl:with-param>
  </xsl:call-template>
  <xsl:text> </xsl:text>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="refdescriptor">
  <!-- todo: finish this -->
</xsl:template>

<xsl:template match="refclass">
  <fo:block font-weight="bold">
    <xsl:if test="@role">
      <xsl:value-of select="@role"/>
      <xsl:text>: </xsl:text>
    </xsl:if>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="refsynopsisdiv">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:call-template name="refsynopsisdiv.titlepage"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="refsection">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:call-template name="refsection.titlepage"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="refsect1">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:call-template name="refsect1.titlepage"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="refsect2">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:call-template name="refsect2.titlepage"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="refsect3">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:call-template name="refsect3.titlepage"/>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="refsynopsisdiv/title
                     |refsection/title
                     |refsect1/title
                     |refsect2/title
                     |refsect3/title">
  <!-- nop; titlepage.mode instead -->
</xsl:template>

<xsl:template match="refsynopsisdiv/title
                     |refsection/title
                     |refsect1/title
                     |refsect2/title
                     |refsect3/title"
              mode="titlepage.mode"
              priority="2">
  <xsl:variable name="section" select="parent::*"/>
  <fo:block keep-with-next.within-column="always">
    <xsl:variable name="id">
      <xsl:call-template name="object.id">
        <xsl:with-param name="object" select="$section"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="level">
      <xsl:call-template name="section.level">
        <xsl:with-param name="node" select="$section"/>
      </xsl:call-template>
    </xsl:variable>

    <xsl:variable name="title">
      <xsl:apply-templates select="$section" mode="object.title.markup">
        <xsl:with-param name="allow-anchors" select="1"/>
      </xsl:apply-templates>
    </xsl:variable>

    <xsl:if test="$passivetex.extensions != 0">
      <fotex:bookmark xmlns:fotex="http://www.tug.org/fotex" 
                      fotex-bookmark-level="{$level + 2}" 
                      fotex-bookmark-label="{$id}">
        <xsl:value-of select="$title"/>
      </fotex:bookmark>
    </xsl:if>

    <xsl:if test="$axf.extensions != 0">
      <xsl:attribute name="axf:outline-level">
        <xsl:value-of select="count(ancestor::*)-1"/>
      </xsl:attribute>
      <xsl:attribute name="axf:outline-expand">false</xsl:attribute>
      <xsl:attribute name="axf:outline-title">
        <xsl:value-of select="$title"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="section.heading">
      <xsl:with-param name="level" select="$level"/>
      <xsl:with-param name="title" select="$title"/>
    </xsl:call-template>
  </fo:block>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
