<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:exsl="http://exslt.org/common"
		version="1.0"
                exclude-result-prefixes="exsl">

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:param name="onechunk" select="0"/>
<xsl:param name="refentry.separator" select="0"/>
<xsl:param name="chunk.fast" select="0"/>

<xsl:key name="genid" match="*" use="generate-id()"/>

<!-- ==================================================================== -->

<xsl:variable name="chunk.hierarchy">
  <xsl:if test="$chunk.fast != 0">
    <xsl:choose>
      <xsl:when test="function-available('exsl:node-set')">
        <xsl:message>Computing chunks...</xsl:message>
        <xsl:apply-templates select="/*" mode="find.chunks"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:message>
          <xsl:text>Fast chunking requires exsl:node-set(). </xsl:text>
          <xsl:text>Using "slow" chunking.</xsl:text>
        </xsl:message>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:variable>

<xsl:template match="*" mode="find.chunks">
  <xsl:variable name="chunk">
    <xsl:call-template name="chunk"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="$chunk != 0">
      <div class="{local-name(.)}" id="{generate-id()}">
        <xsl:apply-templates select="*" mode="find.chunks"/>
      </div>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="*" mode="find.chunks"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="process-chunk-element">
  <xsl:param name="content">
    <xsl:apply-imports/>
  </xsl:param>

  <xsl:choose>
    <xsl:when test="$chunk.fast != 0 and function-available('exsl:node-set')">
      <xsl:variable name="chunks" select="exsl:node-set($chunk.hierarchy)//div"/>
      <xsl:variable name="genid" select="generate-id()"/>

      <xsl:variable name="div" select="$chunks[@id=$genid]"/>

      <xsl:variable name="prevdiv"
                    select="($div/preceding-sibling::div|$div/preceding::div|$div/parent::div)[last()]"/>
      <xsl:variable name="prev" select="key('genid', $prevdiv/@id)"/>

      <xsl:variable name="nextdiv"
                    select="($div/following-sibling::div|$div/following::div|$div/div)[1]"/>
      <xsl:variable name="next" select="key('genid', $nextdiv/@id)"/>

      <xsl:choose>
        <xsl:when test="$onechunk != 0 and parent::*">
          <xsl:copy-of select="$content"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="process-chunk">
            <xsl:with-param name="prev" select="$prev"/>
            <xsl:with-param name="next" select="$next"/>
            <xsl:with-param name="content" select="$content"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="$onechunk != 0 and not(parent::*)">
          <xsl:call-template name="chunk-all-sections">
            <xsl:with-param name="content" select="$content"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$onechunk != 0">
          <xsl:copy-of select="$content"/>
        </xsl:when>
        <xsl:when test="$chunk.first.sections = 0">
          <xsl:call-template name="chunk-first-section-with-parent">
            <xsl:with-param name="content" select="$content"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="chunk-all-sections">
            <xsl:with-param name="content" select="$content"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="process-chunk">
  <xsl:param name="prev" select="."/>
  <xsl:param name="next" select="."/>
  <xsl:param name="content">
    <xsl:apply-imports/>
  </xsl:param>

  <xsl:variable name="ischunk">
    <xsl:call-template name="chunk"/>
  </xsl:variable>

  <xsl:variable name="chunkfn">
    <xsl:if test="$ischunk='1'">
      <xsl:apply-templates mode="chunk-filename" select="."/>
    </xsl:if>
  </xsl:variable>

  <xsl:if test="$ischunk='0'">
    <xsl:message>
      <xsl:text>Error </xsl:text>
      <xsl:value-of select="name(.)"/>
      <xsl:text> is not a chunk!</xsl:text>
    </xsl:message>
  </xsl:if>

  <xsl:variable name="filename">
    <xsl:call-template name="make-relative-filename">
      <xsl:with-param name="base.dir" select="$base.dir"/>
      <xsl:with-param name="base.name" select="$chunkfn"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:call-template name="write.chunk">
    <xsl:with-param name="filename" select="$filename"/>
    <xsl:with-param name="content">
      <xsl:call-template name="chunk-element-content">
        <xsl:with-param name="prev" select="$prev"/>
        <xsl:with-param name="next" select="$next"/>
        <xsl:with-param name="content" select="$content"/>
      </xsl:call-template>
    </xsl:with-param>
    <xsl:with-param name="quiet" select="$chunk.quietly"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="chunk-first-section-with-parent">
  <xsl:param name="content">
    <xsl:apply-imports/>
  </xsl:param>

  <!-- These xpath expressions are really hairy. The trick is to pick sections -->
  <!-- that are not first children and are not the children of first children -->

  <!-- Break these variables into pieces to work around
       http://nagoya.apache.org/bugzilla/show_bug.cgi?id=6063 -->

  <xsl:variable name="prev-v1"
     select="(ancestor::sect1[$chunk.section.depth &gt; 0
                               and preceding-sibling::sect1][1]

             |ancestor::sect2[$chunk.section.depth &gt; 1
                               and preceding-sibling::sect2
                               and parent::sect1[preceding-sibling::sect1]][1]

             |ancestor::sect3[$chunk.section.depth &gt; 2
                               and preceding-sibling::sect3
                               and parent::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |ancestor::sect4[$chunk.section.depth &gt; 3
                               and preceding-sibling::sect4
                               and parent::sect3[preceding-sibling::sect2]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |ancestor::sect5[$chunk.section.depth &gt; 4
                               and preceding-sibling::sect5
                               and parent::sect4[preceding-sibling::sect4]
                               and ancestor::sect3[preceding-sibling::sect3]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |ancestor::section[$chunk.section.depth &gt; count(ancestor::section)
                                and not(ancestor::section[not(preceding-sibling::section)])][1])[last()]"/>

  <xsl:variable name="prev-v2"
     select="(preceding::sect1[$chunk.section.depth &gt; 0
                               and preceding-sibling::sect1][1]

             |preceding::sect2[$chunk.section.depth &gt; 1
                               and preceding-sibling::sect2
                               and parent::sect1[preceding-sibling::sect1]][1]

             |preceding::sect3[$chunk.section.depth &gt; 2
                               and preceding-sibling::sect3
                               and parent::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |preceding::sect4[$chunk.section.depth &gt; 3
                               and preceding-sibling::sect4
                               and parent::sect3[preceding-sibling::sect2]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |preceding::sect5[$chunk.section.depth &gt; 4
                               and preceding-sibling::sect5
                               and parent::sect4[preceding-sibling::sect4]
                               and ancestor::sect3[preceding-sibling::sect3]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |preceding::section[$chunk.section.depth &gt; count(ancestor::section)
                                 and preceding-sibling::section
                                 and not(ancestor::section[not(preceding-sibling::section)])][1])[last()]"/>

  <xsl:variable name="prev"
    select="(preceding::book[1]
             |preceding::preface[1]
             |preceding::chapter[1]
             |preceding::appendix[1]
             |preceding::part[1]
             |preceding::reference[1]
             |preceding::refentry[1]
             |preceding::colophon[1]
             |preceding::article[1]
             |preceding::bibliography[1]
             |preceding::glossary[1]
             |preceding::index[$generate.index != 0][1]
             |preceding::setindex[$generate.index != 0][1]
             |ancestor::set
             |ancestor::book[1]
             |ancestor::preface[1]
             |ancestor::chapter[1]
             |ancestor::appendix[1]
             |ancestor::part[1]
             |ancestor::reference[1]
             |ancestor::article[1]
             |$prev-v1
             |$prev-v2)[last()]"/>

  <xsl:variable name="next-v1"
    select="(following::sect1[$chunk.section.depth &gt; 0
                               and preceding-sibling::sect1][1]

             |following::sect2[$chunk.section.depth &gt; 1
                               and preceding-sibling::sect2
                               and parent::sect1[preceding-sibling::sect1]][1]

             |following::sect3[$chunk.section.depth &gt; 2
                               and preceding-sibling::sect3
                               and parent::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |following::sect4[$chunk.section.depth &gt; 3
                               and preceding-sibling::sect4
                               and parent::sect3[preceding-sibling::sect2]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |following::sect5[$chunk.section.depth &gt; 4
                               and preceding-sibling::sect5
                               and parent::sect4[preceding-sibling::sect4]
                               and ancestor::sect3[preceding-sibling::sect3]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |following::section[$chunk.section.depth &gt; count(ancestor::section)
                                 and preceding-sibling::section
                                 and not(ancestor::section[not(preceding-sibling::section)])][1])[1]"/>

  <xsl:variable name="next-v2"
    select="(descendant::sect1[$chunk.section.depth &gt; 0
                               and preceding-sibling::sect1][1]

             |descendant::sect2[$chunk.section.depth &gt; 1
                               and preceding-sibling::sect2
                               and parent::sect1[preceding-sibling::sect1]][1]

             |descendant::sect3[$chunk.section.depth &gt; 2
                               and preceding-sibling::sect3
                               and parent::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |descendant::sect4[$chunk.section.depth &gt; 3
                               and preceding-sibling::sect4
                               and parent::sect3[preceding-sibling::sect2]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |descendant::sect5[$chunk.section.depth &gt; 4
                               and preceding-sibling::sect5
                               and parent::sect4[preceding-sibling::sect4]
                               and ancestor::sect3[preceding-sibling::sect3]
                               and ancestor::sect2[preceding-sibling::sect2]
                               and ancestor::sect1[preceding-sibling::sect1]][1]

             |descendant::section[$chunk.section.depth &gt; count(ancestor::section)
                                 and preceding-sibling::section
                                 and not(ancestor::section[not(preceding-sibling::section)])])[1]"/>

  <xsl:variable name="next"
    select="(following::book[1]
             |following::preface[1]
             |following::chapter[1]
             |following::appendix[1]
             |following::part[1]
             |following::reference[1]
             |following::refentry[1]
             |following::colophon[1]
             |following::bibliography[1]
             |following::glossary[1]
             |following::index[$generate.index != 0][1]
             |following::article[1]
             |following::setindex[$generate.index != 0][1]
             |descendant::book[1]
             |descendant::preface[1]
             |descendant::chapter[1]
             |descendant::appendix[1]
             |descendant::article[1]
             |descendant::bibliography[1]
             |descendant::glossary[1]
             |descendant::index[$generate.index != 0][1]
             |descendant::colophon[1]
             |descendant::setindex[$generate.index != 0][1]
             |descendant::part[1]
             |descendant::reference[1]
             |descendant::refentry[1]
             |$next-v1
             |$next-v2)[1]"/>

  <xsl:call-template name="process-chunk">
    <xsl:with-param name="prev" select="$prev"/>
    <xsl:with-param name="next" select="$next"/>
    <xsl:with-param name="content" select="$content"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="chunk-all-sections">
  <xsl:param name="content">
    <xsl:apply-imports/>
  </xsl:param>

  <xsl:variable name="prev-v1"
    select="(preceding::sect1[$chunk.section.depth &gt; 0][1]
             |preceding::sect2[$chunk.section.depth &gt; 1][1]
             |preceding::sect3[$chunk.section.depth &gt; 2][1]
             |preceding::sect4[$chunk.section.depth &gt; 3][1]
             |preceding::sect5[$chunk.section.depth &gt; 4][1]
             |preceding::section[$chunk.section.depth &gt; count(ancestor::section)][1])[last()]"/>

  <xsl:variable name="prev-v2"
    select="(ancestor::sect1[$chunk.section.depth &gt; 0][1]
             |ancestor::sect2[$chunk.section.depth &gt; 1][1]
             |ancestor::sect3[$chunk.section.depth &gt; 2][1]
             |ancestor::sect4[$chunk.section.depth &gt; 3][1]
             |ancestor::sect5[$chunk.section.depth &gt; 4][1]
             |ancestor::section[$chunk.section.depth &gt; count(ancestor::section)][1])[last()]"/>

  <xsl:variable name="prev"
    select="(preceding::book[1]
             |preceding::preface[1]
             |preceding::chapter[1]
             |preceding::appendix[1]
             |preceding::part[1]
             |preceding::reference[1]
             |preceding::refentry[1]
             |preceding::colophon[1]
             |preceding::article[1]
             |preceding::bibliography[1]
             |preceding::glossary[1]
             |preceding::index[$generate.index != 0][1]
             |preceding::setindex[$generate.index != 0][1]
             |ancestor::set
             |ancestor::book[1]
             |ancestor::preface[1]
             |ancestor::chapter[1]
             |ancestor::appendix[1]
             |ancestor::part[1]
             |ancestor::reference[1]
             |ancestor::article[1]
             |$prev-v1
             |$prev-v2)[last()]"/>

  <xsl:variable name="next-v1"
    select="(following::sect1[$chunk.section.depth &gt; 0][1]
             |following::sect2[$chunk.section.depth &gt; 1][1]
             |following::sect3[$chunk.section.depth &gt; 2][1]
             |following::sect4[$chunk.section.depth &gt; 3][1]
             |following::sect5[$chunk.section.depth &gt; 4][1]
             |following::section[$chunk.section.depth &gt; count(ancestor::section)][1])[1]"/>

  <xsl:variable name="next-v2"
    select="(descendant::sect1[$chunk.section.depth &gt; 0][1]
             |descendant::sect2[$chunk.section.depth &gt; 1][1]
             |descendant::sect3[$chunk.section.depth &gt; 2][1]
             |descendant::sect4[$chunk.section.depth &gt; 3][1]
             |descendant::sect5[$chunk.section.depth &gt; 4][1]
             |descendant::section[$chunk.section.depth 
                                  &gt; count(ancestor::section)][1])[1]"/>

  <xsl:variable name="next"
    select="(following::book[1]
             |following::preface[1]
             |following::chapter[1]
             |following::appendix[1]
             |following::part[1]
             |following::reference[1]
             |following::refentry[1]
             |following::colophon[1]
             |following::bibliography[1]
             |following::glossary[1]
             |following::index[$generate.index != 0][1]
             |following::article[1]
             |following::setindex[$generate.index != 0][1]
             |descendant::book[1]
             |descendant::preface[1]
             |descendant::chapter[1]
             |descendant::appendix[1]
             |descendant::article[1]
             |descendant::bibliography[1]
             |descendant::glossary[1]
             |descendant::index[$generate.index != 0][1]
             |descendant::colophon[1]
             |descendant::setindex[$generate.index != 0][1]
             |descendant::part[1]
             |descendant::reference[1]
             |descendant::refentry[1]
             |$next-v1
             |$next-v2)[1]"/>

  <xsl:call-template name="process-chunk">
    <xsl:with-param name="prev" select="$prev"/>
    <xsl:with-param name="next" select="$next"/>
    <xsl:with-param name="content" select="$content"/>
  </xsl:call-template>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="/">
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
          <xsl:if test="$collect.xref.targets = 'yes' or
                        $collect.xref.targets = 'only'">
            <xsl:apply-templates select="key('id', $rootid)"
                        mode="collect.targets"/>
          </xsl:if>
          <xsl:if test="$collect.xref.targets != 'only'">
            <xsl:apply-templates select="key('id',$rootid)"
                        mode="process.root"/>
            <xsl:if test="$tex.math.in.alt != ''">
              <xsl:apply-templates select="key('id',$rootid)"
                          mode="collect.tex.math"/>
            </xsl:if>
            <xsl:if test="$generate.manifest != 0">
              <xsl:call-template name="generate.manifest">
                <xsl:with-param name="node" select="key('id',$rootid)"/>
              </xsl:call-template>
            </xsl:if>
          </xsl:if>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="$collect.xref.targets = 'yes' or
                    $collect.xref.targets = 'only'">
        <xsl:apply-templates select="/" mode="collect.targets"/>
      </xsl:if>
      <xsl:if test="$collect.xref.targets != 'only'">
        <xsl:apply-templates select="/" mode="process.root"/>
        <xsl:if test="$tex.math.in.alt != ''">
          <xsl:apply-templates select="/" mode="collect.tex.math"/>
        </xsl:if>
        <xsl:if test="$generate.manifest != 0">
          <xsl:call-template name="generate.manifest"/>
        </xsl:if>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="*" mode="process.root">
  <xsl:apply-templates select="."/>
</xsl:template>

<!-- ====================================================================== -->

<xsl:template match="set|book|part|preface|chapter|appendix
                     |article
                     |reference|refentry
                     |book/glossary|article/glossary|part/glossary
                     |book/bibliography|article/bibliography
                     |colophon">
  <xsl:choose>
    <xsl:when test="$onechunk != 0 and parent::*">
      <xsl:apply-imports/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="process-chunk-element"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="sect1|sect2|sect3|sect4|sect5|section">
  <xsl:variable name="ischunk">
    <xsl:call-template name="chunk"/>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="not(parent::*)">
      <xsl:call-template name="process-chunk-element"/>
    </xsl:when>
    <xsl:when test="$ischunk = 0">
      <xsl:apply-imports/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="process-chunk-element"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="setindex
                     |book/index
                     |article/index">
  <!-- some implementations use completely empty index tags to indicate -->
  <!-- where an automatically generated index should be inserted. so -->
  <!-- if the index is completely empty, skip it. -->
  <xsl:if test="count(*)>0 or $generate.index != '0'">
    <xsl:call-template name="process-chunk-element"/>
  </xsl:if>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="make.lots">
  <xsl:param name="toc.params" select="''"/>
  <xsl:param name="toc"/>

  <xsl:variable name="lots">
    <xsl:if test="contains($toc.params, 'toc')">
      <xsl:copy-of select="$toc"/>
    </xsl:if>

    <xsl:if test="contains($toc.params, 'figure')">
      <xsl:call-template name="list.of.titles">
        <xsl:with-param name="titles" select="'figure'"/>
        <xsl:with-param name="nodes" select=".//figure"/>
      </xsl:call-template>
    </xsl:if>

    <xsl:if test="contains($toc.params, 'table')">
      <xsl:call-template name="list.of.titles">
        <xsl:with-param name="titles" select="'table'"/>
        <xsl:with-param name="nodes" select=".//table"/>
      </xsl:call-template>
    </xsl:if>

    <xsl:if test="contains($toc.params, 'example')">
      <xsl:call-template name="list.of.titles">
        <xsl:with-param name="titles" select="'example'"/>
        <xsl:with-param name="nodes" select=".//example"/>
      </xsl:call-template>
    </xsl:if>

    <xsl:if test="contains($toc.params, 'equation')">
      <xsl:call-template name="list.of.titles">
        <xsl:with-param name="titles" select="'equation'"/>
        <xsl:with-param name="nodes" select=".//equation[title]"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:variable>

  <xsl:if test="string($lots) != ''">
    <xsl:choose>
      <xsl:when test="$chunk.tocs.and.lots != 0 and not(parent::*)">
        <xsl:call-template name="write.chunk">
          <xsl:with-param name="filename">
            <xsl:call-template name="make-relative-filename">
              <xsl:with-param name="base.dir" select="$base.dir"/>
              <xsl:with-param name="base.name">
                <xsl:call-template name="dbhtml-dir"/>
                <xsl:apply-templates select="." mode="recursive-chunk-filename"/>
                <xsl:text>-toc</xsl:text>
                <xsl:value-of select="$html.ext"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:with-param>
          <xsl:with-param name="content">
            <xsl:call-template name="chunk-element-content">
              <xsl:with-param name="prev" select="/foo"/>
              <xsl:with-param name="next" select="/foo"/>
              <xsl:with-param name="nav.context" select="'toc'"/>
              <xsl:with-param name="content">
                <h1>
                  <xsl:apply-templates select="." mode="object.title.markup"/>
                </h1>
                <xsl:copy-of select="$lots"/>
              </xsl:with-param>
            </xsl:call-template>
          </xsl:with-param>
          <xsl:with-param name="quiet" select="$chunk.quietly"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:copy-of select="$lots"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:if>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="in.other.chunk">
  <xsl:param name="chunk" select="."/>
  <xsl:param name="node" select="."/>

  <xsl:variable name="is.chunk">
    <xsl:call-template name="chunk">
      <xsl:with-param name="node" select="$node"/>
    </xsl:call-template>
  </xsl:variable>

<!--
  <xsl:message>
    <xsl:text>in.other.chunk: </xsl:text>
    <xsl:value-of select="name($chunk)"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="name($node)"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="$chunk = $node"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="$is.chunk"/>
  </xsl:message>
-->

  <xsl:choose>
    <xsl:when test="$chunk = $node">0</xsl:when>
    <xsl:when test="$is.chunk = 1">1</xsl:when>
    <xsl:when test="count($node) = 0">0</xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="in.other.chunk">
        <xsl:with-param name="chunk" select="$chunk"/>
        <xsl:with-param name="node" select="$node/parent::*"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="count.footnotes.in.this.chunk">
  <xsl:param name="node" select="."/>
  <xsl:param name="footnotes" select="$node//footnote"/>
  <xsl:param name="count" select="0"/>

<!--
  <xsl:message>
    <xsl:text>count.footnotes.in.this.chunk: </xsl:text>
    <xsl:value-of select="name($node)"/>
  </xsl:message>
-->

  <xsl:variable name="in.other.chunk">
    <xsl:call-template name="in.other.chunk">
      <xsl:with-param name="chunk" select="$node"/>
      <xsl:with-param name="node" select="$footnotes[1]"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="count($footnotes) = 0">
      <xsl:value-of select="$count"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="$in.other.chunk != 0">
          <xsl:call-template name="count.footnotes.in.this.chunk">
            <xsl:with-param name="node" select="$node"/>
            <xsl:with-param name="footnotes"
                            select="$footnotes[position() &gt; 1]"/>
            <xsl:with-param name="count" select="$count"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$footnotes[1]/ancestor::table
                        |$footnotes[1]/ancestor::informaltable">
          <xsl:call-template name="count.footnotes.in.this.chunk">
            <xsl:with-param name="node" select="$node"/>
            <xsl:with-param name="footnotes"
                            select="$footnotes[position() &gt; 1]"/>
            <xsl:with-param name="count" select="$count"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="count.footnotes.in.this.chunk">
            <xsl:with-param name="node" select="$node"/>
            <xsl:with-param name="footnotes"
                            select="$footnotes[position() &gt; 1]"/>
            <xsl:with-param name="count" select="$count + 1"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="process.footnotes.in.this.chunk">
  <xsl:param name="node" select="."/>
  <xsl:param name="footnotes" select="$node//footnote"/>

<!--
  <xsl:message>process.footnotes.in.this.chunk</xsl:message>
-->

  <xsl:variable name="in.other.chunk">
    <xsl:call-template name="in.other.chunk">
      <xsl:with-param name="chunk" select="$node"/>
      <xsl:with-param name="node" select="$footnotes[1]"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:choose>
    <xsl:when test="count($footnotes) = 0">
      <!-- nop -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:choose>
        <xsl:when test="$in.other.chunk != 0">
          <xsl:call-template name="process.footnotes.in.this.chunk">
            <xsl:with-param name="node" select="$node"/>
            <xsl:with-param name="footnotes"
                            select="$footnotes[position() &gt; 1]"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:when test="$footnotes[1]/ancestor::table
                        |$footnotes[1]/ancestor::informaltable">
          <xsl:call-template name="process.footnotes.in.this.chunk">
            <xsl:with-param name="node" select="$node"/>
            <xsl:with-param name="footnotes"
                            select="$footnotes[position() &gt; 1]"/>
          </xsl:call-template>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="$footnotes[1]"
                               mode="process.footnote.mode"/>
          <xsl:call-template name="process.footnotes.in.this.chunk">
            <xsl:with-param name="node" select="$node"/>
            <xsl:with-param name="footnotes"
                            select="$footnotes[position() &gt; 1]"/>
          </xsl:call-template>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="process.footnotes">
  <xsl:variable name="footnotes" select=".//footnote"/>
  <xsl:variable name="fcount">
    <xsl:call-template name="count.footnotes.in.this.chunk">
      <xsl:with-param name="node" select="."/>
      <xsl:with-param name="footnotes" select="$footnotes"/>
    </xsl:call-template>
  </xsl:variable>

<!--
  <xsl:message>
    <xsl:value-of select="name(.)"/>
    <xsl:text> fcount: </xsl:text>
    <xsl:value-of select="$fcount"/>
  </xsl:message>
-->

  <!-- Only bother to do this if there's at least one non-table footnote -->
  <xsl:if test="$fcount &gt; 0">
    <div class="footnotes">
      <br/>
      <hr width="100" align="left"/>
      <xsl:call-template name="process.footnotes.in.this.chunk">
        <xsl:with-param name="node" select="."/>
        <xsl:with-param name="footnotes" select="$footnotes"/>
      </xsl:call-template>
    </div>
  </xsl:if>
</xsl:template>

<xsl:template name="process.chunk.footnotes">
  <xsl:variable name="is.chunk">
    <xsl:call-template name="chunk"/>
  </xsl:variable>
  <xsl:if test="$is.chunk = 1">
    <xsl:call-template name="process.footnotes"/>
  </xsl:if>
</xsl:template>

<!-- ====================================================================== -->

</xsl:stylesheet>
