<?xml version="1.0"?>
<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ Copyright (c) 2008-2012, Red Hat Inc. or third-party contributors as
  ~ indicated by the @author tags or express copyright attribution
  ~ statements applied by the authors.  All third-party contributions are
  ~ distributed under license by Red Hat Inc.
  ~
  ~ This copyrighted material is made available to anyone wishing to use, modify,
  ~ copy, or redistribute it subject to the terms and conditions of the GNU
  ~ Lesser General Public License, as published by the Free Software Foundation.
  ~
  ~ This program is distributed in the hope that it will be useful,
  ~ but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
  ~ or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
  ~ for more details.
  ~
  ~ You should have received a copy of the GNU Lesser General Public License
  ~ along with this distribution; if not, write to:
  ~ Free Software Foundation, Inc.
  ~ 51 Franklin Street, Fifth Floor
  ~ Boston, MA  02110-1301  USA
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
