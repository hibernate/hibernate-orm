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

<xsl:template name="formal.object">
  <xsl:param name="placement" select="'before'"/>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="content">
    <xsl:if test="$placement = 'before'">
      <xsl:call-template name="formal.object.heading">
        <xsl:with-param name="placement" select="$placement"/>
      </xsl:call-template>
    </xsl:if>
    <xsl:apply-templates/>
    <xsl:if test="$placement != 'before'">
      <xsl:call-template name="formal.object.heading">
        <xsl:with-param name="placement" select="$placement"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="self::figure">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="figure.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:when>
    <xsl:when test="self::example">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="example.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:when>
    <xsl:when test="self::equation">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="equation.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:when>
    <xsl:when test="self::table">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="table.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:when>
    <xsl:when test="self::procedure">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="procedure.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:when>
    <xsl:otherwise>
      <fo:block id="{$id}"
                xsl:use-attribute-sets="formal.object.properties">
        <xsl:copy-of select="$content"/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="formal.object.heading">
  <xsl:param name="object" select="."/>
  <xsl:param name="placement" select="'before'"/>

  <fo:block xsl:use-attribute-sets="formal.title.properties">
    <xsl:choose>
      <xsl:when test="$placement = 'before'">
        <xsl:attribute
               name="keep-with-next.within-column">always</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
        <xsl:attribute
               name="keep-with-previous.within-column">always</xsl:attribute>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:apply-templates select="$object" mode="object.title.markup">
      <xsl:with-param name="allow-anchors" select="1"/>
    </xsl:apply-templates>
  </fo:block>
</xsl:template>

<xsl:template name="informal.object">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="local-name(.) = 'equation' or 
                    local-name(.) = 'informalequation'">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="equation.properties">
        <xsl:apply-templates/>
      </fo:block>
    </xsl:when>
    <xsl:when test="local-name(.) = 'procedure'">
      <fo:block id="{$id}"
                xsl:use-attribute-sets="procedure.properties">
        <xsl:apply-templates/>
      </fo:block>
    </xsl:when>
    <xsl:otherwise>
      <fo:block id="{$id}">
        <xsl:apply-templates/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="semiformal.object">
  <xsl:param name="placement" select="'before'"/>
  <xsl:choose>
    <xsl:when test="./title">
      <xsl:call-template name="formal.object">
        <xsl:with-param name="placement" select="$placement"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="informal.object"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="figure">
  <xsl:variable name="param.placement"
                select="substring-after(normalize-space($formal.title.placement),
                                        concat(local-name(.), ' '))"/>

  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Get align value from internal mediaobject -->
  <xsl:variable name="align">
    <xsl:if test="mediaobject|mediaobjectco|screenshot|graphic|graphicco">
      <xsl:variable name="olist" select="mediaobject/imageobject
                                         |mediaobject/imageobjectco
                                         |mediaobject/videoobject
                                         |mediaobject/audioobject
                                         |mediaobject/textobject

                                         |mediaobjectco/imageobject
                                         |mediaobjectco/imageobjectco
                                         |mediaobjectco/videoobject
                                         |mediaobjectco/audioobject
                                         |mediaobjectco/textobject

                                         |screenshot/mediaobject/imageobject
                                         |screenshot/mediaobject/imageobjectco
                                         |screenshot/mediaobject/videoobject
                                         |screenshot/mediaobject/audioobject
                                         |screenshot/mediaobject/textobject

                                         |screenshot/mediaobjectco/imageobject
                                         |screenshot/mediaobjectco/imageobjectco
                                         |screenshot/mediaobjectco/videoobject
                                         |screenshot/mediaobjectco/audioobject
                                         |screenshot/mediaobjectco/textobject

                                         |graphic
                                         |graphicco/graphic
                                         |screenshot/graphic
                                         |screenshot/graphicco/graphic"/>

      <xsl:variable name="object.index">
        <xsl:call-template name="select.mediaobject.index">
          <xsl:with-param name="olist" select="$olist"/>
          <xsl:with-param name="count" select="1"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:variable name="object" select="$olist[position() = $object.index]"/>

      <xsl:value-of select="$object/imagedata[@align][1]/@align"/>
    </xsl:if>
  </xsl:variable>


  <xsl:variable name="figure">
    <xsl:choose>
      <xsl:when test="$align != ''">
        <fo:block>
          <xsl:attribute name="text-align">
            <xsl:value-of select="$align"/>
          </xsl:attribute>
          <xsl:call-template name="formal.object">
            <xsl:with-param name="placement" select="$placement"/>
          </xsl:call-template>
        </fo:block>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="formal.object">
          <xsl:with-param name="placement" select="$placement"/>
        </xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="@float and @float != '0'">
      <fo:float>
        <xsl:attribute name="float">
          <xsl:choose>
            <xsl:when test="@float = '1'">
              <xsl:value-of select="$default.float.class"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@float"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:copy-of select="$figure"/>
      </fo:float>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$figure"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="example">
  <xsl:variable name="param.placement"
                select="substring-after(normalize-space($formal.title.placement),
                                        concat(local-name(.), ' '))"/>

  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Get align value from internal mediaobject -->
  <xsl:variable name="align">
    <xsl:if test="mediaobject">
      <xsl:variable name="olist" select="mediaobject/imageobject
                     |mediaobject/imageobjectco
                     |mediaobject/videoobject
                     |mediaobject/audioobject
		     |mediaobject/textobject"/>

      <xsl:variable name="object.index">
        <xsl:call-template name="select.mediaobject.index">
          <xsl:with-param name="olist" select="$olist"/>
          <xsl:with-param name="count" select="1"/>
        </xsl:call-template>
      </xsl:variable>

      <xsl:variable name="object" select="$olist[position() = $object.index]"/>

      <xsl:value-of select="$object/imagedata[@align][1]/@align"/>
    </xsl:if>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$align != ''">
      <fo:block>
	  <xsl:attribute name="text-align">
	    <xsl:value-of select="$align"/>
	  </xsl:attribute>
        <xsl:call-template name="formal.object">
          <xsl:with-param name="placement" select="$placement"/>
        </xsl:call-template>
      </fo:block>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="formal.object">
        <xsl:with-param name="placement" select="$placement"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>

</xsl:template>

<xsl:template name="table.frame">
  <xsl:variable name="frame">
    <xsl:choose>
      <xsl:when test="../@frame">
        <xsl:value-of select="../@frame"/>
      </xsl:when>
      <xsl:otherwise>all</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$frame='all'">
      <xsl:attribute name="border-left-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-right-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-left-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-right-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-left-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
      <xsl:attribute name="border-right-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
    </xsl:when>
    <xsl:when test="$frame='bottom'">
      <xsl:attribute name="border-left-style">none</xsl:attribute>
      <xsl:attribute name="border-right-style">none</xsl:attribute>
      <xsl:attribute name="border-top-style">none</xsl:attribute>
      <xsl:attribute name="border-bottom-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
    </xsl:when>
    <xsl:when test="$frame='sides'">
      <xsl:attribute name="border-left-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-right-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-style">none</xsl:attribute>
      <xsl:attribute name="border-bottom-style">none</xsl:attribute>
      <xsl:attribute name="border-left-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-right-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-left-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
      <xsl:attribute name="border-right-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
    </xsl:when>
    <xsl:when test="$frame='top'">
      <xsl:attribute name="border-left-style">none</xsl:attribute>
      <xsl:attribute name="border-right-style">none</xsl:attribute>
      <xsl:attribute name="border-top-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-style">none</xsl:attribute>
      <xsl:attribute name="border-top-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
    </xsl:when>
    <xsl:when test="$frame='topbot'">
      <xsl:attribute name="border-left-style">none</xsl:attribute>
      <xsl:attribute name="border-right-style">none</xsl:attribute>
      <xsl:attribute name="border-top-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-style">
        <xsl:value-of select="$table.frame.border.style"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-width">
        <xsl:value-of select="$table.frame.border.thickness"/>
      </xsl:attribute>
      <xsl:attribute name="border-top-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
      <xsl:attribute name="border-bottom-color">
        <xsl:value-of select="$table.frame.border.color"/>
      </xsl:attribute>
    </xsl:when>
    <xsl:when test="$frame='none'">
      <xsl:attribute name="border-left-style">none</xsl:attribute>
      <xsl:attribute name="border-right-style">none</xsl:attribute>
      <xsl:attribute name="border-top-style">none</xsl:attribute>
      <xsl:attribute name="border-bottom-style">none</xsl:attribute>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message>
        <xsl:text>Impossible frame on table: </xsl:text>
        <xsl:value-of select="$frame"/>
      </xsl:message>
      <xsl:attribute name="border-left-style">none</xsl:attribute>
      <xsl:attribute name="border-right-style">none</xsl:attribute>
      <xsl:attribute name="border-top-style">none</xsl:attribute>
      <xsl:attribute name="border-bottom-style">none</xsl:attribute>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="table">
  <xsl:choose>
    <xsl:when test="tgroup|mediaobject|graphic">
      <xsl:call-template name="calsTable"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="." mode="htmlTable"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="calsTable">
  <xsl:if test="tgroup/tbody/tr
                |tgroup/thead/tr
                |tgroup/tfoot/tr">
    <xsl:message terminate="yes">Broken table: tr descendent of CALS Table.</xsl:message>
  </xsl:if>

  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="param.placement"
                select="substring-after(normalize-space($formal.title.placement),
                                        concat(local-name(.), ' '))"/>

  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="table.content">
    <fo:block id="{$id}"
              xsl:use-attribute-sets="table.properties">

      <xsl:if test="$placement = 'before'">
        <xsl:call-template name="formal.object.heading">
          <xsl:with-param name="placement" select="$placement"/>
        </xsl:call-template>
      </xsl:if>

      <xsl:for-each select="tgroup">
        <xsl:variable name="prop-columns"
                      select=".//colspec[contains(@colwidth, '*')]"/>
        <fo:table xsl:use-attribute-sets="table.table.properties">
          <xsl:call-template name="table.frame"/>
          <xsl:if test="following-sibling::tgroup">
            <xsl:attribute name="border-bottom-width">0pt</xsl:attribute>
            <xsl:attribute name="border-bottom-style">none</xsl:attribute>
            <xsl:attribute name="padding-bottom">0pt</xsl:attribute>
            <xsl:attribute name="margin-bottom">0pt</xsl:attribute>
            <xsl:attribute name="space-after">0pt</xsl:attribute>
            <xsl:attribute name="space-after.minimum">0pt</xsl:attribute>
            <xsl:attribute name="space-after.optimum">0pt</xsl:attribute>
            <xsl:attribute name="space-after.maximum">0pt</xsl:attribute>
          </xsl:if>
          <xsl:if test="preceding-sibling::tgroup">
            <xsl:attribute name="border-top-width">0pt</xsl:attribute>
            <xsl:attribute name="border-top-style">none</xsl:attribute>
            <xsl:attribute name="padding-top">0pt</xsl:attribute>
            <xsl:attribute name="margin-top">0pt</xsl:attribute>
            <xsl:attribute name="space-before">0pt</xsl:attribute>
            <xsl:attribute name="space-before.minimum">0pt</xsl:attribute>
            <xsl:attribute name="space-before.optimum">0pt</xsl:attribute>
            <xsl:attribute name="space-before.maximum">0pt</xsl:attribute>
          </xsl:if>
          <xsl:if test="count($prop-columns) != 0">
            <xsl:attribute name="table-layout">fixed</xsl:attribute>
          </xsl:if>
          <xsl:apply-templates select="."/>
        </fo:table>
      </xsl:for-each>

      <xsl:if test="$placement != 'before'">
        <xsl:call-template name="formal.object.heading">
          <xsl:with-param name="placement" select="$placement"/>
        </xsl:call-template>
      </xsl:if>
    </fo:block>
  </xsl:variable>

  <xsl:variable name="footnotes">
    <xsl:if test="tgroup//footnote">
      <fo:block font-family="{$body.fontset}"
                font-size="{$footnote.font.size}"
                keep-with-previous="always">
        <xsl:apply-templates select="tgroup//footnote" mode="table.footnote.mode"/>
      </fo:block>
    </xsl:if>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="@orient='land'">
      <fo:block-container reference-orientation="90">
        <fo:block>
	  <!-- Such spans won't work in most FO processors since it does
	       not follow the XSL spec, which says it must appear on
	       an element that is a direct child of fo:flow.
	       Some processors relax that requirement, however. -->
          <xsl:attribute name="span">
            <xsl:choose>
              <xsl:when test="@pgwide=1">all</xsl:when>
              <xsl:otherwise>none</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:copy-of select="$table.content"/>
          <xsl:copy-of select="$footnotes"/>
        </fo:block>
      </fo:block-container>
    </xsl:when>
    <xsl:otherwise>
      <fo:block>
        <xsl:attribute name="span">
          <xsl:choose>
            <xsl:when test="@pgwide=1">all</xsl:when>
            <xsl:otherwise>none</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:copy-of select="$table.content"/>
        <xsl:copy-of select="$footnotes"/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="equation">
  <xsl:variable name="param.placement"
                select="substring-after(normalize-space($formal.title.placement),
                                        concat(local-name(.), ' '))"/>

  <xsl:variable name="placement">
    <xsl:choose>
      <xsl:when test="contains($param.placement, ' ')">
        <xsl:value-of select="substring-before($param.placement, ' ')"/>
      </xsl:when>
      <xsl:when test="$param.placement = ''">before</xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$param.placement"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:call-template name="semiformal.object">
    <xsl:with-param name="placement" select="$placement"/>
  </xsl:call-template>
</xsl:template>

<xsl:template match="figure/title"></xsl:template>
<xsl:template match="figure/titleabbrev"></xsl:template>
<xsl:template match="table/title"></xsl:template>
<xsl:template match="table/titleabbrev"></xsl:template>
<xsl:template match="table/textobject"></xsl:template>
<xsl:template match="example/title"></xsl:template>
<xsl:template match="example/titleabbrev"></xsl:template>
<xsl:template match="equation/title"></xsl:template>
<xsl:template match="equation/titleabbrev"></xsl:template>

<xsl:template match="informalfigure">
  <xsl:call-template name="informal.object"/>
</xsl:template>

<xsl:template match="informalexample">
  <xsl:call-template name="informal.object"/>
</xsl:template>

<xsl:template match="informaltable">
  <xsl:choose>
    <xsl:when test="tgroup|mediaobject|graphic">
      <xsl:call-template name="informalCalsTable"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="." mode="htmlTable"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="informalCalsTable">
  <xsl:variable name="id">
    <xsl:call-template name="object.id"/>
  </xsl:variable>

  <xsl:variable name="table.content">
    <xsl:for-each select="tgroup">
      <xsl:variable name="prop-columns"
                    select=".//colspec[contains(@colwidth, '*')]"/>
      <fo:block xsl:use-attribute-sets="informal.object.properties">
	<fo:table xsl:use-attribute-sets="table.table.properties">
	  <xsl:call-template name="table.frame"/>
	  <xsl:if test="following-sibling::tgroup">
	    <xsl:attribute name="border-bottom-width">0pt</xsl:attribute>
	    <xsl:attribute name="border-bottom-style">none</xsl:attribute>
	    <xsl:attribute name="padding-bottom">0pt</xsl:attribute>
	    <xsl:attribute name="margin-bottom">0pt</xsl:attribute>
	    <xsl:attribute name="space-after">0pt</xsl:attribute>
	    <xsl:attribute name="space-after.minimum">0pt</xsl:attribute>
	    <xsl:attribute name="space-after.optimum">0pt</xsl:attribute>
	    <xsl:attribute name="space-after.maximum">0pt</xsl:attribute>
	  </xsl:if>
	  <xsl:if test="preceding-sibling::tgroup">
	    <xsl:attribute name="border-top-width">0pt</xsl:attribute>
	    <xsl:attribute name="border-top-style">none</xsl:attribute>
	    <xsl:attribute name="padding-top">0pt</xsl:attribute>
	    <xsl:attribute name="margin-top">0pt</xsl:attribute>
	    <xsl:attribute name="space-before">0pt</xsl:attribute>
	    <xsl:attribute name="space-before.minimum">0pt</xsl:attribute>
	    <xsl:attribute name="space-before.optimum">0pt</xsl:attribute>
	    <xsl:attribute name="space-before.maximum">0pt</xsl:attribute>
	  </xsl:if>
	  <xsl:if test="count($prop-columns) != 0">
	    <xsl:attribute name="table-layout">fixed</xsl:attribute>
	  </xsl:if>
	  <xsl:apply-templates select="."/>
	</fo:table>
      </fo:block>
    </xsl:for-each>
  </xsl:variable>

  <xsl:variable name="footnotes">
    <xsl:if test="tgroup//footnote">
      <fo:block font-family="{$body.fontset}"
                font-size="{$footnote.font.size}"
                keep-with-previous="always">
        <xsl:apply-templates select="tgroup//footnote" mode="table.footnote.mode"/>
      </fo:block>
    </xsl:if>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="@orient='land'">
      <fo:block-container reference-orientation="90">
        <fo:block id="{$id}">
          <xsl:attribute name="span">
            <xsl:choose>
              <xsl:when test="@pgwide=1">all</xsl:when>
              <xsl:otherwise>none</xsl:otherwise>
            </xsl:choose>
          </xsl:attribute>
          <xsl:copy-of select="$table.content"/>
          <xsl:copy-of select="$footnotes"/>
        </fo:block>
      </fo:block-container>
    </xsl:when>
    <xsl:otherwise>
      <fo:block id="{$id}">
        <xsl:attribute name="span">
          <xsl:choose>
            <xsl:when test="@pgwide=1">all</xsl:when>
            <xsl:otherwise>none</xsl:otherwise>
          </xsl:choose>
        </xsl:attribute>
        <xsl:copy-of select="$table.content"/>
        <xsl:copy-of select="$footnotes"/>
      </fo:block>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="informaltable/textobject"></xsl:template>

<xsl:template match="informalequation">
  <xsl:call-template name="informal.object"/>
</xsl:template>

</xsl:stylesheet>
