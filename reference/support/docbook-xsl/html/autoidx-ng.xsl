<?xml version="1.0"?>
<!DOCTYPE xsl:stylesheet [

<!ENTITY lowercase "'abcdefghijklmnopqrstuvwxyz'">
<!ENTITY uppercase "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'">

<!ENTITY primary   'normalize-space(concat(primary/@sortas, primary[not(@sortas)]))'>
<!ENTITY secondary 'normalize-space(concat(secondary/@sortas, secondary[not(@sortas)]))'>
<!ENTITY tertiary  'normalize-space(concat(tertiary/@sortas, tertiary[not(@sortas)]))'>

<!ENTITY sep '" "'>
<!ENTITY scope 'count(ancestor::node()|$scope) = count(ancestor::node())'>
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0"
                xmlns:func="http://exslt.org/functions"
                exclude-result-prefixes="i"
                xmlns:i="urn:cz-kosek:functions:index">

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the DocBook XSL Stylesheet distribution.
     See ../README or http://docbook.sf.net/ for copyright
     and other information.

     ******************************************************************** -->

<xsl:include href="../common/autoidx-ng.xsl"/>

<!-- Modified original code is using index group codes instead of just first letter 
     to gain better grouping -->
<xsl:template name="generate-index">
  <xsl:param name="scope" select="(ancestor::book|/)[last()]"/>

  <xsl:variable name="terms"
                select="//indexterm[count(.|key('group-code',
                                                i:group-index(&primary;))[&scope;][1]) = 1
                                    and not(@class = 'endofrange')]"/>

  <div class="index">
    <xsl:apply-templates select="$terms" mode="index-div">
      <xsl:with-param name="scope" select="$scope"/>
      <xsl:sort select="i:group-index(&primary;)" data-type="number"/>
    </xsl:apply-templates>
  </div>
</xsl:template>

<xsl:template match="indexterm" mode="index-div">
  <xsl:param name="scope" select="."/>

  <xsl:variable name="key"
                select="i:group-index(&primary;)"/>

  <xsl:if test="key('group-code', $key)[&scope;]
                [count(.|key('primary', &primary;)[&scope;][1]) = 1]">
    <div class="indexdiv">
      <h3>
        <xsl:value-of select="i:group-letter($key)"/>
      </h3>
      <dl>
        <xsl:apply-templates select="key('group-code', $key)[&scope;]
                                     [count(.|key('primary', &primary;)[&scope;][1])=1]"
                             mode="index-primary">
          <xsl:sort select="translate(&primary;, &lowercase;, &uppercase;)"/>
          <xsl:with-param name="scope" select="$scope"/>
        </xsl:apply-templates>
      </dl>
    </div>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
