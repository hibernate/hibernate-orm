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

<xsl:template match="index">
  <!-- some implementations use completely empty index tags to indicate -->
  <!-- where an automatically generated index should be inserted. so -->
  <!-- if the index is completely empty, skip it. Unless generate.index -->
  <!-- is non-zero, in which case, this is where the automatically -->
  <!-- generated index should go. -->

  <xsl:if test="count(*)>0 or $generate.index != '0'">
    <div class="{name(.)}">
      <xsl:if test="$generate.id.attributes != 0">
        <xsl:attribute name="id">
          <xsl:call-template name="object.id"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:call-template name="index.titlepage"/>
      <xsl:apply-templates/>

      <xsl:if test="count(indexentry) = 0 and count(indexdiv) = 0">
        <xsl:call-template name="generate-index">
          <xsl:with-param name="scope" select="(ancestor::book|/)[last()]"/>
        </xsl:call-template>
      </xsl:if>

      <xsl:if test="not(parent::article)">
        <xsl:call-template name="process.footnotes"/>
      </xsl:if>
    </div>
  </xsl:if>
</xsl:template>

<xsl:template match="setindex">
  <!-- some implementations use completely empty index tags to indicate -->
  <!-- where an automatically generated index should be inserted. so -->
  <!-- if the index is completely empty, skip it. Unless generate.index -->
  <!-- is non-zero, in which case, this is where the automatically -->
  <!-- generated index should go. -->

  <xsl:if test="count(*)>0 or $generate.index != '0'">
    <div class="{name(.)}">
      <xsl:if test="$generate.id.attributes != 0">
        <xsl:attribute name="id">
          <xsl:call-template name="object.id"/>
        </xsl:attribute>
      </xsl:if>

      <xsl:call-template name="setindex.titlepage"/>
      <xsl:apply-templates/>

      <xsl:if test="count(indexentry) = 0 and count(indexdiv) = 0">
        <xsl:call-template name="generate-index">
          <xsl:with-param name="scope" select="/"/>
        </xsl:call-template>
      </xsl:if>

      <xsl:if test="not(parent::article)">
        <xsl:call-template name="process.footnotes"/>
      </xsl:if>
    </div>
  </xsl:if>
</xsl:template>

<xsl:template match="index/title"></xsl:template>
<xsl:template match="index/subtitle"></xsl:template>
<xsl:template match="index/titleabbrev"></xsl:template>

<!-- ==================================================================== -->

<xsl:template match="indexdiv">
  <div class="{name(.)}">
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="anchor"/>
    <xsl:apply-templates select="*[not(self::indexentry)]"/>
    <dl>
      <xsl:apply-templates select="indexentry"/>
    </dl>
  </div>
</xsl:template>

<xsl:template match="indexdiv/title">
  <h3 class="{name(.)}">
    <xsl:apply-templates/>
  </h3>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="indexterm">
  <!-- this one must have a name, even if it doesn't have an ID -->
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <a class="indexterm" name="{$id}"/>
</xsl:template>

<xsl:template match="primary|secondary|tertiary|see|seealso">
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="indexentry">
  <xsl:apply-templates select="primaryie"/>
</xsl:template>

<xsl:template match="primaryie">
  <dt>
    <xsl:apply-templates/>
  </dt>
  <xsl:choose>
    <xsl:when test="following-sibling::secondaryie">
      <dd>
        <dl>
          <xsl:apply-templates select="following-sibling::secondaryie"/>
        </dl>
      </dd>
    </xsl:when>
    <xsl:when test="following-sibling::seeie
                    |following-sibling::seealsoie">
      <dd>
        <dl>
          <xsl:apply-templates select="following-sibling::seeie
                                       |following-sibling::seealsoie"/>
        </dl>
      </dd>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="secondaryie">
  <dt>
    <xsl:apply-templates/>
  </dt>
  <xsl:choose>
    <xsl:when test="following-sibling::tertiaryie">
      <dd>
        <dl>
          <xsl:apply-templates select="following-sibling::tertiaryie"/>
        </dl>
      </dd>
    </xsl:when>
    <xsl:when test="following-sibling::seeie
                    |following-sibling::seealsoie">
      <dd>
        <dl>
          <xsl:apply-templates select="following-sibling::seeie
                                       |following-sibling::seealsoie"/>
        </dl>
      </dd>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template match="tertiaryie">
  <dt>
    <xsl:apply-templates/>
  </dt>
  <xsl:if test="following-sibling::seeie
                |following-sibling::seealsoie">
    <dd>
      <dl>
        <xsl:apply-templates select="following-sibling::seeie
                                     |following-sibling::seealsoie"/>
      </dl>
    </dd>
  </xsl:if>
</xsl:template>

<xsl:template match="seeie|seealsoie">
  <dt>
    <xsl:apply-templates/>
  </dt>
</xsl:template>

</xsl:stylesheet>
