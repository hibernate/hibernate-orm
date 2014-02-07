<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ Copyright (c) 2014, Red Hat Inc. or third-party contributors as
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
<xsl:transform version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/hibernate-mapping">
        <entity-mappings xmlns="http://www.hibernate.org/xsd/orm" version="2.1.0">

            <description>
                Hibernate orm.xml document auto-generated from legacy hbm.xml format via Hibernate-supplied
                XSLT transformation (generated at <xsl:value-of select="current-dateTime()" />).
            </description>

            <persistence-unit-metadata>
                <description>
                    Defines information which applies to the persistence unit overall, not just to this mapping file.

                    The XSLT transformation does not specify any persistence-unit-metadata itself.
                </description>

                <!-- By definition, the legacy hbm.xml files were utterly metadata complete -->
                <xml-mapping-metadata-complete/>

                <persistence-unit-defaults>
                    <description>
                        Defines defaults across the persistence unit overall, not just to this mapping file.

                        Again, the XSLT transformation does not specify any.
                    </description>
                </persistence-unit-defaults>
            </persistence-unit-metadata>

            <xsl:if test="@package != ''">
                <xsl:element name="default-package">
                    <xsl:value-of select="@package"/>
                </xsl:element>

                <xsl:element name="package">
                    <xsl:value-of select="@package"/>
                </xsl:element>
            </xsl:if>

            <xsl:if test="@schema != ''">
                <xsl:element name="schema">
                    <xsl:value-of select="@schema"/>
                </xsl:element>
            </xsl:if>

            <xsl:if test="@catalog != ''">
                <xsl:element name="catalog">
                    <xsl:value-of select="@catalog"/>
                </xsl:element>
            </xsl:if>
            
            <xsl:if test="@default-access != ''">
                <xsl:choose>
                    <xsl:when test="@default-acces = lower-case('property')">
                        <xsl:element name="access">
                            <xsl:text>PROPERTY</xsl:text>
                        </xsl:element>
                    </xsl:when>
                    <xsl:when test="@default-acces = lower-case('field')">
                        <xsl:element name="access">
                            <xsl:text>FIELD</xsl:text>
                        </xsl:element>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:element name="custom-access">
                            <xsl:value-of select="@default-access"/>
                        </xsl:element>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:if>

            <xsl:if test="@auto-import != ''">
                <xsl:element name="auto-import">
                    <xsl:value-of select="@auto-import"/>
                </xsl:element>
            </xsl:if>

            <xsl:if test="@default-cascade != ''">
                <xsl:element name="default-cascade">
                    <xsl:value-of select="@default-cascade"/>
                </xsl:element>
            </xsl:if>

            <xsl:if test="@default-cascade != ''">
                <xsl:element name="default-cascade">
                    <xsl:value-of select="@default-cascade"/>
                </xsl:element>
            </xsl:if>
        </entity-mappings>
    </xsl:template>

</xsl:transform>