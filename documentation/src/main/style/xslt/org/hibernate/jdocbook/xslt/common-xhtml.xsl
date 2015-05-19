<?xml version="1.0"?>
<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ License: GNU Lesser General Public License (LGPL), version 2.1 or later.
  ~ See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:d="http://docbook.org/ns/docbook">

    <xsl:import href="common-base.xsl"/>

    <xsl:param name="siteHref" select="'http://www.hibernate.org'"/>
    <xsl:param name="docHref" select="'http://hibernate.org/Documentation/DocumentationOverview'"/>
    <xsl:param name="siteLinkText" select="'Hibernate.org'"/>

    <xsl:param name="legalnotice.filename">legalnotice.html</xsl:param>

    <xsl:template match="d:legalnotice" mode="chunk-filename">
        <xsl:value-of select="$legalnotice.filename"/>
    </xsl:template>

    <xsl:template name="user.footer.content">
        <hr/>
        <a>
            <xsl:attribute name="href">
                <xsl:value-of select="$legalnotice.filename"/>
            </xsl:attribute>
            <xsl:choose>
                <xsl:when test="//d:book/d:bookinfo/d:copyright[1]">
                    <xsl:apply-templates select="//d:book/d:bookinfo/d:copyright[1]" mode="titlepage.mode"/>
                </xsl:when>
                <xsl:when test="//d:legalnotice[1]">
                    <xsl:apply-templates select="//d:legalnotice[1]" mode="titlepage.mode"/>
                </xsl:when>
            </xsl:choose>
        </a>
    </xsl:template>

</xsl:stylesheet>
