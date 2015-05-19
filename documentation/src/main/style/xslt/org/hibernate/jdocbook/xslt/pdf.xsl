<?xml version='1.0'?>

<!--
  ~ Hibernate, Relational Persistence for Idiomatic Java
  ~
  ~ License: GNU Lesser General Public License (LGPL), version 2.1 or later.
  ~ See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
  -->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                exclude-result-prefixes="#default">

   <xsl:import href="classpath:/xslt/org/jboss/pressgang/pdf.xsl"/>
   <xsl:import href="common-base.xsl"/>
   <xsl:import href="fop1.xsl" />

    <xsl:import href="classpath:/xslt/org/jboss/xslt/fonts/pdf/fonts.xsl" />

    <!-- work around problems in the jboss.org styles wrt pdf & jhighlight -->
    <xsl:param name="programlisting.font" select="'monospace,fixed'" />
    <xsl:param name="programlisting.font.size" select="'75%'" />

	<xsl:param name="title.font.family">
		<xsl:variable name="font">
			<xsl:call-template name="pickfont-sans"/>
		</xsl:variable>
        <xsl:message>
            <xsl:text>Setting 'title.font.family' param =</xsl:text><xsl:copy-of select="$font"/>
        </xsl:message>
		<xsl:copy-of select="$font"/>
	</xsl:param>

	<xsl:param name="body.font.family">
		<xsl:variable name="font">
			<xsl:call-template name="pickfont-sans"/>
		</xsl:variable>
        <xsl:message>
            <xsl:text>Setting 'body.font.family' param =</xsl:text><xsl:copy-of select="$font"/>
        </xsl:message>
		<xsl:copy-of select="$font"/>
	</xsl:param>

	<xsl:param name="monospace.font.family">
		<xsl:variable name="font">
		    <xsl:call-template name="pickfont-mono"/>
		</xsl:variable>
        <xsl:message>
            <xsl:text>Setting 'monospace.font.family' param =</xsl:text><xsl:copy-of select="$font"/>
        </xsl:message>
		<xsl:copy-of select="$font"/>
	</xsl:param>

	<xsl:param name="sans.font.family">
		<xsl:variable name="font">
			<xsl:call-template name="pickfont-sans"/>
		</xsl:variable>
        <xsl:message>
            <xsl:text>Setting 'sans.font.family' param =</xsl:text><xsl:copy-of select="$font"/>
        </xsl:message>
		<xsl:copy-of select="$font"/>
	</xsl:param>

   <!-- Change the font color for titles to Hibernate.org one -->
   <xsl:param name="title.color">#4a5d75</xsl:param>
   <xsl:param name="titlepage.color">#4a5d75</xsl:param>
   <xsl:param name="chapter.title.color">#4a5d75</xsl:param>
   <xsl:param name="section.title.color">#4a5d75</xsl:param>

   <!-- Style tables to look like SeamFramework.org
   <xsl:param name="table.cell.border.color">#D3D2D1</xsl:param>
   <xsl:param name="table.frame.border.color">#D3D2D1</xsl:param>
   <xsl:param name="table.cell.border.thickness">0.6pt</xsl:param>

   <xsl:param name="table.frame.border.thickness">0.6pt</xsl:param>
   <xsl:param name="table.cell.border.right.color">white</xsl:param>
   <xsl:param name="table.cell.border.left.color">#D3D2D1</xsl:param>
   <xsl:param name="table.frame.border.right.color">white</xsl:param>
   <xsl:param name="table.frame.border.left.color">white</xsl:param>
-->

   <xsl:template name="table.cell.block.properties">
      <!-- highlight this entry? -->
      <xsl:if test="ancestor::thead or ancestor::tfoot">
         <xsl:attribute name="font-weight">bold</xsl:attribute>
         <xsl:attribute name="background-color">#EDE8DB</xsl:attribute>
         <xsl:attribute name="color">black</xsl:attribute>
      </xsl:if>
   </xsl:template>

   <!--
      From: fo/table.xsl
      Reason: Table Header format
      Version:1.72
   -->
   <!-- customize this template to add row properties -->
   <xsl:template name="table.row.properties">
      <xsl:variable name="bgcolor">
         <xsl:call-template name="dbfo-attribute">
            <xsl:with-param name="pis" select="processing-instruction('dbfo')" />
            <xsl:with-param name="attribute" select="'bgcolor'" />
         </xsl:call-template>
      </xsl:variable>
      <xsl:if test="$bgcolor != ''">
         <xsl:attribute name="background-color">
      <xsl:value-of select="$bgcolor" />
    </xsl:attribute>
      </xsl:if>
      <xsl:if test="ancestor::thead or ancestor::tfoot">
         <xsl:attribute name="background-color">#EDE8DB</xsl:attribute>
      </xsl:if>
   </xsl:template>

</xsl:stylesheet>

