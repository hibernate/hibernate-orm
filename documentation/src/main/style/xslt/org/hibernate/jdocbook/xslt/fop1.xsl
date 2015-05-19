<?xml version='1.0'?>

<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ License: GNU Lesser General Public License (LGPL), version 2.1 or later.
  ~ See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version='1.0'>

    <!-- this one taken verbatim from the Seam xslt -->

    <xsl:template match="*" mode="fop1.outline">
        <xsl:apply-templates select="*" mode="fop1.outline"/>
    </xsl:template>

    <xsl:template match="set|book|part|reference|
                     preface|chapter|appendix|article
                     |glossary|bibliography|index|setindex
                     |refentry
                     |sect1|sect2|sect3|sect4|sect5|section"
                  mode="fop1.outline">

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
                <fo:bookmark internal-destination="{$id}" starting-state="hide">
                    <fo:bookmark-title>
                        <xsl:value-of select="normalize-space(translate($bookmark-label, $a-dia, $a-asc))"/>
                    </fo:bookmark-title>
                    <xsl:apply-templates select="*" mode="fop1.outline"/>
                </fo:bookmark>
            </xsl:when>
            <xsl:otherwise>
                <fo:bookmark internal-destination="{$id}" starting-state="hide">
                    <fo:bookmark-title>
                        <xsl:value-of select="normalize-space(translate($bookmark-label, $a-dia, $a-asc))"/>
                    </fo:bookmark-title>
                </fo:bookmark>

                <xsl:variable name="toc.params">
                    <xsl:call-template name="find.path.params">
                        <xsl:with-param name="table" select="normalize-space($generate.toc)"/>
                    </xsl:call-template>
                </xsl:variable>

                <xsl:if test="contains($toc.params, 'toc')
                    and (book|part|reference|preface|chapter|appendix|article
                         |glossary|bibliography|index|setindex
                         |refentry
                         |sect1|sect2|sect3|sect4|sect5|section)">
                    <fo:bookmark internal-destination="toc...{$id}" starting-state="hide">
                        <fo:bookmark-title>
                            <xsl:call-template name="gentext">
                                <xsl:with-param name="key" select="'TableofContents'"/>
                            </xsl:call-template>
                        </fo:bookmark-title>
                    </fo:bookmark>
                </xsl:if>
                <xsl:apply-templates select="*" mode="fop1.outline"/>
            </xsl:otherwise>
        </xsl:choose>
        <!--
  <fo:bookmark internal-destination="{$id}"/>
-->
    </xsl:template>


</xsl:stylesheet>

