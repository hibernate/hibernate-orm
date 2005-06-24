<?xml version="1.0"?>

<!--

    This is the XSL HTML configuration file for the Hibernate
    Reference Documentation.

    It took me days to figure out this stuff and fix most of
    the obvious bugs in the DocBook XSL distribution. Some of
    the workarounds might not be appropriate with a newer version
    of DocBook XSL. This file is released as part of Hibernate,
    hence LGPL licensed.

    christian@hibernate.org
-->

<!DOCTYPE xsl:stylesheet [
    <!ENTITY db_xsl_path        "../../support/docbook-xsl/">
]>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0"
                xmlns="http://www.w3.org/TR/xhtml1/transitional"
                exclude-result-prefixes="#default">
                
<xsl:import href="&db_xsl_path;/html/chunk.xsl"/>

<!--###################################################
                     HTML Settings
    ################################################### -->   

    <xsl:param name="chunk.section.depth">'5'</xsl:param>
    <xsl:param name="use.id.as.filename">'1'</xsl:param>
    <xsl:param name="html.stylesheet">../shared/css/html.css</xsl:param>

    <!-- These extensions are required for table printing and other stuff -->
    <xsl:param name="use.extensions">1</xsl:param>
    <xsl:param name="tablecolumns.extension">0</xsl:param>
    <xsl:param name="callout.extensions">1</xsl:param>
    <xsl:param name="graphicsize.extension">0</xsl:param>
    
<!--###################################################
                      Table Of Contents
    ################################################### -->   

    <!-- Generate the TOCs for named components only -->
    <xsl:param name="generate.toc">
        book   toc
    </xsl:param>
    
    <!-- Show only Sections up to level 3 in the TOCs -->
    <xsl:param name="toc.section.depth">3</xsl:param>

<!--###################################################
                         Labels
    ################################################### -->   

    <!-- Label Chapters and Sections (numbering) -->
    <xsl:param name="chapter.autolabel">1</xsl:param>
    <xsl:param name="section.autolabel" select="1"/>
    <xsl:param name="section.label.includes.component.label" select="1"/>
                
<!--###################################################
                         Callouts
    ################################################### -->   

    <!-- Don't use graphics, use a simple number style -->
    <xsl:param name="callout.graphics">0</xsl:param>

    <!-- Place callout marks at this column in annotated areas -->
    <xsl:param name="callout.defaultcolumn">90</xsl:param>

<!--###################################################
                          Misc
    ################################################### -->   

    <!-- Placement of titles -->
    <xsl:param name="formal.title.placement">
        figure after
        example before
        equation before
        table before
        procedure before
    </xsl:param>    
    
</xsl:stylesheet>
