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

<xsl:template match="glossary">
  <div class="{name(.)}">
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="glossary.titlepage"/>

    <xsl:choose>
      <xsl:when test="glossdiv">
        <xsl:apply-templates select="(glossdiv[1]/preceding-sibling::*)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="(glossentry[1]/preceding-sibling::*)"/>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="glossdiv">
        <xsl:apply-templates select="glossdiv"/>
      </xsl:when>
      <xsl:otherwise>
        <dl>
          <xsl:apply-templates select="glossentry"/>
        </dl>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="not(parent::article)">
      <xsl:call-template name="process.footnotes"/>
    </xsl:if>
  </div>
</xsl:template>

<xsl:template match="glossary/glossaryinfo"></xsl:template>
<xsl:template match="glossary/title"></xsl:template>
<xsl:template match="glossary/subtitle"></xsl:template>
<xsl:template match="glossary/titleabbrev"></xsl:template>

<!-- ==================================================================== -->

<xsl:template match="glosslist">
  <div class="{name(.)}">
    <xsl:call-template name="anchor"/>
    <xsl:if test="blockinfo/title|title">
      <xsl:call-template name="formal.object.heading"/>
    </xsl:if>
    <dl>
      <xsl:apply-templates select="glossentry"/>
    </dl>
  </div>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="glossdiv">
  <div class="{name(.)}">
    <xsl:apply-templates select="(glossentry[1]/preceding-sibling::*)"/>

    <dl>
      <xsl:apply-templates select="glossentry"/>
    </dl>
  </div>
</xsl:template>

<xsl:template match="glossdiv/title">
  <h3 class="{name(.)}">
    <xsl:apply-templates/>
  </h3>
</xsl:template>

<!-- ==================================================================== -->

<!--
GlossEntry ::=
  GlossTerm, Acronym?, Abbrev?,
  (IndexTerm)*,
  RevHistory?,
  (GlossSee | GlossDef+)
-->

<xsl:template match="glossentry">
  <xsl:choose>
    <xsl:when test="$glossentry.show.acronym = 'primary'">
      <dt>
        <xsl:call-template name="anchor">
          <xsl:with-param name="conditional">
            <xsl:choose>
              <xsl:when test="$glossterm.auto.link != 0">0</xsl:when>
              <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>

        <xsl:choose>
          <xsl:when test="acronym|abbrev">
            <xsl:apply-templates select="acronym|abbrev"/>
            <xsl:text> (</xsl:text>
            <xsl:apply-templates select="glossterm"/>
            <xsl:text>)</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="glossterm"/>
          </xsl:otherwise>
        </xsl:choose>
      </dt>
    </xsl:when>
    <xsl:when test="$glossentry.show.acronym = 'yes'">
      <dt>
        <xsl:call-template name="anchor">
          <xsl:with-param name="conditional">
            <xsl:choose>
              <xsl:when test="$glossterm.auto.link != 0">0</xsl:when>
              <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>

        <xsl:apply-templates select="glossterm"/>

        <xsl:if test="acronym|abbrev">
          <xsl:text> (</xsl:text>
          <xsl:apply-templates select="acronym|abbrev"/>
          <xsl:text>)</xsl:text>
        </xsl:if>
      </dt>
    </xsl:when>
    <xsl:otherwise>
      <dt>
        <xsl:call-template name="anchor">
          <xsl:with-param name="conditional">
            <xsl:choose>
              <xsl:when test="$glossterm.auto.link != 0">0</xsl:when>
              <xsl:otherwise>1</xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:call-template>

        <xsl:apply-templates select="glossterm"/>
      </dt>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:apply-templates select="indexterm|revhistory|glosssee|glossdef"/>
</xsl:template>

<xsl:template match="glossentry/glossterm">
  <xsl:apply-templates/>
  <xsl:if test="following-sibling::glossterm">, </xsl:if>
</xsl:template>

<xsl:template match="glossentry/acronym">
  <xsl:apply-templates/>
  <xsl:if test="following-sibling::acronym|following-sibling::abbrev">, </xsl:if>
</xsl:template>

<xsl:template match="glossentry/abbrev">
  <xsl:apply-templates/>
  <xsl:if test="following-sibling::acronym|following-sibling::abbrev">, </xsl:if>
</xsl:template>

<xsl:template match="glossentry/revhistory">
</xsl:template>

<xsl:template match="glossentry/glosssee">
  <xsl:variable name="otherterm" select="@otherterm"/>
  <xsl:variable name="targets" select="//node()[@id=$otherterm]"/>
  <xsl:variable name="target" select="$targets[1]"/>

  <dd>
    <p>
      <xsl:call-template name="gentext.template">
        <xsl:with-param name="context" select="'glossary'"/>
        <xsl:with-param name="name" select="'see'"/>
      </xsl:call-template>
      <xsl:choose>
        <xsl:when test="$target">
          <a href="#{@otherterm}">
            <xsl:apply-templates select="$target" mode="xref-to"/>
          </a>
        </xsl:when>
        <xsl:when test="$otherterm != '' and not($target)">
          <xsl:message>
            <xsl:text>Warning: glosssee @otherterm reference not found: </xsl:text>
            <xsl:value-of select="$otherterm"/>
          </xsl:message>
          <xsl:apply-templates/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>.</xsl:text>
    </p>
  </dd>
</xsl:template>

<xsl:template match="glossentry/glossdef">
  <dd>
    <xsl:apply-templates select="*[local-name(.) != 'glossseealso']"/>
    <xsl:if test="glossseealso">
      <p>
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="context" select="'glossary'"/>
          <xsl:with-param name="name" select="'seealso'"/>
        </xsl:call-template>
        <xsl:apply-templates select="glossseealso"/>
      </p>
    </xsl:if>
  </dd>
</xsl:template>

<xsl:template match="glossseealso">
  <xsl:variable name="otherterm" select="@otherterm"/>
  <xsl:variable name="targets" select="//node()[@id=$otherterm]"/>
  <xsl:variable name="target" select="$targets[1]"/>

  <xsl:choose>
    <xsl:when test="$target">
      <a href="#{@otherterm}">
        <xsl:apply-templates select="$target" mode="xref-to"/>
      </a>
    </xsl:when>
    <xsl:when test="$otherterm != '' and not($target)">
      <xsl:message>
        <xsl:text>Warning: glossseealso @otherterm reference not found: </xsl:text>
        <xsl:value-of select="$otherterm"/>
      </xsl:message>
      <xsl:apply-templates/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates/>
    </xsl:otherwise>
  </xsl:choose>

  <xsl:choose>
    <xsl:when test="position() = last()">
      <xsl:text>.</xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>, </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==================================================================== -->

<!-- Glossary collection -->

<xsl:template match="glossary[@role='auto']" priority="2">
  <xsl:variable name="terms" select="//glossterm[not(parent::glossdef)]|//firstterm"/>
  <xsl:variable name="collection" select="document($glossary.collection, .)"/>

  <xsl:if test="$glossary.collection = ''">
    <xsl:message>
      <xsl:text>Warning: processing automatic glossary </xsl:text>
      <xsl:text>without a glossary.collection file.</xsl:text>
    </xsl:message>
  </xsl:if>

  <xsl:if test="not($collection) and $glossary.collection != ''">
    <xsl:message>
      <xsl:text>Warning: processing automatic glossary but unable to </xsl:text>
      <xsl:text>open glossary.collection file '</xsl:text>
      <xsl:value-of select="$glossary.collection"/>
      <xsl:text>'</xsl:text>
    </xsl:message>
  </xsl:if>

  <div class="{name(.)}">
    <xsl:if test="$generate.id.attributes != 0">
      <xsl:attribute name="id">
        <xsl:call-template name="object.id"/>
      </xsl:attribute>
    </xsl:if>

    <xsl:call-template name="glossary.titlepage"/>

    <xsl:choose>
      <xsl:when test="glossdiv and $collection//glossdiv">
        <xsl:for-each select="$collection//glossdiv">
          <!-- first see if there are any in this div -->
          <xsl:variable name="exist.test">
            <xsl:for-each select="glossentry">
              <xsl:variable name="cterm" select="glossterm"/>
              <xsl:if test="$terms[@baseform = $cterm or . = $cterm]">
                <xsl:value-of select="glossterm"/>
              </xsl:if>
            </xsl:for-each>
          </xsl:variable>

          <xsl:if test="$exist.test != ''">
            <xsl:apply-templates select="." mode="auto-glossary">
              <xsl:with-param name="terms" select="$terms"/>
            </xsl:apply-templates>
          </xsl:if>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <dl>
          <xsl:for-each select="$collection//glossentry">
            <xsl:variable name="cterm" select="glossterm"/>
            <xsl:if test="$terms[@baseform = $cterm or . = $cterm]">
              <xsl:apply-templates select="." mode="auto-glossary"/>
            </xsl:if>
          </xsl:for-each>
        </dl>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:if test="not(parent::article)">
      <xsl:call-template name="process.footnotes"/>
    </xsl:if>
  </div>
</xsl:template>

<xsl:template match="*" mode="auto-glossary">
  <!-- pop back out to the default mode for most elements -->
  <xsl:apply-templates select="."/>
</xsl:template>

<xsl:template match="glossdiv" mode="auto-glossary">
  <xsl:param name="terms" select="."/>

  <div class="{name(.)}">
    <xsl:apply-templates select="(glossentry[1]/preceding-sibling::*)"/>

    <dl>
      <xsl:for-each select="glossentry">
        <xsl:variable name="cterm" select="glossterm"/>
        <xsl:if test="$terms[@baseform = $cterm or . = $cterm]">
          <xsl:apply-templates select="." mode="auto-glossary"/>
        </xsl:if>
      </xsl:for-each>
    </dl>
  </div>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
