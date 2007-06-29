<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:template match="qandaset">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>

  <xsl:variable name="label-width">
    <xsl:call-template name="dbfo-attribute">
      <xsl:with-param name="pis"
                      select="processing-instruction('dbfo')"/>
      <xsl:with-param name="attribute" select="'label-width'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="label-length">
    <xsl:choose>
      <xsl:when test="$label-width != ''">
        <xsl:value-of select="$label-width"/>
      </xsl:when>
      <xsl:when test="descendant::label">
        <xsl:call-template name="longest.term">
          <xsl:with-param name="terms" select="descendant::label"/>
          <xsl:with-param name="maxlength" select="20"/>
        </xsl:call-template>
        <xsl:text>em * 0.50</xsl:text>
      </xsl:when>
      <xsl:otherwise>2.5em</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:if test="blockinfo/title|info/title|title">
      <xsl:apply-templates select="(blockinfo/title|info/title|title)[1]"/>
    </xsl:if>

    <xsl:apply-templates select="*[name(.) != 'title'
                                 and name(.) != 'titleabbrev'
                                 and name(.) != 'qandadiv'
                                 and name(.) != 'qandaentry']"/>
    <xsl:apply-templates select="qandadiv"/>

    <xsl:if test="qandaentry">
      <fo:list-block xsl:use-attribute-sets="list.block.spacing"
                     provisional-label-separation="0.2em">
	<xsl:attribute name="provisional-distance-between-starts">
	  <xsl:choose>
	    <xsl:when test="$label-length != ''">
	      <xsl:value-of select="$label-length"/>
	    </xsl:when>
	    <xsl:otherwise>2.5em</xsl:otherwise>
	  </xsl:choose>
	</xsl:attribute>
        <xsl:apply-templates select="qandaentry"/>
      </fo:list-block>
    </xsl:if>
  </fo:block>
</xsl:template>

<xsl:template match="qandaset/blockinfo/title|qandset/info/title|qandaset/title">
  <xsl:variable name="enclsect" select="(ancestor::section
                                        | ancestor::simplesect
                                        | ancestor::sect5
                                        | ancestor::sect4
                                        | ancestor::sect3
                                        | ancestor::sect2
                                        | ancestor::sect1
                                        | ancestor::refsect3
                                        | ancestor::refsect2
                                        | ancestor::refsect1)[last()]"/>
  <xsl:variable name="sectlvl">
    <xsl:call-template name="section.level">
      <xsl:with-param name="node" select="$enclsect"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="qanda.heading">
    <xsl:with-param name="level" select="$sectlvl + 1"/>
    <xsl:with-param name="marker" select="0"/>
    <xsl:with-param name="title">
      <xsl:apply-templates/>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template match="qandaset/blockinfo">
  <!-- what should this template really do? -->
  <xsl:apply-templates select="legalnotice" mode="titlepage.mode"/>
</xsl:template>

<xsl:template match="qandadiv">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>

  <xsl:variable name="label-width">
    <xsl:call-template name="dbfo-attribute">
      <xsl:with-param name="pis"
                      select="processing-instruction('dbfo')"/>
      <xsl:with-param name="attribute" select="'label-width'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="label-length">
    <xsl:choose>
      <xsl:when test="$label-width != ''">
        <xsl:value-of select="$label-width"/>
      </xsl:when>
      <xsl:when test="descendant::label">
        <xsl:call-template name="longest.term">
          <xsl:with-param name="terms" select="descendant::label"/>
          <xsl:with-param name="maxlength" select="20"/>
        </xsl:call-template>
        <xsl:text>*0.6em</xsl:text>
      </xsl:when>
      <xsl:otherwise>2.5em</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:block id="{$id}">
    <xsl:apply-templates select="(blockinfo/title|info/title|title)[1]"/>
    <xsl:apply-templates select="*[name(.) != 'title'
                                 and name(.) != 'titleabbrev'
                                 and name(.) != 'qandadiv'
                                 and name(.) != 'qandaentry']"/>
    <fo:block start-indent="{count(ancestor::qandadiv)*2}pc">
      <xsl:apply-templates select="qandadiv"/>

      <xsl:if test="qandaentry">
        <fo:list-block xsl:use-attribute-sets="list.block.spacing"
                       provisional-label-separation="0.2em">
	  <xsl:attribute name="provisional-distance-between-starts">
	    <xsl:choose>
	      <xsl:when test="$label-length != ''">
	        <xsl:value-of select="$label-length"/>
	      </xsl:when>
	      <xsl:otherwise>2.5em</xsl:otherwise>
	    </xsl:choose>
	  </xsl:attribute>
          <xsl:apply-templates select="qandaentry"/>
        </fo:list-block>
      </xsl:if>
    </fo:block>
  </fo:block>
</xsl:template>

<xsl:template match="qandadiv/blockinfo/title|qandadiv/info/title|qandadiv/title">
  <xsl:variable name="enclsect" select="(ancestor::section
                                        | ancestor::simplesect
                                        | ancestor::sect5
                                        | ancestor::sect4
                                        | ancestor::sect3
                                        | ancestor::sect2
                                        | ancestor::sect1
                                        | ancestor::refsect3
                                        | ancestor::refsect2
                                        | ancestor::refsect1)[last()]"/>
  <xsl:variable name="sectlvl">
    <xsl:call-template name="section.level">
      <xsl:with-param name="node" select="$enclsect"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="qanda.heading">
    <xsl:with-param name="level"  select="$sectlvl + 1 + count(ancestor::qandadiv)"/>
    <xsl:with-param name="marker" select="0"/>
    <xsl:with-param name="title">
      <xsl:apply-templates select="parent::qandadiv" mode="label.markup"/>
      <xsl:if test="$qandadiv.autolabel != 0">
        <xsl:apply-templates select="." mode="intralabel.punctuation"/>
	<xsl:text> </xsl:text>
      </xsl:if>
      <xsl:apply-templates/>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template match="qandaentry">
  <xsl:apply-templates/>
<!--
  <fo:block>
    <xsl:if test="@id">
      <xsl:attribute name="id"><xsl:value-of select="@id"/></xsl:attribute>
    </xsl:if>
    <xsl:apply-templates/>
  </fo:block>
-->
</xsl:template>

<xsl:template match="question">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>

  <xsl:variable name="entry.id">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="parent::*"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="deflabel">
    <xsl:choose>
      <xsl:when test="ancestor-or-self::*[@defaultlabel]">
        <xsl:value-of select="(ancestor-or-self::*[@defaultlabel])[last()]
                              /@defaultlabel"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$qanda.defaultlabel"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:list-item id="{$entry.id}" xsl:use-attribute-sets="list.item.spacing">
    <fo:list-item-label id="{$id}" end-indent="label-end()">
      <xsl:choose>
        <xsl:when test="$deflabel = 'none'">
          <fo:block/>
        </xsl:when>
        <xsl:otherwise>
          <fo:block>
            <xsl:apply-templates select="." mode="label.markup"/>
	    <xsl:if test="$deflabel = 'number' and not(label)">
              <xsl:apply-templates select="." mode="intralabel.punctuation"/>
	    </xsl:if>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </fo:list-item-label>
    <fo:list-item-body start-indent="body-start()">
      <xsl:choose>
        <xsl:when test="$deflabel = 'none'">
          <fo:block font-weight="bold">
            <xsl:apply-templates select="*[local-name(.)!='label']"/>
          </fo:block>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="*[local-name(.)!='label']"/>
        </xsl:otherwise>
      </xsl:choose>
    </fo:list-item-body>
  </fo:list-item>
</xsl:template>

<xsl:template match="answer">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <xsl:variable name="entry.id">
    <xsl:call-template name="object.id">
      <xsl:with-param name="object" select="parent::*"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="deflabel">
    <xsl:choose>
      <xsl:when test="ancestor-or-self::*[@defaultlabel]">
        <xsl:value-of select="(ancestor-or-self::*[@defaultlabel])[last()]
                              /@defaultlabel"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$qanda.defaultlabel"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:list-item xsl:use-attribute-sets="list.item.spacing">
    <fo:list-item-label id="{$id}" end-indent="label-end()">
      <xsl:choose>
        <xsl:when test="$deflabel = 'none'">
          <fo:block/>
        </xsl:when>
        <xsl:otherwise>
          <fo:block>
            <xsl:variable name="answer.label">
              <xsl:apply-templates select="." mode="label.markup"/>
            </xsl:variable>
            <xsl:copy-of select="$answer.label"/>
          </fo:block>
        </xsl:otherwise>
      </xsl:choose>
    </fo:list-item-label>
    <fo:list-item-body start-indent="body-start()">
      <xsl:apply-templates select="*[local-name(.)!='label']"/>
    </fo:list-item-body>
  </fo:list-item>
</xsl:template>

<xsl:template match="label">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template name="qanda.heading">
  <xsl:param name="level" select="1"/>
  <xsl:param name="marker" select="0"/>
  <xsl:param name="title"/>
  <xsl:param name="titleabbrev"/>

  <fo:block xsl:use-attribute-sets="qanda.title.properties">
    <xsl:if test="$marker != 0">
      <fo:marker marker-class-name="section.head.marker">
        <xsl:choose>
          <xsl:when test="$titleabbrev = ''">
            <xsl:value-of select="$title"/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$titleabbrev"/>
          </xsl:otherwise>
        </xsl:choose>
      </fo:marker>
    </xsl:if>
    <xsl:choose>
      <xsl:when test="$level=1">
        <fo:block xsl:use-attribute-sets="qanda.title.level1.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=2">
        <fo:block xsl:use-attribute-sets="qanda.title.level2.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=3">
        <fo:block xsl:use-attribute-sets="qanda.title.level3.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=4">
        <fo:block xsl:use-attribute-sets="qanda.title.level4.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:when test="$level=5">
        <fo:block xsl:use-attribute-sets="qanda.title.level5.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <fo:block xsl:use-attribute-sets="qanda.title.level6.properties">
          <xsl:copy-of select="$title"/>
        </fo:block>
      </xsl:otherwise>
    </xsl:choose>
  </fo:block>
</xsl:template>

</xsl:stylesheet>
