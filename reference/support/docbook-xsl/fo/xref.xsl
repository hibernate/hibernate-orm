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

<!-- Create keys for quickly looking up olink targets -->
<xsl:key name="targetdoc-key" match="document" use="@targetdoc" />
<xsl:key name="targetptr-key"  match="div|obj"
         use="concat(ancestor::document/@targetdoc, '/', @targetptr)" />

<!-- ==================================================================== -->

<xsl:template match="anchor">
  <fo:wrapper id="{@id}"/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="xref" name="xref">
  <xsl:variable name="targets" select="key('id',@linkend)"/>
  <xsl:variable name="target" select="$targets[1]"/>
  <xsl:variable name="refelem" select="local-name($target)"/>

  <xsl:call-template name="check.id.unique">
    <xsl:with-param name="linkend" select="@linkend"/>
  </xsl:call-template>

  <xsl:choose>
    <xsl:when test="$refelem=''">
      <xsl:message>
        <xsl:text>XRef to nonexistent id: </xsl:text>
        <xsl:value-of select="@linkend"/>
      </xsl:message>
      <xsl:text>???</xsl:text>
    </xsl:when>

    <xsl:when test="@endterm">
      <fo:basic-link internal-destination="{@linkend}"
                     xsl:use-attribute-sets="xref.properties">
        <xsl:variable name="etargets" select="key('id',@endterm)"/>
        <xsl:variable name="etarget" select="$etargets[1]"/>
        <xsl:choose>
          <xsl:when test="count($etarget) = 0">
            <xsl:message>
              <xsl:value-of select="count($etargets)"/>
              <xsl:text>Endterm points to nonexistent ID: </xsl:text>
              <xsl:value-of select="@endterm"/>
            </xsl:message>
            <xsl:text>???</xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="$etarget" mode="endterm"/>
          </xsl:otherwise>
        </xsl:choose>
      </fo:basic-link>
    </xsl:when>

    <xsl:when test="$target/@xreflabel">
      <fo:basic-link internal-destination="{@linkend}"
                     xsl:use-attribute-sets="xref.properties">
        <xsl:call-template name="xref.xreflabel">
          <xsl:with-param name="target" select="$target"/>
        </xsl:call-template>
      </fo:basic-link>
    </xsl:when>

    <xsl:otherwise>
      <fo:basic-link internal-destination="{@linkend}"
                     xsl:use-attribute-sets="xref.properties">
        <xsl:apply-templates select="$target" mode="xref-to">
          <xsl:with-param name="referrer" select="."/>
          <xsl:with-param name="xrefstyle">
            <xsl:choose>
              <xsl:when test="@role and not(@xrefstyle) and $use.role.as.xrefstyle != 0">
                <xsl:value-of select="@role"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="@xrefstyle"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:with-param>
        </xsl:apply-templates>
      </fo:basic-link>
    </xsl:otherwise>
  </xsl:choose>

  <!-- Add standard page reference? -->
  <xsl:if test="not(starts-with(normalize-space(@xrefstyle), 'select:') != '' 
                and (contains(@xrefstyle, 'page')
                     or contains(@xrefstyle, 'Page')))
                and ( $insert.xref.page.number = 'yes' 
		   or $insert.xref.page.number = '1')
                or local-name($target) = 'para'">
    <fo:basic-link internal-destination="{@linkend}"
                   xsl:use-attribute-sets="xref.properties">
      <xsl:apply-templates select="$target" mode="page.citation">
        <xsl:with-param name="id" select="@linkend"/>
      </xsl:apply-templates>
    </fo:basic-link>
  </xsl:if>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="*" mode="endterm">
  <!-- Process the children of the endterm element -->
  <xsl:variable name="endterm">
    <xsl:apply-templates select="child::node()"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="function-available('exsl:node-set')">
      <xsl:apply-templates select="exsl:node-set($endterm)" mode="remove-ids"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$endterm"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*" mode="remove-ids">
  <xsl:copy>
    <xsl:for-each select="@*">
      <xsl:choose>
        <xsl:when test="name(.) != 'id'">
          <xsl:copy/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>removing <xsl:value-of select="name(.)"/></xsl:message>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
    <xsl:apply-templates mode="remove-ids"/>
  </xsl:copy>
</xsl:template>

<!--- ==================================================================== -->

<xsl:template match="*" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:message>
    <xsl:text>Don't know what gentext to create for xref to: "</xsl:text>
    <xsl:value-of select="name(.)"/>
    <xsl:text>"</xsl:text>
  </xsl:message>
  <xsl:text>???</xsl:text>
</xsl:template>

<xsl:template match="title" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <!-- if you xref to a title, xref to the parent... -->
  <xsl:choose>
    <!-- FIXME: how reliable is this? -->
    <xsl:when test="contains(local-name(parent::*), 'info')">
      <xsl:apply-templates select="parent::*[2]" mode="xref-to">
        <xsl:with-param name="referrer" select="$referrer"/>
        <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
      </xsl:apply-templates>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="parent::*" mode="xref-to">
        <xsl:with-param name="referrer" select="$referrer"/>
        <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="abstract|article|authorblurb|bibliodiv|bibliomset
                     |biblioset|blockquote|calloutlist|caution|colophon
                     |constraintdef|formalpara|glossdiv|important|indexdiv
                     |itemizedlist|legalnotice|lot|msg|msgexplan|msgmain
                     |msgrel|msgset|msgsub|note|orderedlist|partintro
                     |productionset|qandadiv|refsynopsisdiv|segmentedlist
                     |set|setindex|sidebar|tip|toc|variablelist|warning"
              mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <!-- catch-all for things with (possibly optional) titles -->
  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="author|editor|othercredit|personname" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:call-template name="person.name"/>
</xsl:template>

<xsl:template match="authorgroup" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:call-template name="person.name.list"/>
</xsl:template>

<xsl:template match="figure|example|table|equation" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="procedure" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="cmdsynopsis" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="(.//command)[1]" mode="xref"/>
</xsl:template>

<xsl:template match="funcsynopsis" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="(.//function)[1]" mode="xref"/>
</xsl:template>

<xsl:template match="dedication|preface|chapter|appendix" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="bibliography" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="biblioentry|bibliomixed" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <!-- handles both biblioentry and bibliomixed -->
  <xsl:text>[</xsl:text>
  <xsl:choose>
    <xsl:when test="string(.) = ''">
      <xsl:variable name="bib" select="document($bibliography.collection,.)"/>
      <xsl:variable name="id" select="@id"/>
      <xsl:variable name="entry" select="$bib/bibliography/*[@id=$id][1]"/>
      <xsl:choose>
        <xsl:when test="$entry">
          <xsl:choose>
            <xsl:when test="$bibliography.numbered != 0">
              <xsl:number from="bibliography" count="biblioentry|bibliomixed"
                          level="any" format="1"/>
            </xsl:when>
            <xsl:when test="local-name($entry/*[1]) = 'abbrev'">
              <xsl:apply-templates select="$entry/*[1]"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="@id"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>
            <xsl:text>No bibliography entry: </xsl:text>
            <xsl:value-of select="$id"/>
            <xsl:text> found in </xsl:text>
            <xsl:value-of select="$bibliography.collection"/>
          </xsl:message>
          <xsl:value-of select="@id"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="$bibliography.numbered != 0">
          <xsl:number from="bibliography" count="biblioentry|bibliomixed"
                      level="any" format="1"/>
        </xsl:when>
        <xsl:when test="local-name(*[1]) = 'abbrev'">
          <xsl:apply-templates select="*[1]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@id"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:text>]</xsl:text>
</xsl:template>

<xsl:template match="glossary" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="glossentry" mode="xref-to">
  <xsl:choose>
    <xsl:when test="$glossentry.show.acronym = 'primary'">
      <xsl:choose>
        <xsl:when test="acronym|abbrev">
          <xsl:apply-templates select="(acronym|abbrev)[1]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="glossterm[1]" mode="xref-to"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="glossterm[1]" mode="xref-to"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="glossterm" mode="xref-to">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="index" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="listitem" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="section|simplesect
                     |sect1|sect2|sect3|sect4|sect5
                     |refsect1|refsect2|refsect3|refsection" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
  <!-- What about "in Chapter X"? -->
</xsl:template>

<xsl:template match="bridgehead" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
  <!-- What about "in Chapter X"? -->
</xsl:template>

<xsl:template match="qandaset" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="qandadiv" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="qandaentry" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="question[1]" mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="question|answer" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="part|reference" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="refentry" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:choose>
    <xsl:when test="refmeta/refentrytitle">
      <xsl:apply-templates select="refmeta/refentrytitle"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="refnamediv/refname[1]"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates select="refmeta/manvolnum"/>
</xsl:template>

<xsl:template match="refnamediv" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="refname[1]" mode="xref-to">
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="refname" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates mode="xref-to">
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="step" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:call-template name="gentext">
    <xsl:with-param name="key" select="'Step'"/>
  </xsl:call-template>
  <xsl:text> </xsl:text>
  <xsl:apply-templates select="." mode="number"/>
</xsl:template>

<xsl:template match="varlistentry" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="term[1]" mode="xref-to">
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="varlistentry/term" mode="xref-to">
  <!-- to avoid the comma that will be generated if there are several terms -->
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="co" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="callout-bug"/>
</xsl:template>

<xsl:template match="book" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="para" mode="xref-to">
  <xsl:param name="referrer"/>
  <xsl:param name="xrefstyle"/>

  <xsl:variable name="context" select="(ancestor::simplesect
                                       |ancestor::section
                                       |ancestor::sect1
                                       |ancestor::sect2
                                       |ancestor::sect3
                                       |ancestor::sect4
                                       |ancestor::sect5
                                       |ancestor::refsection
                                       |ancestor::refsect1
                                       |ancestor::refsect2
                                       |ancestor::refsect3
                                       |ancestor::chapter
                                       |ancestor::appendix
                                       |ancestor::preface
                                       |ancestor::partintro
                                       |ancestor::dedication
                                       |ancestor::colophon
                                       |ancestor::bibliography
                                       |ancestor::index
                                       |ancestor::glossary
                                       |ancestor::glossentry
                                       |ancestor::listitem
                                       |ancestor::varlistentry)[last()]"/>

  <xsl:apply-templates select="$context" mode="xref-to"/>
<!--
  <xsl:apply-templates select="." mode="object.xref.markup">
    <xsl:with-param name="purpose" select="'xref'"/>
    <xsl:with-param name="xrefstyle" select="$xrefstyle"/>
    <xsl:with-param name="referrer" select="$referrer"/>
  </xsl:apply-templates>
-->
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="link" name="link">
  <xsl:variable name="targets" select="key('id',@linkend)"/>
  <xsl:variable name="target" select="$targets[1]"/>

  <xsl:call-template name="check.id.unique">
    <xsl:with-param name="linkend" select="@linkend"/>
  </xsl:call-template>

  <fo:basic-link internal-destination="{@linkend}"
                 xsl:use-attribute-sets="xref.properties">
    <xsl:choose>
      <xsl:when test="count(child::node()) &gt; 0">
        <!-- If it has content, use it -->
        <xsl:apply-templates/>
      </xsl:when>
      <xsl:otherwise>
        <!-- else look for an endterm -->
        <xsl:choose>
          <xsl:when test="@endterm">
            <xsl:variable name="etargets" select="key('id',@endterm)"/>
            <xsl:variable name="etarget" select="$etargets[1]"/>
            <xsl:choose>
              <xsl:when test="count($etarget) = 0">
                <xsl:message>
                  <xsl:value-of select="count($etargets)"/>
                  <xsl:text>Endterm points to nonexistent ID: </xsl:text>
                  <xsl:value-of select="@endterm"/>
                </xsl:message>
                <xsl:text>???</xsl:text>
              </xsl:when>
              <xsl:otherwise>
                  <xsl:apply-templates select="$etarget" mode="endterm"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>

          <xsl:otherwise>
            <xsl:message>
              <xsl:text>Link element has no content and no Endterm. </xsl:text>
              <xsl:text>Nothing to show in the link to </xsl:text>
              <xsl:value-of select="$target"/>
            </xsl:message>
            <xsl:text>???</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </fo:basic-link>
</xsl:template>

<xsl:template match="ulink" name="ulink">
  <fo:basic-link xsl:use-attribute-sets="xref.properties">
    <xsl:attribute name="external-destination">
      <xsl:call-template name="fo-external-image">
        <xsl:with-param name="filename" select="@url"/>
      </xsl:call-template>
    </xsl:attribute>

    <xsl:choose>
      <xsl:when test="count(child::node())=0">
        <xsl:call-template name="hyphenate-url">
          <xsl:with-param name="url" select="@url"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </fo:basic-link>

  <xsl:if test="count(child::node()) != 0
                and string(.) != @url
                and $ulink.show != 0">
    <!-- yes, show the URI -->
    <xsl:choose>
      <xsl:when test="$ulink.footnotes != 0 and not(ancestor::footnote)">
	<xsl:text>&#xA0;</xsl:text>
        <fo:footnote>
          <xsl:call-template name="ulink.footnote.number"/>
          <fo:footnote-body font-family="{$body.fontset}"
                            font-size="{$footnote.font.size}">
            <fo:block>
              <xsl:call-template name="ulink.footnote.number"/>
              <xsl:text> </xsl:text>
              <fo:inline>
                <xsl:value-of select="@url"/>
              </fo:inline>
            </fo:block>
          </fo:footnote-body>
        </fo:footnote>
      </xsl:when>
      <xsl:otherwise>
        <fo:inline hyphenate="false">
          <xsl:text> [</xsl:text>
          <xsl:call-template name="hyphenate-url">
            <xsl:with-param name="url" select="@url"/>
          </xsl:call-template>
          <xsl:text>]</xsl:text>
        </fo:inline>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<xsl:template name="ulink.footnote.number">
  <fo:inline font-size="90%">
    <!-- FIXME: this isn't going to be perfect! -->
    <xsl:text>[</xsl:text>
    <xsl:number level="any"
                from="chapter|appendix|preface|article|refentry"
                format="{$ulink.footnote.number.format}"/>
    <xsl:text>]</xsl:text>
  </fo:inline>
</xsl:template>

<xsl:template name="hyphenate-url">
  <xsl:param name="url" select="''"/>
  <xsl:choose>
    <xsl:when test="$ulink.hyphenate = ''">
      <xsl:value-of select="$url"/>
    </xsl:when>
    <xsl:when test="contains($url, '/')">
      <xsl:value-of select="substring-before($url, '/')"/>
      <xsl:text>/</xsl:text>
      <xsl:copy-of select="$ulink.hyphenate"/>
      <xsl:call-template name="hyphenate-url">
        <xsl:with-param name="url" select="substring-after($url, '/')"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$url"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<xsl:template match="olink">
  <xsl:param name="target.database"
      select="document($target.database.document, /)"/>

  <xsl:variable name="localinfo" select="@localinfo"/>

  <!-- Olink that points to internal id can be a link -->
  <xsl:variable name="linkend">
    <xsl:choose>
      <xsl:when test="@targetdoc and not(@targetptr)" >
        <xsl:message>Olink missing @targetptr attribute value</xsl:message>
      </xsl:when>
      <xsl:when test="not(@targetdoc) and @targetptr" >
        <xsl:message>Olink missing @targetdoc attribute value</xsl:message>
      </xsl:when>
      <xsl:when test="@targetdoc and @targetptr">
        <xsl:if test="$current.docid = @targetdoc">
          <xsl:if test="id(@targetptr)">
            <xsl:value-of select="@targetptr"/>
          </xsl:if>
        </xsl:if>
      </xsl:when>
    </xsl:choose>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$linkend != ''">
      <fo:basic-link internal-destination="{$linkend}"
                   xsl:use-attribute-sets="xref.properties">
        <xsl:call-template name="olink.hottext">
          <xsl:with-param name="target.database" select="$target.database"/>
        </xsl:call-template>
      </fo:basic-link>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="olink.hottext">
        <xsl:with-param name="target.database" select="$target.database"/>
      </xsl:call-template>

      <!-- Append other document title if appropriate -->
      <xsl:if test="@targetdoc and @targetptr and $olink.doctitle != 0
                  and $current.docid != '' and $current.docid != @targetdoc">
        <xsl:variable name="doctitle">
          <xsl:variable name="seek.targetdoc" select="@targetdoc"/>
          <xsl:for-each select="$target.database" >
            <xsl:value-of select="key('targetdoc-key', $seek.targetdoc)/div[1]/ttl" />
          </xsl:for-each>
        </xsl:variable>
        <xsl:if test="$doctitle != ''">
          <xsl:text> (</xsl:text><xsl:value-of select="$doctitle"/><xsl:text>)</xsl:text>
        </xsl:if>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>


<xsl:template name="olink.hottext">
  <xsl:param name="target.database"/>

  <xsl:choose>
    <!-- If it has elements or text (not just PI or comment) -->
    <xsl:when test="child::text() or child::*">
      <xsl:apply-templates/>
    </xsl:when>
    <xsl:when test="@targetdoc and @targetptr">
      <!-- Get the xref text for this record -->
      <xsl:variable name="seek.targetdoc" select="@targetdoc"/>
      <xsl:variable name="seek.targetptr" select="@targetptr"/>
      <xsl:variable name="xref.text" >
        <xsl:for-each select="$target.database" >
          <xsl:value-of select="key('targetptr-key', concat($seek.targetdoc, '/', $seek.targetptr))/xreftext"/>
        </xsl:for-each>
      </xsl:variable>

      <xsl:choose>
        <xsl:when test="$use.local.olink.style != 0">
          <!-- Get the element name and lang for this targetptr -->
          <xsl:variable name="element" >
            <xsl:for-each select="$target.database" >
              <xsl:value-of select="key('targetptr-key', concat($seek.targetdoc, '/', $seek.targetptr))/@element"/>
            </xsl:for-each>
          </xsl:variable>

          <xsl:variable name="lang">
            <xsl:variable name="candidate">
              <xsl:for-each select="$target.database" >
                <xsl:value-of select="key('targetptr-key', concat($seek.targetdoc, '/', $seek.targetptr))/@lang"/>
              </xsl:for-each>
            </xsl:variable>
            <xsl:choose>
              <xsl:when test="$candidate != ''">
                <xsl:value-of select="$candidate"/>
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="'en'"/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:variable>
          <xsl:variable name="template">
            <xsl:call-template name="gentext.template">
              <xsl:with-param name="context" select="'title'"/>
              <xsl:with-param name="name" select="$element"/>
              <xsl:with-param name="lang" select="$lang"/>
            </xsl:call-template>
          </xsl:variable>
          <xsl:call-template name="substitute-markup">
            <xsl:with-param name="template" select="$template"/>
            <xsl:with-param name="title">
              <xsl:for-each select="$target.database" >
                <xsl:value-of select="key('targetptr-key', concat($seek.targetdoc, '/', $seek.targetptr))/ttl"/>
              </xsl:for-each>
            </xsl:with-param>
            <xsl:with-param name="label">
              <xsl:for-each select="$target.database" >
                <xsl:value-of select="key('targetptr-key', concat($seek.targetdoc, '/', $seek.targetptr))/@number"/>
              </xsl:for-each>
            </xsl:with-param>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$xref.text !=''">
          <xsl:value-of select="$xref.text"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>Olink error: no generated text for targetdoc/targetptr = <xsl:value-of select="@targetdoc"/>/<xsl:value-of select="@targetptr"/></xsl:message>
          <xsl:text>????</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text>????</xsl:text>
<!--
      <xsl:call-template name="olink.outline">
        <xsl:with-param name="outline.base.uri"
                        select="unparsed-entity-uri(@targetdocent)"/>
        <xsl:with-param name="localinfo" select="@localinfo"/>
        <xsl:with-param name="return" select="'xreftext'"/>
      </xsl:call-template>
-->
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="olink.outline">
  <xsl:message terminate="yes">Fatal error: what is this supposed to do?</xsl:message>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="title.xref">
  <xsl:param name="target" select="."/>
  <xsl:choose>
    <xsl:when test="local-name($target) = 'figure'
                    or local-name($target) = 'example'
                    or local-name($target) = 'equation'
                    or local-name($target) = 'table'
                    or local-name($target) = 'dedication'
                    or local-name($target) = 'preface'
                    or local-name($target) = 'bibliography'
                    or local-name($target) = 'glossary'
                    or local-name($target) = 'index'
                    or local-name($target) = 'setindex'
                    or local-name($target) = 'colophon'">
      <xsl:call-template name="gentext.startquote"/>
      <xsl:apply-templates select="$target" mode="title.markup"/>
      <xsl:call-template name="gentext.endquote"/>
    </xsl:when>
    <xsl:otherwise>
      <fo:inline font-style="italic">
        <xsl:apply-templates select="$target" mode="title.markup"/>
      </fo:inline>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="number.xref">
  <xsl:param name="target" select="."/>
  <xsl:apply-templates select="$target" mode="label.markup"/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="xref.xreflabel">
  <!-- called to process an xreflabel...you might use this to make  -->
  <!-- xreflabels come out in the right font for different targets, -->
  <!-- for example. -->
  <xsl:param name="target" select="."/>
  <xsl:value-of select="$target/@xreflabel"/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="title" mode="xref">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="command" mode="xref">
  <xsl:call-template name="inline.boldseq"/>
</xsl:template>

<xsl:template match="function" mode="xref">
  <xsl:call-template name="inline.monoseq"/>
</xsl:template>

<xsl:template match="*" mode="page.citation">
  <xsl:param name="id" select="'???'"/>

  <fo:inline keep-together.within-line="always">
    <xsl:call-template name="substitute-markup">
      <xsl:with-param name="template">
        <xsl:call-template name="gentext.template">
          <xsl:with-param name="name" select="'page.citation'"/>
          <xsl:with-param name="context" select="'xref'"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
  </fo:inline>
</xsl:template>

<xsl:template match="*" mode="pagenumber.markup">
  <fo:page-number-citation ref-id="{@id}"/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="*" mode="insert.title.markup">
  <xsl:param name="purpose"/>
  <xsl:param name="xrefstyle"/>
  <xsl:param name="title"/>

  <xsl:choose>
    <!-- FIXME: what about the case where titleabbrev is inside the info? -->
    <xsl:when test="$purpose = 'xref' and titleabbrev">
      <xsl:apply-templates select="." mode="titleabbrev.markup"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$title"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="chapter|appendix" mode="insert.title.markup">
  <xsl:param name="purpose"/>
  <xsl:param name="xrefstyle"/>
  <xsl:param name="title"/>

  <xsl:choose>
    <xsl:when test="$purpose = 'xref'">
      <fo:inline font-style="italic">
        <xsl:copy-of select="$title"/>
      </fo:inline>
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$title"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*" mode="insert.subtitle.markup">
  <xsl:param name="purpose"/>
  <xsl:param name="xrefstyle"/>
  <xsl:param name="subtitle"/>

  <xsl:copy-of select="$subtitle"/>
</xsl:template>

<xsl:template match="*" mode="insert.label.markup">
  <xsl:param name="purpose"/>
  <xsl:param name="xrefstyle"/>
  <xsl:param name="label"/>

  <xsl:copy-of select="$label"/>
</xsl:template>

<xsl:template match="*" mode="insert.pagenumber.markup">
  <xsl:param name="purpose"/>
  <xsl:param name="xrefstyle"/>
  <xsl:param name="pagenumber"/>

  <xsl:copy-of select="$pagenumber"/>
</xsl:template>

<xsl:template match="*" mode="insert.direction.markup">
  <xsl:param name="purpose"/>
  <xsl:param name="xrefstyle"/>
  <xsl:param name="direction"/>

  <xsl:copy-of select="$direction"/>
</xsl:template>

</xsl:stylesheet>
