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

<xsl:template name="set.toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="nodes" select="book|setindex"/>

  <xsl:if test="$nodes">
    <fo:block id="toc...{$id}"
              xsl:use-attribute-sets="toc.margin.properties">
      <xsl:if test="$axf.extensions != 0">
        <xsl:attribute name="axf:outline-level">1</xsl:attribute>
        <xsl:attribute name="axf:outline-expand">false</xsl:attribute>
        <xsl:attribute name="axf:outline-title">
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'TableofContents'"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:call-template name="table.of.contents.titlepage"/>
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template name="division.toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="nodes"
                select="$toc-context/part
                        |$toc-context/reference
                        |$toc-context/preface
                        |$toc-context/chapter
                        |$toc-context/appendix
                        |$toc-context/article
                        |$toc-context/bibliography
                        |$toc-context/glossary
                        |$toc-context/index"/>

  <xsl:if test="$nodes">
    <fo:block id="toc...{$cid}"
              xsl:use-attribute-sets="toc.margin.properties">
      <xsl:if test="$axf.extensions != 0">
        <xsl:attribute name="axf:outline-level">1</xsl:attribute>
        <xsl:attribute name="axf:outline-expand">false</xsl:attribute>
        <xsl:attribute name="axf:outline-title">
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'TableofContents'"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:if>
      <xsl:call-template name="table.of.contents.titlepage"/>
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template name="component.toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="nodes" select="section|sect1|refentry
                                     |article|bibliography|glossary
                                     |appendix"/>
  <xsl:if test="$nodes">
    <fo:block id="toc...{$id}"
              xsl:use-attribute-sets="toc.margin.properties">
      <xsl:call-template name="table.of.contents.titlepage"/>
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template name="component.toc.separator">
  <!-- Customize to output something between
       component.toc and first output -->
</xsl:template>

<xsl:template name="section.toc">
  <xsl:param name="toc-context" select="."/>
  <xsl:param name="toc.title.p" select="true()"/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="nodes"
                select="section|sect1|sect2|sect3|sect4|sect5|refentry
                        |bridgehead[$bridgehead.in.toc != 0]"/>

  <xsl:variable name="level">
    <xsl:call-template name="section.level"/>
  </xsl:variable>

  <xsl:if test="$nodes">
    <fo:block id="toc...{$id}"
              xsl:use-attribute-sets="toc.margin.properties">

      <xsl:if test="$toc.title.p">
        <xsl:call-template name="section.heading">
          <xsl:with-param name="level" select="$level + 1"/>
          <xsl:with-param name="title">
            <fo:block space-after="0.5em">
              <xsl:call-template name="gentext">
                <xsl:with-param name="key" select="'TableofContents'"/>
              </xsl:call-template>
            </fo:block>
          </xsl:with-param>
        </xsl:call-template>
      </xsl:if>

      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template name="section.toc.separator">
  <!-- Customize to output something between
       section.toc and first output -->
</xsl:template>
<!-- ==================================================================== -->

<xsl:template name="toc.line">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="label">
    <xsl:apply-templates select="." mode="label.markup"/>
  </xsl:variable>

  <fo:block text-align-last="justify"
            end-indent="{$toc.indent.width}pt"
            last-line-end-indent="-{$toc.indent.width}pt">
    <fo:inline keep-with-next.within-line="always">
      <fo:basic-link internal-destination="{$id}">
        <xsl:if test="$label != ''">
          <xsl:copy-of select="$label"/>
          <xsl:value-of select="$autotoc.label.separator"/>
        </xsl:if>
        <xsl:apply-templates select="." mode="titleabbrev.markup"/>
      </fo:basic-link>
    </fo:inline>
    <fo:inline keep-together.within-line="always">
      <xsl:text> </xsl:text>
      <fo:leader leader-pattern="dots"
                 leader-pattern-width="3pt"
                 leader-alignment="reference-area"
                 keep-with-next.within-line="always"/>
      <xsl:text> </xsl:text> 
      <fo:basic-link internal-destination="{$id}">
        <fo:page-number-citation ref-id="{$id}"/>
      </fo:basic-link>
    </fo:inline>
  </fo:block>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="book|setindex" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:variable name="nodes" select="glossary|bibliography|preface|chapter
                                     |reference|part|article|appendix|index"/>

  <xsl:if test="$toc.section.depth &gt; 0 and $nodes">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{count(ancestor::*)*$toc.indent.width}pt">
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="part" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:variable name="nodes" select="chapter|appendix|preface|reference"/>

  <xsl:if test="$toc.section.depth &gt; 0 and $nodes">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{count(ancestor::*)*$toc.indent.width}pt">
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="reference" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:if test="$toc.section.depth &gt; 0 and refentry">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{count(ancestor::*)*$toc.indent.width}pt">
      <xsl:apply-templates select="refentry" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="refentry" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:call-template name="toc.line"/>
</xsl:template>

<xsl:template match="preface|chapter|appendix|article"
              mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:variable name="nodes" select="section|sect1"/>

  <xsl:if test="$toc.section.depth &gt; 0 and $nodes">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{count(ancestor::*)*$toc.indent.width}pt">
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="sect1" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:if test="$toc.section.depth &gt; 1 and sect2">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{count(ancestor::*)*$toc.indent.width}pt">
      <xsl:apply-templates select="sect2" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="sect2" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:variable name="reldepth"
                select="count(ancestor::*)-count($toc-context/ancestor::*)"/>

  <xsl:if test="$toc.section.depth &gt; 2 and sect3">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{$reldepth*$toc.indent.width}pt">
      <xsl:apply-templates select="sect3" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="sect3" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:variable name="reldepth"
                select="count(ancestor::*)-count($toc-context/ancestor::*)"/>

  <xsl:if test="$toc.section.depth &gt; 3 and sect4">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{$reldepth*$toc.indent.width}pt">
      <xsl:apply-templates select="sect4" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="sect4" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="toc.line"/>

  <xsl:variable name="reldepth"
                select="count(ancestor::*)-count($toc-context/ancestor::*)"/>

  <xsl:if test="$toc.section.depth &gt; 4 and sect5">
    <fo:block id="toc.{$cid}.{$id}"
              start-indent="{$reldepth*$toc.indent.width}pt">
      <xsl:apply-templates select="sect5" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="sect5" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:call-template name="toc.line"/>
</xsl:template>

<xsl:template match="section" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="cid">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="$toc-context"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="depth" select="count(ancestor::section) + 1"/>
  <xsl:variable name="reldepth"
                select="count(ancestor::*)-count($toc-context/ancestor::*)"/>

  <xsl:if test="$toc.section.depth &gt;= $depth">
    <xsl:call-template name="toc.line"/>

    <xsl:if test="$toc.section.depth &gt; $depth and section">
      <fo:block id="toc.{$cid}.{$id}"
                start-indent="{$reldepth*$toc.indent.width}pt">
        <xsl:apply-templates select="section" mode="toc">
          <xsl:with-param name="toc-context" select="$toc-context"/>
        </xsl:apply-templates>
      </fo:block>
    </xsl:if>
  </xsl:if>
</xsl:template>

<xsl:template match="bibliography|glossary"
              mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:call-template name="toc.line"/>
</xsl:template>

<xsl:template match="index" mode="toc">
  <xsl:param name="toc-context" select="."/>

  <xsl:if test="* or $generate.index != 0">
    <xsl:call-template name="toc.line"/>
  </xsl:if>
</xsl:template>

<xsl:template match="title" mode="toc">
  <xsl:apply-templates/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="list.of.titles">
  <xsl:param name="titles" select="'table'"/>
  <xsl:param name="nodes" select=".//table"/>
  <xsl:param name="toc-context" select="."/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:if test="$nodes">
    <fo:block id="lot...{$titles}...{$id}">
      <xsl:choose>
        <xsl:when test="$titles='table'">
          <xsl:call-template name="list.of.tables.titlepage"/>
        </xsl:when>
        <xsl:when test="$titles='figure'">
          <xsl:call-template name="list.of.figures.titlepage"/>
        </xsl:when>
        <xsl:when test="$titles='equation'">
          <xsl:call-template name="list.of.equations.titlepage"/>
        </xsl:when>
        <xsl:when test="$titles='example'">
          <xsl:call-template name="list.of.examples.titlepage"/>
        </xsl:when>
        <xsl:when test="$titles='procedure'">
          <xsl:call-template name="list.of.procedures.titlepage"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="list.of.unknowns.titlepage"/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="$nodes" mode="toc">
        <xsl:with-param name="toc-context" select="$toc-context"/>
      </xsl:apply-templates>
    </fo:block>
  </xsl:if>
</xsl:template>

<xsl:template match="figure|table|example|equation|procedure" mode="toc">
  <xsl:call-template name="toc.line"/>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>

