<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:rx="http://www.renderx.com/XSL/Extensions"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************
     (c) Stephane Bline Peregrine Systems 2001
     Implementation of xep extensions:
       * Pdf bookmarks (based on the XEP 2.5 implementation)
       * Document information (XEP 2.5 meta information extensions)
     ******************************************************************** -->

<!-- ********************************************************************
     Document information
     In PDF bookmarks can't be used characters with code>255. This version of file
     translates characters with code>255 back to ASCII.

        Pavel Zampach (zampach@volny.cz)

     ********************************************************************-->

<!-- FIXME: Norm, I changed things so that the top-level element (book or set)
     does not appear in the TOC. Is this the right thing? -->

<xsl:template name="xep-document-information">
  <rx:meta-info>
    <xsl:if test="//author[1]">
      <xsl:element name="rx:meta-field">
        <xsl:attribute name="name">author</xsl:attribute>
        <xsl:attribute name="value">
          <xsl:call-template name="person.name">
            <xsl:with-param name="node" select="//author[1]"/>
          </xsl:call-template>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

    <xsl:variable name="title">
      <xsl:apply-templates select="/*[1]" mode="label.markup"/>
      <xsl:apply-templates select="/*[1]" mode="title.markup"/>
    </xsl:variable>

    <xsl:element name="rx:meta-field">
      <xsl:attribute name="name">title</xsl:attribute>
      <xsl:attribute name="value">
        <xsl:value-of select="$title"/>
      </xsl:attribute>
    </xsl:element>

    <xsl:if test="//keyword">
      <xsl:element name="rx:meta-field">
        <xsl:attribute name="name">keywords</xsl:attribute>
        <xsl:attribute name="value">
          <xsl:for-each select="//keyword">
            <xsl:value-of select="."/>
            <xsl:if test="position() != last()">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>

    <xsl:if test="//subjectterm">
      <xsl:element name="rx:meta-field">
        <xsl:attribute name="name">subject</xsl:attribute>
        <xsl:attribute name="value">
          <xsl:for-each select="//subjectterm">
            <xsl:value-of select="."/>
            <xsl:if test="position() != last()">
              <xsl:text>, </xsl:text>
            </xsl:if>
          </xsl:for-each>
        </xsl:attribute>
      </xsl:element>
    </xsl:if>
  </rx:meta-info>
</xsl:template>

<!-- ********************************************************************
     Pdf bookmarks
     ******************************************************************** -->

<xsl:template match="*" mode="xep.outline">
  <xsl:apply-templates select="*" mode="xep.outline"/>
</xsl:template>

<xsl:template match="set|book|part|reference|preface|chapter|appendix|article
                     |glossary|bibliography|index|setindex
                     |refentry
                     |sect1|sect2|sect3|sect4|sect5|section"
              mode="xep.outline">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>
  <xsl:variable name="bookmark-label">
    <xsl:apply-templates select="." mode="object.title.markup"/>
  </xsl:variable>

  <!-- Put the root element bookmark at the same level as its children -->
  <!-- If the object is a set or book, generate a bookmark for the toc -->

  <xsl:choose>
    <xsl:when test="parent::*">
      <rx:bookmark internal-destination="{$id}">
        <rx:bookmark-label>
          <xsl:value-of select="$bookmark-label"/>
        </rx:bookmark-label>
        <xsl:apply-templates select="*" mode="xep.outline"/>
      </rx:bookmark>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="$bookmark-label != ''">
        <rx:bookmark internal-destination="{$id}">
          <rx:bookmark-label>
            <xsl:value-of select="$bookmark-label"/>
          </rx:bookmark-label>
        </rx:bookmark>
      </xsl:if>

      <xsl:variable name="toc.params">
        <xsl:call-template name="find.path.params">
          <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
        </xsl:call-template>
      </xsl:variable>
      <xsl:if test="contains($toc.params, 'toc')
                    and set|book|part|reference|section|sect1|refentry
                        |article|bibliography|glossary|chapter
                        |appendix">
        <rx:bookmark internal-destination="toc...{$id}">
          <rx:bookmark-label>
            <xsl:call-template name="gentext">
              <xsl:with-param name="key" select="'TableofContents'"/>
            </xsl:call-template>
          </rx:bookmark-label>
        </rx:bookmark>
      </xsl:if>
      <xsl:apply-templates select="*" mode="xep.outline"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
