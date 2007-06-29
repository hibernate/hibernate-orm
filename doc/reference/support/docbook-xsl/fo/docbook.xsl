<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exsl="http://exslt.org/common"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="exsl"
                version='1.0'>

<!-- It is important to use indent="no" here, otherwise verbatim -->
<!-- environments get broken by indented tags...at least when the -->
<!-- callout extension is used...at least with some processors -->
<xsl:output method="xml" indent="no"/>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:include href="../VERSION"/>
<xsl:include href="param.xsl"/>
<xsl:include href="../lib/lib.xsl"/>
<xsl:include href="../common/l10n.xsl"/>
<xsl:include href="../common/common.xsl"/>
<xsl:include href="../common/labels.xsl"/>
<xsl:include href="../common/titles.xsl"/>
<xsl:include href="../common/subtitles.xsl"/>
<xsl:include href="../common/gentext.xsl"/>
<xsl:include href="../common/pi.xsl"/>
<xsl:include href="autotoc.xsl"/>
<xsl:include href="autoidx.xsl"/>
<xsl:include href="lists.xsl"/>
<xsl:include href="callout.xsl"/>
<xsl:include href="verbatim.xsl"/>
<xsl:include href="graphics.xsl"/>
<xsl:include href="xref.xsl"/>
<xsl:include href="formal.xsl"/>
<xsl:include href="table.xsl"/>
<xsl:include href="htmltbl.xsl"/>
<xsl:include href="sections.xsl"/>
<xsl:include href="inline.xsl"/>
<xsl:include href="footnote.xsl"/>
<xsl:include href="fo.xsl"/>
<xsl:include href="fo-rtf.xsl"/>
<xsl:include href="info.xsl"/>
<xsl:include href="keywords.xsl"/>
<xsl:include href="division.xsl"/>
<xsl:include href="index.xsl"/>
<xsl:include href="toc.xsl"/>
<xsl:include href="refentry.xsl"/>
<xsl:include href="math.xsl"/>
<xsl:include href="admon.xsl"/>
<xsl:include href="component.xsl"/>
<xsl:include href="biblio.xsl"/>
<xsl:include href="glossary.xsl"/>
<xsl:include href="block.xsl"/>
<xsl:include href="task.xsl"/>
<xsl:include href="qandaset.xsl"/>
<xsl:include href="synop.xsl"/>
<xsl:include href="titlepage.xsl"/>
<xsl:include href="titlepage.templates.xsl"/>
<xsl:include href="pagesetup.xsl"/>
<xsl:include href="pi.xsl"/>
<xsl:include href="ebnf.xsl"/>
<xsl:include href="docbookng.xsl"/>

<xsl:include href="fop.xsl"/>
<xsl:include href="passivetex.xsl"/>
<xsl:include href="xep.xsl"/>
<xsl:include href="axf.xsl"/>

<xsl:param name="stylesheet.result.type" select="'fo'"/>

<!-- ==================================================================== -->

<xsl:key name="id" match="*" use="@id"/>

<!-- ==================================================================== -->

<xsl:template match="*">
  <xsl:message>
    <xsl:value-of select="name(.)"/>
    <xsl:text> encountered</xsl:text>
    <xsl:if test="parent::*">
      <xsl:text> in </xsl:text>
      <xsl:value-of select="name(parent::*)"/>
    </xsl:if>
    <xsl:text>, but no template matches.</xsl:text>
  </xsl:message>
  <fo:block color="red">
    <xsl:text>&lt;</xsl:text>
    <xsl:value-of select="name(.)"/>
    <xsl:text>&gt;</xsl:text>
    <xsl:apply-templates/> 
    <xsl:text>&lt;/</xsl:text>
    <xsl:value-of select="name(.)"/>
    <xsl:text>&gt;</xsl:text>
  </fo:block>
</xsl:template>

<!-- Update this list if new root elements supported -->
<xsl:variable name="root.elements" select="' appendix article bibliography book chapter colophon dedication glossary index part preface refentry reference sect1 section set setindex '"/>

<xsl:template match="/">
  <xsl:choose>
    <xsl:when test="function-available('exsl:node-set')
                    and namespace-uri(*[1]) = 'http://docbook.org/docbook-ng'">
      <!-- Hack! If someone hands us a DocBook NG document, toss the namespace -->
      <!-- and continue. Someday we may reverse this logic and add the namespace -->
      <!-- to documents that don't have one. But not before the whole stylesheet -->
      <!-- has been converted to use namespaces. i.e., don't hold your breath -->
      <xsl:variable name="nons">
	<xsl:apply-templates mode="stripNS"/>
      </xsl:variable>
      <xsl:apply-templates select="exsl:node-set($nons)"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="document.element" select="*[1]"/>

      <xsl:if test="not(contains($root.elements,
	                         concat(' ', local-name($document.element), ' ')))">
	<xsl:message terminate="yes">
	  <xsl:text>ERROR: Document root element for FO output </xsl:text>
	  <xsl:text>must be one of the following elements:</xsl:text>
	  <xsl:value-of select="$root.elements"/>
	</xsl:message>
      </xsl:if>

      <xsl:call-template name="root.messages"/>

      <xsl:variable name="title">
	<xsl:choose>
	  <xsl:when test="$document.element/title[1]">
	    <xsl:value-of select="$document.element/title[1]"/>
	  </xsl:when>
	  <xsl:otherwise>[could not find document title]</xsl:otherwise>
	</xsl:choose>
      </xsl:variable>
      
      <fo:root xsl:use-attribute-sets="root.properties">
	<xsl:attribute name="language">
	  <xsl:call-template name="l10n.language">
	    <xsl:with-param name="target" select="/*[1]"/>
	  </xsl:call-template>
	</xsl:attribute>

	<xsl:if test="$xep.extensions != 0">
	  <xsl:call-template name="xep-document-information"/>
	</xsl:if>
	<xsl:if test="$axf.extensions != 0">
	  <xsl:call-template name="axf-document-information"/>
	</xsl:if>
	<xsl:call-template name="setup.pagemasters"/>
	<xsl:choose>
	  <xsl:when test="$rootid != ''">
	    <xsl:choose>
	      <xsl:when test="count(key('id',$rootid)) = 0">
		<xsl:message terminate="yes">
		  <xsl:text>ID '</xsl:text>
		  <xsl:value-of select="$rootid"/>
		  <xsl:text>' not found in document.</xsl:text>
		</xsl:message>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:if test="$fop.extensions != 0">
		  <xsl:apply-templates select="key('id',$rootid)" mode="fop.outline"/>
		</xsl:if>
		<xsl:if test="$xep.extensions != 0">
		  <xsl:variable name="bookmarks">
		    <xsl:apply-templates select="key('id',$rootid)" mode="xep.outline"/>
		  </xsl:variable>
		  <xsl:if test="string($bookmarks) != ''">
		    <rx:outline xmlns:rx="http://www.renderx.com/XSL/Extensions">
		      <xsl:copy-of select="$bookmarks"/>
		    </rx:outline>
		  </xsl:if>
		</xsl:if>
		<xsl:apply-templates select="key('id',$rootid)"/>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:if test="$fop.extensions != 0">
	      <xsl:apply-templates mode="fop.outline"/>
	    </xsl:if>
	    <xsl:if test="$xep.extensions != 0">
	      <xsl:variable name="bookmarks">
		<xsl:apply-templates mode="xep.outline"/>
	      </xsl:variable>
	      <xsl:if test="string($bookmarks) != ''">
		<rx:outline xmlns:rx="http://www.renderx.com/XSL/Extensions">
		  <xsl:copy-of select="$bookmarks"/>
		</rx:outline>
	      </xsl:if>
	    </xsl:if>
	    <xsl:apply-templates/>
	  </xsl:otherwise>
	</xsl:choose>
      </fo:root>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="root.messages">
  <!-- redefine this any way you'd like to output messages -->
  <!-- DO NOT OUTPUT ANYTHING FROM THIS TEMPLATE -->
  <xsl:message>
    <xsl:text>Making </xsl:text>
    <xsl:value-of select="$page.orientation"/>
    <xsl:text> pages on </xsl:text>
    <xsl:value-of select="$paper.type"/>
    <xsl:text> paper (</xsl:text>
    <xsl:value-of select="$page.width"/>
    <xsl:text>x</xsl:text>
    <xsl:value-of select="$page.height"/>
    <xsl:text>)</xsl:text>
  </xsl:message>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="*" mode="stripNS">
  <xsl:choose>
    <xsl:when test="namespace-uri(.) = 'http://docbook.org/docbook-ng'">
      <xsl:element name="{local-name(.)}">
	<xsl:copy-of select="@*"/>
	<xsl:apply-templates mode="stripNS"/>
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy>
	<xsl:copy-of select="@*"/>
	<xsl:apply-templates mode="stripNS"/>
      </xsl:copy>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="comment()|processing-instruction()|text()" mode="stripNS">
  <xsl:copy/>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
