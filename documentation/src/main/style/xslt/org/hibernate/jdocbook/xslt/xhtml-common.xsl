<?xml version="1.0"?>
<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
  ~ indicated by the @author tags or express copyright attribution
  ~ statements applied by the authors.  All third-party contributions are
  ~ distributed under license by Red Hat Middleware LLC.
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
  ~
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

    <xsl:param name="siteHref" select="'http://www.hibernate.org'"/>
    <xsl:param name="docHref" select="'http://hibernate.org/Documentation/DocumentationOverview'"/>
    <xsl:param name="siteLinkText" select="'Hibernate.org'"/>
<!--
    <xsl:param name="use.id.as.filename">1</xsl:param>
-->
    <xsl:param name="legalnotice.filename">legalnotice.html</xsl:param>

    <xsl:template match="legalnotice" mode="chunk-filename">
        <xsl:value-of select="$legalnotice.filename"/>
    </xsl:template>

    <xsl:template name="user.footer.content">
        <HR/>
        <a>
            <xsl:attribute name="href">
                <xsl:apply-templates select="//legalnotice[1]" mode="chunk-filename"/>
            </xsl:attribute>
            <xsl:choose>
                <xsl:when test="//book/bookinfo/copyright[1]">
                    <xsl:apply-templates select="//book/bookinfo/copyright[1]" mode="titlepage.mode"/>
                </xsl:when>
                <xsl:when test="//legalnotice[1]">
                    <xsl:apply-templates select="//legalnotice[1]" mode="titlepage.mode"/>
                </xsl:when>
            </xsl:choose>
        </a>
    </xsl:template>

</xsl:stylesheet>
