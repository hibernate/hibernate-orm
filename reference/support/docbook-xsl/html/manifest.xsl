<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:doc="http://nwalsh.com/xsl/documentation/1.0"
		version="1.0"
                exclude-result-prefixes="doc">

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:variable name="manifest.base.dir">
</xsl:variable>

<xsl:template name="generate.manifest">
  <xsl:param name="node" select="/"/>
  <xsl:call-template name="write.text.chunk">
    <xsl:with-param name="filename">
      <xsl:if test="$manifest.in.base.dir != 0">
        <xsl:value-of select="$base.dir"/>
      </xsl:if>
      <xsl:value-of select="$manifest"/>
    </xsl:with-param>
    <xsl:with-param name="method" select="'text'"/>
    <xsl:with-param name="content">
      <xsl:apply-templates select="$node" mode="enumerate-files"/>
    </xsl:with-param>
    <xsl:with-param name="encoding" select="$chunker.output.encoding"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="set|book|part|preface|chapter|appendix
                     |article
                     |reference|refentry
                     |sect1|sect2|sect3|sect4|sect5
                     |section
                     |book/glossary|article/glossary
                     |book/bibliography|article/bibliography
                     |book/index|article/index
                     |colophon"
              mode="enumerate-files">
  <xsl:variable name="ischunk"><xsl:call-template name="chunk"/></xsl:variable>
  <xsl:if test="$ischunk='1'">
    <xsl:call-template name="make-relative-filename">
      <xsl:with-param name="base.dir">
        <xsl:if test="$manifest.in.base.dir = 0">
          <xsl:value-of select="$base.dir"/>
        </xsl:if>
      </xsl:with-param>
      <xsl:with-param name="base.name">
        <xsl:apply-templates mode="chunk-filename" select="."/>
      </xsl:with-param>
    </xsl:call-template>
    <xsl:text>&#10;</xsl:text>
  </xsl:if>
  <xsl:apply-templates select="*" mode="enumerate-files"/>
</xsl:template>

<xsl:template match="legalnotice" mode="enumerate-files">
  <xsl:variable name="id"><xsl:call-template name="object.id"/></xsl:variable>
  <xsl:if test="$generate.legalnotice.link != 0">
    <xsl:call-template name="make-relative-filename">
      <xsl:with-param name="base.dir">
        <xsl:if test="$manifest.in.base.dir = 0">
          <xsl:value-of select="$base.dir"/>
        </xsl:if>
      </xsl:with-param>
      <xsl:with-param name="base.name" select="concat('ln-',$id,$html.ext)"/>
    </xsl:call-template>
    <xsl:text>&#10;</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="mediaobject[imageobject] | inlinemediaobject[imageobject]" mode="enumerate-files">
  <xsl:variable name="longdesc.uri">
    <xsl:call-template name="longdesc.uri">
      <xsl:with-param name="mediaobject"
                      select="."/>
    </xsl:call-template>
  </xsl:variable>
  <xsl:variable name="mediaobject" select="."/>

  <xsl:if test="$html.longdesc != 0 and $mediaobject/textobject[not(phrase)]">
    <xsl:call-template name="longdesc.uri">
      <xsl:with-param name="mediaobject" select="$mediaobject"/>
    </xsl:call-template>
    <xsl:text>&#10;</xsl:text>
  </xsl:if>
</xsl:template>

<xsl:template match="text()" mode="enumerate-files">
</xsl:template>

</xsl:stylesheet>
