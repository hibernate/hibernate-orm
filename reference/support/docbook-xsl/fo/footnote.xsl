<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:exsl="http://exslt.org/common"
                exclude-result-prefixes="exsl"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<xsl:template name="format.footnote.mark">
  <xsl:param name="mark" select="'?'"/>
  <fo:inline font-size="90%">
    <xsl:choose>
      <xsl:when test="$fop.extensions != 0">
        <xsl:attribute name="vertical-align">super</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute name="baseline-shift">super</xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:copy-of select="$mark"/>
  </fo:inline>
</xsl:template>

<xsl:template match="footnote">
  <xsl:choose>
    <xsl:when test="ancestor::tgroup">
      <xsl:call-template name="format.footnote.mark">
        <xsl:with-param name="mark">
          <xsl:apply-templates select="." mode="footnote.number"/>
        </xsl:with-param>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <fo:footnote>
        <fo:inline>
          <xsl:call-template name="format.footnote.mark">
            <xsl:with-param name="mark">
              <xsl:apply-templates select="." mode="footnote.number"/>
            </xsl:with-param>
          </xsl:call-template>
        </fo:inline>
        <fo:footnote-body font-family="{$body.fontset}"
                          font-size="{$footnote.font.size}"
                          font-weight="normal"
                          font-style="normal"
                          margin-left="0pc">
          <xsl:apply-templates/>
        </fo:footnote-body>
      </fo:footnote>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="footnoteref">
  <xsl:variable name="footnote" select="key('id',@linkend)"/>
  <xsl:call-template name="format.footnote.mark">
    <xsl:with-param name="mark">
      <xsl:apply-templates select="$footnote" mode="footnote.number"/>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template match="footnote" mode="footnote.number">
  <xsl:choose>
    <xsl:when test="ancestor::tgroup">
      <xsl:variable name="tfnum">
        <xsl:number level="any" from="table|informaltable" format="1"/>
      </xsl:variable>

      <xsl:choose>
        <xsl:when test="string-length($table.footnote.number.symbols) &gt;= $tfnum">
          <xsl:value-of select="substring($table.footnote.number.symbols, $tfnum, 1)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:number level="any" from="tgroup"
                      format="{$table.footnote.number.format}"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="pfoot" select="preceding::footnote"/>
      <xsl:variable name="ptfoot" select="preceding::tgroup//footnote"/>
      <xsl:variable name="fnum" select="count($pfoot) - count($ptfoot) + 1"/>

      <xsl:choose>
        <xsl:when test="string-length($footnote.number.symbols) &gt;= $fnum">
          <xsl:value-of select="substring($footnote.number.symbols, $fnum, 1)"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:number value="$fnum" format="{$footnote.number.format}"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="*" mode="footnote.body.number">
  <xsl:variable name="footnote.mark">
    <xsl:call-template name="format.footnote.mark">
      <xsl:with-param name="mark">
        <xsl:apply-templates select="ancestor::footnote" mode="footnote.number"/>
      </xsl:with-param>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="fo">
    <xsl:apply-templates select="."/>
  </xsl:variable>

  <xsl:variable name="fo-nodes" select="exsl:node-set($fo)"/>

  <xsl:choose>
    <xsl:when test="$fo-nodes//fo:block">
      <xsl:apply-templates select="$fo-nodes" mode="insert.fo.fnum">
        <xsl:with-param name="mark" select="$footnote.mark"/>
      </xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="$fo-nodes" mode="insert.fo.text">
        <xsl:with-param name="mark" select="$footnote.mark"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="footnote/para[1]
                     |footnote/simpara[1]
                     |footnote/formalpara[1]"
              priority="2">
  <!-- this only works if the first thing in a footnote is a para, -->
  <!-- which is ok, because it usually is. -->
  <fo:block>
    <xsl:call-template name="format.footnote.mark">
      <xsl:with-param name="mark">
        <xsl:apply-templates select="ancestor::footnote" mode="footnote.number"/>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:apply-templates/>
  </fo:block>
</xsl:template>

<xsl:template match="footnote" name="process.footnote" mode="table.footnote.mode">
  <xsl:choose>
    <xsl:when test="local-name(*[1]) = 'para' or local-name(*[1]) = 'simpara'">
      <fo:block>
        <xsl:apply-templates/>
      </fo:block>
    </xsl:when>

    <xsl:when test="function-available('exsl:node-set')">
      <fo:block>
        <xsl:apply-templates select="*[1]" mode="footnote.body.number"/>
        <xsl:apply-templates select="*[position() &gt; 1]"/>
      </fo:block>
    </xsl:when>

    <xsl:otherwise>
      <xsl:message>
        <xsl:text>Warning: footnote number may not be generated </xsl:text>
        <xsl:text>correctly; </xsl:text>
        <xsl:value-of select="local-name(*[1])"/>
        <xsl:text> unexpected as first child of footnote.</xsl:text>
      </xsl:message>
      <fo:block>
        <xsl:apply-templates/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
