<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0">

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the DocBook XSL Stylesheet distribution.
     See ../README or http://docbook.sf.net/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<xsl:param name="body.fontset">
  <xsl:value-of select="$body.font.family"/>
  <xsl:if test="$body.font.family != ''
                and $symbol.font.family  != ''">,</xsl:if>
    <xsl:value-of select="$symbol.font.family"/>
</xsl:param>

<xsl:param name="title.fontset">
  <xsl:value-of select="$title.font.family"/>
  <xsl:if test="$title.font.family != ''
                and $symbol.font.family  != ''">,</xsl:if>
    <xsl:value-of select="$symbol.font.family"/>
</xsl:param>

<!-- PassiveTeX can't handle the math expression for
     title.margin.left being negative, so ignore it.
     margin-left="{$page.margin.outer} - {$title.margin.left}"
-->
<xsl:param name="margin.left.outer">
  <xsl:choose>
    <xsl:when test="$passivetex.extensions != 0">
      <xsl:value-of select="$page.margin.outer"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$page.margin.outer"/>
      <xsl:text> - </xsl:text>
      <xsl:value-of select="$title.margin.left"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:param>

<xsl:param name="margin.left.inner">
  <xsl:choose>
    <xsl:when test="$passivetex.extensions != 0">
      <xsl:value-of select="$page.margin.inner"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$page.margin.inner"/>
      <xsl:text> - </xsl:text>
      <xsl:value-of select="$title.margin.left"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:param>

<xsl:template name="setup.pagemasters">
  <fo:layout-master-set>
    <!-- blank pages -->
    <fo:simple-page-master master-name="blank"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body display-align="center"
                      margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}">
        <xsl:if test="$fop.extensions = 0">
          <xsl:attribute name="region-name">blank-body</xsl:attribute>
        </xsl:if>
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-blank"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-blank"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <!-- title pages -->
    <fo:simple-page-master master-name="titlepage-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.titlepage}"
                      column-count="{$column.count.titlepage}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="titlepage-odd"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.titlepage}"
                      column-count="{$column.count.titlepage}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-odd"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-odd"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="titlepage-even"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.titlepage}"
                      column-count="{$column.count.titlepage}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-even"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-even"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <!-- list-of-title pages -->
    <fo:simple-page-master master-name="lot-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.lot}"
                      column-count="{$column.count.lot}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="lot-odd"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.lot}"
                      column-count="{$column.count.lot}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-odd"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-odd"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="lot-even"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.lot}"
                      column-count="{$column.count.lot}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-even"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-even"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <!-- frontmatter pages -->
    <fo:simple-page-master master-name="front-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.front}"
                      column-count="{$column.count.front}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="front-odd"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.front}"
                      column-count="{$column.count.front}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-odd"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-odd"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="front-even"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.front}"
                      column-count="{$column.count.front}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-even"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-even"
                       extent="{$region.after.extent}"
                        display-align="after"/>
    </fo:simple-page-master>

    <!-- body pages -->
    <fo:simple-page-master master-name="body-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.body}"
                      column-count="{$column.count.body}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="body-odd"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.body}"
                      column-count="{$column.count.body}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-odd"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-odd"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="body-even"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.body}"
                      column-count="{$column.count.body}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-even"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-even"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <!-- backmatter pages -->
    <fo:simple-page-master master-name="back-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.back}"
                      column-count="{$column.count.back}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="back-odd"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.back}"
                      column-count="{$column.count.back}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-odd"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-odd"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="back-even"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$margin.left.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.back}"
                      column-count="{$column.count.back}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-even"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-even"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <!-- index pages -->
    <fo:simple-page-master master-name="index-first"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$page.margin.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.index}"
                      column-count="{$column.count.index}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-first"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-first"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="index-odd"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$page.margin.inner}"
                           margin-right="{$page.margin.outer}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.index}"
                      column-count="{$column.count.index}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-odd"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-odd"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <fo:simple-page-master master-name="index-even"
                           page-width="{$page.width}"
                           page-height="{$page.height}"
                           margin-top="{$page.margin.top}"
                           margin-bottom="{$page.margin.bottom}"
                           margin-left="{$page.margin.outer}"
                           margin-right="{$page.margin.inner}">
      <fo:region-body margin-bottom="{$body.margin.bottom}"
                      margin-top="{$body.margin.top}"
                      column-gap="{$column.gap.index}"
                      column-count="{$column.count.index}">
      </fo:region-body>
      <fo:region-before region-name="xsl-region-before-even"
                        extent="{$region.before.extent}"
                        display-align="before"/>
      <fo:region-after region-name="xsl-region-after-even"
                       extent="{$region.after.extent}"
                       display-align="after"/>
    </fo:simple-page-master>

    <xsl:if test="$draft.mode != 'no'">
      <!-- draft blank pages -->
      <fo:simple-page-master master-name="blank-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.outer}"
                             margin-right="{$page.margin.inner}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-blank"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-blank"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <!-- draft title pages -->
      <fo:simple-page-master master-name="titlepage-first-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.titlepage}"
                        column-count="{$column.count.titlepage}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-first"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-first"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="titlepage-odd-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.titlepage}"
                        column-count="{$column.count.titlepage}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-odd"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-odd"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="titlepage-even-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.outer}"
                             margin-right="{$page.margin.inner}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.titlepage}"
                        column-count="{$column.count.titlepage}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-even"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-even"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <!-- draft list-of-title pages -->
      <fo:simple-page-master master-name="lot-first-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.lot}"
                        column-count="{$column.count.lot}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-first"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-first"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="lot-odd-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.lot}"
                        column-count="{$column.count.lot}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-odd"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-odd"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="lot-even-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.outer}"
                             margin-right="{$page.margin.inner}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.lot}"
                        column-count="{$column.count.lot}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-even"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-even"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <!-- draft frontmatter pages -->
      <fo:simple-page-master master-name="front-first-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.front}"
                        column-count="{$column.count.front}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-first"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-first"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="front-odd-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.front}"
                        column-count="{$column.count.front}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-odd"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-odd"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="front-even-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.outer}"
                             margin-right="{$page.margin.inner}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.front}"
                        column-count="{$column.count.front}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-even"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-even"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <!-- draft body pages -->
      <fo:simple-page-master master-name="body-first-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.body}"
                        column-count="{$column.count.body}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-first"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-first"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="body-odd-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.body}"
                        column-count="{$column.count.body}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-odd"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-odd"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="body-even-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.outer}"
                             margin-right="{$page.margin.inner}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.body}"
                        column-count="{$column.count.body}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-even"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-even"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <!-- draft backmatter pages -->
      <fo:simple-page-master master-name="back-first-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.back}"
                        column-count="{$column.count.back}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-first"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-first"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="back-odd-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.back}"
                        column-count="{$column.count.back}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-odd"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-odd"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="back-even-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$margin.left.outer}"
                             margin-right="{$page.margin.inner}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.back}"
                        column-count="{$column.count.back}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-even"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-even"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <!-- draft index pages -->
      <fo:simple-page-master master-name="index-first-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$page.margin.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.index}"
                        column-count="{$column.count.index}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-first"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-first"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="index-odd-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-left="{$page.margin.inner}"
                             margin-right="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.index}"
                        column-count="{$column.count.index}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-odd"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-odd"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>

      <fo:simple-page-master master-name="index-even-draft"
                             page-width="{$page.width}"
                             page-height="{$page.height}"
                             margin-top="{$page.margin.top}"
                             margin-bottom="{$page.margin.bottom}"
                             margin-right="{$page.margin.inner}"
                             margin-left="{$page.margin.outer}">
        <fo:region-body margin-bottom="{$body.margin.bottom}"
                        margin-top="{$body.margin.top}"
                        column-gap="{$column.gap.index}"
                        column-count="{$column.count.index}">
          <xsl:if test="$draft.watermark.image != ''">
            <xsl:attribute name="background-image">
              <xsl:call-template name="fo-external-image">
                <xsl:with-param name="filename" select="$draft.watermark.image"/>
              </xsl:call-template>
            </xsl:attribute>
            <xsl:attribute name="background-attachment">fixed</xsl:attribute>
            <xsl:attribute name="background-repeat">no-repeat</xsl:attribute>
            <xsl:attribute name="background-position-horizontal">center</xsl:attribute>
            <xsl:attribute name="background-position-vertical">center</xsl:attribute>
          </xsl:if>
        </fo:region-body>
        <fo:region-before region-name="xsl-region-before-even"
                          extent="{$region.before.extent}"
                          display-align="before"/>
        <fo:region-after region-name="xsl-region-after-even"
                         extent="{$region.after.extent}"
                         display-align="after"/>
      </fo:simple-page-master>
    </xsl:if>

    <!-- setup for title page(s) -->
    <fo:page-sequence-master master-name="titlepage">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="titlepage-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="titlepage-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference master-reference="titlepage-even"
                                              odd-or-even="even"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <!-- setup for lots -->
    <fo:page-sequence-master master-name="lot">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="lot-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="lot-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference master-reference="lot-even"
                                              odd-or-even="even"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <!-- setup front matter -->
    <fo:page-sequence-master master-name="front">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="front-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="front-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference master-reference="front-even"
                                              odd-or-even="even"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <!-- setup for body pages -->
    <fo:page-sequence-master master-name="body">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="body-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="body-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference master-reference="body-even"
                                              odd-or-even="even"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <!-- setup back matter -->
    <fo:page-sequence-master master-name="back">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="back-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="back-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference master-reference="back-even"
                                              odd-or-even="even"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <!-- setup back matter -->
    <fo:page-sequence-master master-name="index">
      <fo:repeatable-page-master-alternatives>
        <fo:conditional-page-master-reference master-reference="blank"
                                              blank-or-not-blank="blank"/>
        <fo:conditional-page-master-reference master-reference="index-first"
                                              page-position="first"/>
        <fo:conditional-page-master-reference master-reference="index-odd"
                                              odd-or-even="odd"/>
        <fo:conditional-page-master-reference master-reference="index-even"
                                              odd-or-even="even"/>
      </fo:repeatable-page-master-alternatives>
    </fo:page-sequence-master>

    <xsl:if test="$draft.mode != 'no'">
      <!-- setup for draft title page(s) -->
      <fo:page-sequence-master master-name="titlepage-draft">
        <fo:repeatable-page-master-alternatives>
          <fo:conditional-page-master-reference master-reference="blank-draft"
                                                blank-or-not-blank="blank"/>
          <fo:conditional-page-master-reference master-reference="titlepage-first-draft"
                                                page-position="first"/>
          <fo:conditional-page-master-reference master-reference="titlepage-odd-draft"
                                                odd-or-even="odd"/>
          <fo:conditional-page-master-reference master-reference="titlepage-even-draft"
                                                odd-or-even="even"/>
        </fo:repeatable-page-master-alternatives>
      </fo:page-sequence-master>

      <!-- setup for draft lots -->
      <fo:page-sequence-master master-name="lot-draft">
        <fo:repeatable-page-master-alternatives>
          <fo:conditional-page-master-reference master-reference="blank-draft"
                                                blank-or-not-blank="blank"/>
          <fo:conditional-page-master-reference master-reference="lot-first-draft"
                                                page-position="first"/>
          <fo:conditional-page-master-reference master-reference="lot-odd-draft"
                                                odd-or-even="odd"/>
          <fo:conditional-page-master-reference master-reference="lot-even-draft"
                                                odd-or-even="even"/>
        </fo:repeatable-page-master-alternatives>
      </fo:page-sequence-master>

      <!-- setup draft front matter -->
      <fo:page-sequence-master master-name="front-draft">
        <fo:repeatable-page-master-alternatives>
          <fo:conditional-page-master-reference master-reference="blank-draft"
                                                blank-or-not-blank="blank"/>
          <fo:conditional-page-master-reference master-reference="front-first-draft"
                                                page-position="first"/>
          <fo:conditional-page-master-reference master-reference="front-odd-draft"
                                                odd-or-even="odd"/>
          <fo:conditional-page-master-reference master-reference="front-even-draft"
                                                odd-or-even="even"/>
        </fo:repeatable-page-master-alternatives>
      </fo:page-sequence-master>

      <!-- setup for draft body pages -->
      <fo:page-sequence-master master-name="body-draft">
        <fo:repeatable-page-master-alternatives>
          <fo:conditional-page-master-reference master-reference="blank-draft"
                                                blank-or-not-blank="blank"/>
          <fo:conditional-page-master-reference master-reference="body-first-draft"
                                                page-position="first"/>
          <fo:conditional-page-master-reference master-reference="body-odd-draft"
                                                odd-or-even="odd"/>
          <fo:conditional-page-master-reference master-reference="body-even-draft"
                                                odd-or-even="even"/>
        </fo:repeatable-page-master-alternatives>
      </fo:page-sequence-master>

      <!-- setup draft back matter -->
      <fo:page-sequence-master master-name="back-draft">
        <fo:repeatable-page-master-alternatives>
          <fo:conditional-page-master-reference master-reference="blank-draft"
                                                blank-or-not-blank="blank"/>
          <fo:conditional-page-master-reference master-reference="back-first-draft"
                                                page-position="first"/>
          <fo:conditional-page-master-reference master-reference="back-odd-draft"
                                                odd-or-even="odd"/>
          <fo:conditional-page-master-reference master-reference="back-even-draft"
                                                odd-or-even="even"/>
        </fo:repeatable-page-master-alternatives>
      </fo:page-sequence-master>

      <!-- setup draft index pages -->
      <fo:page-sequence-master master-name="index-draft">
        <fo:repeatable-page-master-alternatives>
          <fo:conditional-page-master-reference master-reference="blank-draft"
                                                blank-or-not-blank="blank"/>
          <fo:conditional-page-master-reference master-reference="index-first-draft"
                                                page-position="first"/>
          <fo:conditional-page-master-reference master-reference="index-odd-draft"
                                                odd-or-even="odd"/>
          <fo:conditional-page-master-reference master-reference="index-even-draft"
                                                odd-or-even="even"/>
        </fo:repeatable-page-master-alternatives>
      </fo:page-sequence-master>
    </xsl:if>

    <xsl:call-template name="user.pagemasters"/>

    </fo:layout-master-set>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="user.pagemasters"/> <!-- intentionally empty -->

<!-- ==================================================================== -->

<xsl:template name="select.pagemaster">
  <xsl:param name="element" select="local-name(.)"/>
  <xsl:param name="pageclass" select="''"/>

  <xsl:variable name="pagemaster">
    <xsl:choose>
      <xsl:when test="$pageclass != ''">
        <xsl:value-of select="$pageclass"/>
      </xsl:when>
      <xsl:when test="$pageclass = 'lot'">lot</xsl:when>
      <xsl:when test="$element = 'dedication'">front</xsl:when>
      <xsl:when test="$element = 'preface'">front</xsl:when>
      <xsl:when test="$element = 'appendix'">back</xsl:when>
      <xsl:when test="$element = 'glossary'">back</xsl:when>
      <xsl:when test="$element = 'bibliography'">back</xsl:when>
      <xsl:when test="$element = 'index'">index</xsl:when>
      <xsl:when test="$element = 'colophon'">back</xsl:when>
      <xsl:otherwise>body</xsl:otherwise>
    </xsl:choose>

    <xsl:choose>
      <xsl:when test="$draft.mode = 'yes'">
        <xsl:text>-draft</xsl:text>
      </xsl:when>
      <xsl:when test="$draft.mode = 'no'">
        <!-- nop -->
      </xsl:when>
      <xsl:when test="ancestor-or-self::*[@status][1]/@status = 'draft'">
        <xsl:text>-draft</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <!-- nop -->
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:call-template name="select.user.pagemaster">
    <xsl:with-param name="element" select="$element"/>
    <xsl:with-param name="pageclass" select="$pageclass"/>
    <xsl:with-param name="default-pagemaster" select="$pagemaster"/>
  </xsl:call-template>
</xsl:template>

<xsl:template name="select.user.pagemaster">
  <xsl:param name="element"/>
  <xsl:param name="pageclass"/>
  <xsl:param name="default-pagemaster"/>

  <!-- by default, return the default. But if you've created your own
       pagemasters in user.pagemasters, you might want to select one here. -->
  <xsl:value-of select="$default-pagemaster"/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="head.sep.rule">
  <xsl:param name="pageclass"/>
  <xsl:param name="sequence"/>
  <xsl:param name="gentext-key"/>

  <xsl:if test="$header.rule != 0">
    <xsl:attribute name="border-bottom-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-bottom-style">solid</xsl:attribute>
    <xsl:attribute name="border-bottom-color">black</xsl:attribute>
  </xsl:if>
</xsl:template>

<xsl:template name="foot.sep.rule">
  <xsl:param name="pageclass"/>
  <xsl:param name="sequence"/>
  <xsl:param name="gentext-key"/>

  <xsl:if test="$footer.rule != 0">
    <xsl:attribute name="border-top-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-top-style">solid</xsl:attribute>
    <xsl:attribute name="border-top-color">black</xsl:attribute>
  </xsl:if>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="*" mode="running.head.mode">
  <xsl:param name="master-reference" select="'unknown'"/>
  <xsl:param name="gentext-key" select="name(.)"/>

  <!-- remove -draft from reference -->
  <xsl:variable name="pageclass">
    <xsl:choose>
      <xsl:when test="contains($master-reference, '-draft')">
        <xsl:value-of select="substring-before($master-reference, '-draft')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$master-reference"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:static-content flow-name="xsl-region-before-first">
    <fo:block xsl:use-attribute-sets="header.content.properties">
      <xsl:call-template name="header.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'first'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <fo:static-content flow-name="xsl-region-before-odd">
    <fo:block xsl:use-attribute-sets="header.content.properties">
      <xsl:call-template name="header.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'odd'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <fo:static-content flow-name="xsl-region-before-even">
    <fo:block xsl:use-attribute-sets="header.content.properties">
      <xsl:call-template name="header.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'even'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <fo:static-content flow-name="xsl-region-before-blank">
    <fo:block xsl:use-attribute-sets="header.content.properties">
      <xsl:call-template name="header.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'blank'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <xsl:if test="$fop.extensions = 0">
    <xsl:call-template name="footnote-separator"/>
    <xsl:call-template name="blank.page.content"/>
  </xsl:if>
</xsl:template>

<xsl:template name="footnote-separator">
  <fo:static-content flow-name="xsl-footnote-separator">
    <fo:block>
      <fo:leader color="black" leader-pattern="rule" leader-length="1in"/>
    </fo:block>
  </fo:static-content>
</xsl:template>

<xsl:template name="blank.page.content">
  <fo:static-content flow-name="blank-body">
    <fo:block text-align="center"/>
  </fo:static-content>
</xsl:template>

<xsl:template name="header.table">
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="gentext-key" select="''"/>

  <!-- default is a single table style for all headers -->
  <!-- Customize it for different page classes or sequence location -->

  <xsl:choose>
      <xsl:when test="$pageclass = 'index'">
          <xsl:attribute name="margin-left">0pt</xsl:attribute>
      </xsl:when>
  </xsl:choose>

  <xsl:variable name="column1">
    <xsl:choose>
      <xsl:when test="$sequence = 'first' or $sequence = 'odd'">1</xsl:when>
      <xsl:otherwise>3</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="column3">
    <xsl:choose>
      <xsl:when test="$sequence = 'first' or $sequence = 'odd'">3</xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="candidate">
    <fo:table table-layout="fixed" width="100%">
      <xsl:call-template name="head.sep.rule">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="$sequence"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>

      <fo:table-column column-number="1">
        <xsl:attribute name="column-width">
          <xsl:text>proportional-column-width(</xsl:text>
          <xsl:call-template name="header.footer.width">
            <xsl:with-param name="location">header</xsl:with-param>
            <xsl:with-param name="position" select="$column1"/>
          </xsl:call-template>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </fo:table-column>
      <fo:table-column column-number="2">
        <xsl:attribute name="column-width">
          <xsl:text>proportional-column-width(</xsl:text>
          <xsl:call-template name="header.footer.width">
            <xsl:with-param name="location">header</xsl:with-param>
            <xsl:with-param name="position" select="2"/>
          </xsl:call-template>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </fo:table-column>
      <fo:table-column column-number="3">
        <xsl:attribute name="column-width">
          <xsl:text>proportional-column-width(</xsl:text>
          <xsl:call-template name="header.footer.width">
            <xsl:with-param name="location">header</xsl:with-param>
            <xsl:with-param name="position" select="$column3"/>
          </xsl:call-template>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </fo:table-column>

      <fo:table-body>
        <fo:table-row height="14pt">
          <fo:table-cell text-align="left"
                         display-align="before">
            <xsl:if test="$fop.extensions = 0">
              <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
            <fo:block>
              <xsl:call-template name="header.content">
                <xsl:with-param name="pageclass" select="$pageclass"/>
                <xsl:with-param name="sequence" select="$sequence"/>
                <xsl:with-param name="position" select="'left'"/>
                <xsl:with-param name="gentext-key" select="$gentext-key"/>
              </xsl:call-template>
            </fo:block>
          </fo:table-cell>
          <fo:table-cell text-align="center"
                         display-align="before">
            <xsl:if test="$fop.extensions = 0">
              <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
            <fo:block>
              <xsl:call-template name="header.content">
                <xsl:with-param name="pageclass" select="$pageclass"/>
                <xsl:with-param name="sequence" select="$sequence"/>
                <xsl:with-param name="position" select="'center'"/>
                <xsl:with-param name="gentext-key" select="$gentext-key"/>
              </xsl:call-template>
            </fo:block>
          </fo:table-cell>
          <fo:table-cell text-align="right"
                         display-align="before">
            <xsl:if test="$fop.extensions = 0">
              <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
            <fo:block>
              <xsl:call-template name="header.content">
                <xsl:with-param name="pageclass" select="$pageclass"/>
                <xsl:with-param name="sequence" select="$sequence"/>
                <xsl:with-param name="position" select="'right'"/>
                <xsl:with-param name="gentext-key" select="$gentext-key"/>
              </xsl:call-template>
            </fo:block>
          </fo:table-cell>
        </fo:table-row>
      </fo:table-body>
    </fo:table>
  </xsl:variable>

  <!-- Really output a header? -->
  <xsl:choose>
    <xsl:when test="$pageclass = 'titlepage' and $gentext-key = 'book'
                    and $sequence='first'">
      <!-- no, book titlepages have no headers at all -->
    </xsl:when>
    <xsl:when test="$sequence = 'blank' and $headers.on.blank.pages = 0">
      <!-- no output -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$candidate"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="header.content">
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="position" select="''"/>
  <xsl:param name="gentext-key" select="''"/>

<!--
  <fo:block>
    <xsl:value-of select="$pageclass"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="$sequence"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="$position"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="$gentext-key"/>
  </fo:block>
-->

  <fo:block>

    <!-- sequence can be odd, even, first, blank -->
    <!-- position can be left, center, right -->
    <xsl:choose>
      <xsl:when test="$sequence = 'blank'">
        <!-- nothing -->
      </xsl:when>

      <xsl:when test="$position='left'">
        <!-- Same for odd, even, empty, and blank sequences -->
        <xsl:call-template name="draft.text"/>
      </xsl:when>

      <xsl:when test="($sequence='odd' or $sequence='even') and $position='center'">
        <xsl:if test="$pageclass != 'titlepage'">
          <xsl:choose>
            <xsl:when test="ancestor::book and ($double.sided != 0)">
              <fo:retrieve-marker retrieve-class-name="section.head.marker"
                                  retrieve-position="first-including-carryover"
                                  retrieve-boundary="page-sequence"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="." mode="titleabbrev.markup"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </xsl:when>

      <xsl:when test="$position='center'">
        <!-- nothing for empty and blank sequences -->
      </xsl:when>

      <xsl:when test="$position='right'">
        <!-- Same for odd, even, empty, and blank sequences -->
        <xsl:call-template name="draft.text"/>
      </xsl:when>

      <xsl:when test="$sequence = 'first'">
        <!-- nothing for first pages -->
      </xsl:when>

      <xsl:when test="$sequence = 'blank'">
        <!-- nothing for blank pages -->
      </xsl:when>
    </xsl:choose>
  </fo:block>
</xsl:template>

<xsl:template name="header.footer.width">
  <xsl:param name="location" select="'header'"/>
  <xsl:param name="position" select="1"/>

  <xsl:variable name="width.set">
    <xsl:choose>
      <xsl:when test="$location = 'header'">
        <xsl:value-of select="normalize-space($header.column.widths)"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="normalize-space($footer.column.widths)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>


  <xsl:variable name="width">
    <xsl:choose>
      <xsl:when test="$position = 1">
        <xsl:value-of select="substring-before($width.set, ' ')"/>
      </xsl:when>
      <xsl:when test="$position = 2">
        <xsl:value-of select="substring-before(substring-after($width.set, ' '), ' ')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="substring-after(substring-after($width.set, ' '), ' ')"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <!-- Make sure it is a number -->
  <xsl:choose>
    <xsl:when test = "$width = number($width)">
      <xsl:value-of select="$width"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message>Error: value in <xsl:value-of select="$location"/>.column.widths at position <xsl:value-of select="$position"/> is not a number.</xsl:message>
      <xsl:text>1</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="draft.text">
  <xsl:choose>
    <xsl:when test="$draft.mode = 'yes'">
      <xsl:call-template name="gentext">
        <xsl:with-param name="key" select="'Draft'"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$draft.mode = 'no'">
      <!-- nop -->
    </xsl:when>
    <xsl:when test="ancestor-or-self::*[@status][1]/@status = 'draft'">
      <xsl:call-template name="gentext">
        <xsl:with-param name="key" select="'Draft'"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <!-- nop -->
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="*" mode="running.foot.mode">
  <xsl:param name="master-reference" select="'unknown'"/>
  <xsl:param name="gentext-key" select="name(.)"/>

  <!-- remove -draft from reference -->
  <xsl:variable name="pageclass">
    <xsl:choose>
      <xsl:when test="contains($master-reference, '-draft')">
        <xsl:value-of select="substring-before($master-reference, '-draft')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$master-reference"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <fo:static-content flow-name="xsl-region-after-first">
    <fo:block xsl:use-attribute-sets="footer.content.properties">
      <xsl:call-template name="footer.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'first'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <fo:static-content flow-name="xsl-region-after-odd">
    <fo:block xsl:use-attribute-sets="footer.content.properties">
      <xsl:call-template name="footer.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'odd'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <fo:static-content flow-name="xsl-region-after-even">
    <fo:block xsl:use-attribute-sets="footer.content.properties">
      <xsl:call-template name="footer.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'even'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>

  <fo:static-content flow-name="xsl-region-after-blank">
    <fo:block xsl:use-attribute-sets="footer.content.properties">
      <xsl:call-template name="footer.table">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="'blank'"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
    </fo:block>
  </fo:static-content>
</xsl:template>

<xsl:template name="footer.table">
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="gentext-key" select="''"/>

  <!-- default is a single table style for all footers -->
  <!-- Customize it for different page classes or sequence location -->

  <xsl:choose>
      <xsl:when test="$pageclass = 'index'">
          <xsl:attribute name="margin-left">0pt</xsl:attribute>
      </xsl:when>
  </xsl:choose>

  <xsl:variable name="column1">
    <xsl:choose>
      <xsl:when test="$sequence = 'first' or $sequence = 'odd'">1</xsl:when>
      <xsl:otherwise>3</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="column3">
    <xsl:choose>
      <xsl:when test="$sequence = 'first' or $sequence = 'odd'">3</xsl:when>
      <xsl:otherwise>1</xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="candidate">
    <fo:table table-layout="fixed" width="100%">
      <xsl:call-template name="foot.sep.rule">
        <xsl:with-param name="pageclass" select="$pageclass"/>
        <xsl:with-param name="sequence" select="$sequence"/>
        <xsl:with-param name="gentext-key" select="$gentext-key"/>
      </xsl:call-template>
      <fo:table-column column-number="1">
        <xsl:attribute name="column-width">
          <xsl:text>proportional-column-width(</xsl:text>
          <xsl:call-template name="header.footer.width">
            <xsl:with-param name="location">footer</xsl:with-param>
            <xsl:with-param name="position" select="$column1"/>
          </xsl:call-template>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </fo:table-column>
      <fo:table-column column-number="2">
        <xsl:attribute name="column-width">
          <xsl:text>proportional-column-width(</xsl:text>
          <xsl:call-template name="header.footer.width">
            <xsl:with-param name="location">footer</xsl:with-param>
            <xsl:with-param name="position" select="2"/>
          </xsl:call-template>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </fo:table-column>
      <fo:table-column column-number="3">
        <xsl:attribute name="column-width">
          <xsl:text>proportional-column-width(</xsl:text>
          <xsl:call-template name="header.footer.width">
            <xsl:with-param name="location">footer</xsl:with-param>
            <xsl:with-param name="position" select="$column3"/>
          </xsl:call-template>
          <xsl:text>)</xsl:text>
        </xsl:attribute>
      </fo:table-column>

      <fo:table-body>
        <fo:table-row height="14pt">
          <fo:table-cell text-align="left"
                         display-align="after">
            <xsl:if test="$fop.extensions = 0">
              <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
            <fo:block>
              <xsl:call-template name="footer.content">
                <xsl:with-param name="pageclass" select="$pageclass"/>
                <xsl:with-param name="sequence" select="$sequence"/>
                <xsl:with-param name="position" select="'left'"/>
                <xsl:with-param name="gentext-key" select="$gentext-key"/>
              </xsl:call-template>
            </fo:block>
          </fo:table-cell>
          <fo:table-cell text-align="center"
                         display-align="after">
            <xsl:if test="$fop.extensions = 0">
              <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
            <fo:block>
              <xsl:call-template name="footer.content">
                <xsl:with-param name="pageclass" select="$pageclass"/>
                <xsl:with-param name="sequence" select="$sequence"/>
                <xsl:with-param name="position" select="'center'"/>
                <xsl:with-param name="gentext-key" select="$gentext-key"/>
              </xsl:call-template>
            </fo:block>
          </fo:table-cell>
          <fo:table-cell text-align="right"
                         display-align="after">
            <xsl:if test="$fop.extensions = 0">
              <xsl:attribute name="relative-align">baseline</xsl:attribute>
            </xsl:if>
            <fo:block>
              <xsl:call-template name="footer.content">
                <xsl:with-param name="pageclass" select="$pageclass"/>
                <xsl:with-param name="sequence" select="$sequence"/>
                <xsl:with-param name="position" select="'right'"/>
                <xsl:with-param name="gentext-key" select="$gentext-key"/>
              </xsl:call-template>
            </fo:block>
          </fo:table-cell>
        </fo:table-row>
      </fo:table-body>
    </fo:table>
  </xsl:variable>

  <!-- Really output a footer? -->
  <xsl:choose>
    <xsl:when test="$pageclass='titlepage' and $gentext-key='book'
                    and $sequence='first'">
      <!-- no, book titlepages have no footers at all -->
    </xsl:when>
    <xsl:when test="$sequence = 'blank' and $footers.on.blank.pages = 0">
      <!-- no output -->
    </xsl:when>
    <xsl:otherwise>
      <xsl:copy-of select="$candidate"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="footer.content">
  <xsl:param name="pageclass" select="''"/>
  <xsl:param name="sequence" select="''"/>
  <xsl:param name="position" select="''"/>
  <xsl:param name="gentext-key" select="''"/>

<!--
  <fo:block>
    <xsl:value-of select="$pageclass"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="$sequence"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="$position"/>
    <xsl:text>, </xsl:text>
    <xsl:value-of select="$gentext-key"/>
  </fo:block>
-->

  <fo:block>
    <!-- pageclass can be front, body, back -->
    <!-- sequence can be odd, even, first, blank -->
    <!-- position can be left, center, right -->
    <xsl:choose>
      <xsl:when test="$pageclass = 'titlepage'">
        <!-- nop; no footer on title pages -->
      </xsl:when>

      <xsl:when test="$double.sided != 0 and $sequence = 'even'
                      and $position='left'">
        <fo:page-number/>
      </xsl:when>

      <xsl:when test="$double.sided != 0 and ($sequence = 'odd' or $sequence = 'first')
                      and $position='right'">
        <fo:page-number/>
      </xsl:when>

      <xsl:when test="$double.sided = 0 and $position='center'">
        <fo:page-number/>
      </xsl:when>

      <xsl:when test="$sequence='blank'">
        <xsl:choose>
          <xsl:when test="$double.sided != 0 and $position = 'left'">
            <fo:page-number/>
          </xsl:when>
          <xsl:when test="$double.sided = 0 and $position = 'center'">
            <fo:page-number/>
          </xsl:when>
          <xsl:otherwise>
            <!-- nop -->
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>


      <xsl:otherwise>
        <!-- nop -->
      </xsl:otherwise>
    </xsl:choose>
  </fo:block>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template name="page.number.format">
  <xsl:param name="element" select="local-name(.)"/>

  <xsl:choose>
    <xsl:when test="$element = 'toc'">i</xsl:when>
    <xsl:when test="$element = 'preface'">i</xsl:when>
    <xsl:when test="$element = 'dedication'">i</xsl:when>
    <xsl:otherwise>1</xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
