<?xml version='1.0'?>
<!DOCTYPE xsl:stylesheet [
<!ENTITY nbsp "&#160;">
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->

<!-- synopsis is in verbatim -->

<!-- ==================================================================== -->

<xsl:template match="cmdsynopsis">
  <div class="{name(.)}">
    <p>
      <xsl:call-template name="anchor"/>
      <xsl:apply-templates/>
    </p>
  </div>
</xsl:template>

<xsl:template match="cmdsynopsis/command">
  <br/>
  <xsl:call-template name="inline.monoseq"/>
  <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="cmdsynopsis/command[1]" priority="2">
  <xsl:call-template name="inline.monoseq"/>
  <xsl:text> </xsl:text>
</xsl:template>

<xsl:template match="group|arg" name="group-or-arg">
  <xsl:variable name="choice" select="@choice"/>
  <xsl:variable name="rep" select="@rep"/>
  <xsl:variable name="sepchar">
    <xsl:choose>
      <xsl:when test="ancestor-or-self::*/@sepchar">
        <xsl:value-of select="ancestor-or-self::*/@sepchar"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text> </xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>
  <xsl:if test="position()>1"><xsl:value-of select="$sepchar"/></xsl:if>
  <xsl:choose>
    <xsl:when test="$choice='plain'">
      <xsl:value-of select="$arg.choice.plain.open.str"/>
    </xsl:when>
    <xsl:when test="$choice='req'">
      <xsl:value-of select="$arg.choice.req.open.str"/>
    </xsl:when>
    <xsl:when test="$choice='opt'">
      <xsl:value-of select="$arg.choice.opt.open.str"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$arg.choice.def.open.str"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:apply-templates/>
  <xsl:choose>
    <xsl:when test="$rep='repeat'">
      <xsl:value-of select="$arg.rep.repeat.str"/>
    </xsl:when>
    <xsl:when test="$rep='norepeat'">
      <xsl:value-of select="$arg.rep.norepeat.str"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$arg.rep.def.str"/>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:choose>
    <xsl:when test="$choice='plain'">
      <xsl:value-of select="$arg.choice.plain.close.str"/>
    </xsl:when>
    <xsl:when test="$choice='req'">
      <xsl:value-of select="$arg.choice.req.close.str"/>
    </xsl:when>
    <xsl:when test="$choice='opt'">
      <xsl:value-of select="$arg.choice.opt.close.str"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$arg.choice.def.close.str"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="group/arg">
  <xsl:variable name="choice" select="@choice"/>
  <xsl:variable name="rep" select="@rep"/>
  <xsl:if test="position()>1"><xsl:value-of select="$arg.or.sep"/></xsl:if>
  <xsl:call-template name="group-or-arg"/>
</xsl:template>

<xsl:template match="sbr">
  <br/>
</xsl:template>

<!-- ==================================================================== -->

<xsl:template match="synopfragmentref">
  <xsl:variable name="target" select="key('id',@linkend)"/>
  <xsl:variable name="snum">
    <xsl:apply-templates select="$target" mode="synopfragment.number"/>
  </xsl:variable>
  <i>
    <a href="#{@linkend}">
      <xsl:text>(</xsl:text>
      <xsl:value-of select="$snum"/>
      <xsl:text>)</xsl:text>
    </a>
    <xsl:text>&#160;</xsl:text>
    <xsl:apply-templates/>
  </i>
</xsl:template>

<xsl:template match="synopfragment" mode="synopfragment.number">
  <xsl:number format="1"/>
</xsl:template>

<xsl:template match="synopfragment">
  <xsl:variable name="snum">
    <xsl:apply-templates select="." mode="synopfragment.number"/>
  </xsl:variable>
  <p>
    <a name="{@id}">
      <xsl:text>(</xsl:text>
      <xsl:value-of select="$snum"/>
      <xsl:text>)</xsl:text>
    </a>
    <xsl:text> </xsl:text>
    <xsl:apply-templates/>
  </p>
</xsl:template>

<xsl:template match="funcsynopsis">
  <xsl:call-template name="informal.object"/>
</xsl:template>

<xsl:template match="funcsynopsisinfo">
  <pre class="{name(.)}"><xsl:apply-templates/></pre>
</xsl:template>

<!-- ====================================================================== -->
<!-- funcprototype -->
<!--

funcprototype ::= (funcdef,
                   (void|varargs|paramdef+))

funcdef       ::= (#PCDATA|type|replaceable|function)*

paramdef      ::= (#PCDATA|type|replaceable|parameter|funcparams)*
-->

<xsl:template match="funcprototype">
  <xsl:variable name="html-style">
    <xsl:call-template name="dbhtml-attribute">
      <xsl:with-param name="pis"
                      select="ancestor::funcsynopsis//processing-instruction('dbhtml')"/>
      <xsl:with-param name="attribute" select="'funcsynopsis-style'"/>
    </xsl:call-template>
  </xsl:variable>

  <xsl:variable name="style">
    <xsl:choose>
      <xsl:when test="$html-style != ''">
        <xsl:value-of select="$html-style"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$funcsynopsis.style"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:variable>

  <xsl:variable name="tabular-p"
                select="$funcsynopsis.tabular.threshold &gt; 0
                        and string-length(.) &gt; $funcsynopsis.tabular.threshold"/>

  <xsl:choose>
    <xsl:when test="$style = 'kr' and $tabular-p">
      <xsl:apply-templates select="." mode="kr-tabular"/>
    </xsl:when>
    <xsl:when test="$style = 'kr'">
      <xsl:apply-templates select="." mode="kr-nontabular"/>
    </xsl:when>
    <xsl:when test="$style = 'ansi' and $tabular-p">
      <xsl:apply-templates select="." mode="ansi-tabular"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="." mode="ansi-nontabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<!-- ====================================================================== -->
<!-- funcprototype: kr, non-tabular -->

<xsl:template match="funcprototype" mode="kr-nontabular">
  <p>
    <xsl:apply-templates mode="kr-nontabular"/>
    <xsl:if test="paramdef">
      <br/>
      <xsl:apply-templates select="paramdef" mode="kr-funcsynopsis-mode"/>
    </xsl:if>
  </p>
</xsl:template>

<xsl:template match="funcdef" mode="kr-nontabular">
  <code class="{name(.)}">
    <xsl:apply-templates mode="kr-nontabular"/>
    <xsl:text>(</xsl:text>
  </code>
</xsl:template>

<xsl:template match="funcdef/function" mode="kr-nontabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <b class="fsfunc"><xsl:apply-templates mode="kr-nontabular"/></b>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="kr-nontabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="void" mode="kr-nontabular">
  <code>)</code>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="varargs" mode="kr-nontabular">
  <xsl:text>...</xsl:text>
  <code>)</code>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="paramdef" mode="kr-nontabular">
  <xsl:apply-templates select="parameter" mode="kr-nontabular"/>
  <xsl:choose>
    <xsl:when test="following-sibling::*">
      <xsl:text>, </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <code>)</code>
      <xsl:text>;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="paramdef/parameter" mode="kr-nontabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <var class="pdparam">
        <xsl:apply-templates mode="kr-nontabular"/>
      </var>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="kr-nontabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="paramdef" mode="kr-funcsynopsis-mode">
  <xsl:if test="preceding-sibling::paramdef"><br/></xsl:if>
  <xsl:apply-templates mode="kr-funcsynopsis-mode"/>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="type|parameter|funcparams" mode="kr-funcsynopsis-mode">
  <code>
    <xsl:apply-templates mode="kr-funcsynopsis-mode"/>
  </code>
</xsl:template>

<!-- ====================================================================== -->
<!-- funcprototype: kr, tabular -->

<xsl:template match="funcprototype" mode="kr-tabular">
  <table border="0" summary="Function synopsis" cellspacing="0" cellpadding="0"
         style="padding-bottom: 1em">
    <tr>
      <td>
        <xsl:apply-templates select="funcdef" mode="kr-tabular"/>
      </td>
      <xsl:apply-templates select="(void|varargs|paramdef)[1]" mode="kr-tabular"/>
    </tr>
    <xsl:for-each select="(void|varargs|paramdef)[position() &gt; 1]">
      <tr>
        <td>&#160;</td>
        <xsl:apply-templates select="." mode="kr-tabular"/>
      </tr>
    </xsl:for-each>
  </table>
  <xsl:if test="paramdef">
    <table border="0" summary="Function argument synopsis"
           cellspacing="0" cellpadding="0">
      <xsl:if test="following-sibling::funcprototype">
        <xsl:attribute name="style">padding-bottom: 1em</xsl:attribute>
      </xsl:if>
      <xsl:apply-templates select="paramdef" mode="kr-tabular-funcsynopsis-mode"/>
    </table>
  </xsl:if>
</xsl:template>

<xsl:template match="funcdef" mode="kr-tabular">
  <code class="{name(.)}">
    <xsl:apply-templates mode="kr-tabular"/>
    <xsl:text>(</xsl:text>
  </code>
</xsl:template>

<xsl:template match="funcdef/function" mode="kr-tabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <b class="fsfunc"><xsl:apply-templates mode="kr-nontabular"/></b>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="kr-tabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="void" mode="kr-tabular">
  <td>
    <code>)</code>
    <xsl:text>;</xsl:text>
  </td>
  <td>&#160;</td>
</xsl:template>

<xsl:template match="varargs" mode="kr-tabular">
  <td>
    <xsl:text>...</xsl:text>
    <code>)</code>
    <xsl:text>;</xsl:text>
  </td>
  <td>&#160;</td>
</xsl:template>

<xsl:template match="paramdef" mode="kr-tabular">
  <td>
    <xsl:apply-templates select="parameter" mode="kr-tabular"/>
    <xsl:choose>
      <xsl:when test="following-sibling::*">
        <xsl:text>, </xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <code>)</code>
        <xsl:text>;</xsl:text>
      </xsl:otherwise>
    </xsl:choose>
  </td>
  <td>&#160;</td>
</xsl:template>

<xsl:template match="paramdef/parameter" mode="kr-tabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <var class="pdparam">
        <xsl:apply-templates mode="kr-tabular"/>
      </var>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="kr-tabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="paramdef" mode="kr-tabular-funcsynopsis-mode">
  <tr>
    <xsl:choose>
      <xsl:when test="type and funcparams">
        <td>
          <xsl:apply-templates select="type" mode="kr-tabular-funcsynopsis-mode"/>
          <xsl:text>&#160;</xsl:text>
        </td>
        <td>
          <xsl:apply-templates select="type/following-sibling::node()"
                               mode="kr-tabular-funcsynopsis-mode"/>
        </td>
      </xsl:when>
      <xsl:when test="funcparams">
        <td colspan="2">
          <xsl:apply-templates mode="kr-tabular-funcsynopsis-mode"/>
        </td>
      </xsl:when>
      <xsl:otherwise>
        <td>
          <xsl:apply-templates select="parameter/preceding-sibling::node()"
                               mode="kr-tabular-funcsynopsis-mode"/>
          <xsl:text>&#160;</xsl:text>
        </td>
        <td>
          <xsl:apply-templates select="parameter"
                               mode="kr-tabular"/>
          <xsl:apply-templates select="parameter/following-sibling::node()"
                               mode="kr-tabular-funcsynopsis-mode"/>
          <xsl:text>;</xsl:text>
        </td>
      </xsl:otherwise>
    </xsl:choose>
  </tr>
</xsl:template>

<xsl:template match="paramdef/parameter" mode="kr-tabular-funcsynopsis-mode">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <var class="pdparam">
        <xsl:apply-templates mode="kr-tabular-funcsynopsis-mode"/>
      </var>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="kr-tabular-funcsynopsis-mode"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="funcparams" mode="kr-tabular-funcsynopsis-mode">
  <code>(</code>
  <xsl:apply-templates mode="kr-tabular-funcsynopsis-mode"/>
  <code>)</code>
  <xsl:text>;</xsl:text>
</xsl:template>

<!-- ====================================================================== -->
<!-- funcprototype: ansi, non-tabular -->

<xsl:template match="funcprototype" mode="ansi-nontabular">
  <p>
    <xsl:apply-templates mode="ansi-nontabular"/>
  </p>
</xsl:template>

<xsl:template match="funcdef" mode="ansi-nontabular">
  <code class="{name(.)}">
    <xsl:apply-templates mode="ansi-nontabular"/>
    <xsl:text>(</xsl:text>
  </code>
</xsl:template>

<xsl:template match="funcdef/function" mode="ansi-nontabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <b class="fsfunc"><xsl:apply-templates mode="ansi-nontabular"/></b>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="ansi-nontabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="void" mode="ansi-nontabular">
  <code>void)</code>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="varargs" mode="ansi-nontabular">
  <xsl:text>...</xsl:text>
  <code>)</code>
  <xsl:text>;</xsl:text>
</xsl:template>

<xsl:template match="paramdef" mode="ansi-nontabular">
  <xsl:apply-templates mode="ansi-nontabular"/>
  <xsl:choose>
    <xsl:when test="following-sibling::*">
      <xsl:text>, </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <code>)</code>
      <xsl:text>;</xsl:text>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="paramdef/parameter" mode="ansi-nontabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <var class="pdparam">
        <xsl:apply-templates mode="ansi-nontabular"/>
      </var>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="ansi-nontabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="funcparams" mode="ansi-nontabular">
  <code>(</code>
  <xsl:apply-templates mode="ansi-nontabular"/>
  <code>)</code>
</xsl:template>

<!-- ====================================================================== -->
<!-- funcprototype: ansi, tabular -->

<xsl:template match="funcprototype" mode="ansi-tabular">
  <table border="0" summary="Function synopsis" cellspacing="0" cellpadding="0">
    <xsl:if test="following-sibling::funcprototype">
      <xsl:attribute name="style">padding-bottom: 1em</xsl:attribute>
    </xsl:if>
    <tr>
      <td>
        <xsl:apply-templates select="funcdef" mode="ansi-tabular"/>
      </td>
      <xsl:apply-templates select="(void|varargs|paramdef)[1]" mode="ansi-tabular"/>
    </tr>
    <xsl:for-each select="(void|varargs|paramdef)[position() &gt; 1]">
      <tr>
        <td>&#160;</td>
        <xsl:apply-templates select="." mode="ansi-tabular"/>
      </tr>
    </xsl:for-each>
  </table>
</xsl:template>

<xsl:template match="funcdef" mode="ansi-tabular">
  <code class="{name(.)}">
    <xsl:apply-templates mode="ansi-tabular"/>
    <xsl:text>(</xsl:text>
  </code>
</xsl:template>

<xsl:template match="funcdef/function" mode="ansi-tabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <b class="fsfunc"><xsl:apply-templates mode="ansi-nontabular"/></b>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="kr-tabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="void" mode="ansi-tabular">
  <td>
    <code>void)</code>
    <xsl:text>;</xsl:text>
  </td>
  <td>&#160;</td>
</xsl:template>

<xsl:template match="varargs" mode="ansi-tabular">
  <td>
    <xsl:text>...</xsl:text>
    <code>)</code>
    <xsl:text>;</xsl:text>
  </td>
  <td>&#160;</td>
</xsl:template>

<xsl:template match="paramdef" mode="ansi-tabular">
  <xsl:choose>
    <xsl:when test="type and funcparams">
      <td>
        <xsl:apply-templates select="type" mode="kr-tabular-funcsynopsis-mode"/>
        <xsl:text>&#160;</xsl:text>
      </td>
      <td>
        <xsl:apply-templates select="type/following-sibling::node()"
                             mode="kr-tabular-funcsynopsis-mode"/>
      </td>
    </xsl:when>
    <xsl:otherwise>
      <td>
        <xsl:apply-templates select="parameter/preceding-sibling::node()"
                             mode="ansi-tabular"/>
        <xsl:text>&#160;</xsl:text>
      </td>
      <td>
        <xsl:apply-templates select="parameter"
                             mode="ansi-tabular"/>
        <xsl:apply-templates select="parameter/following-sibling::node()"
                             mode="ansi-tabular"/>
        <xsl:choose>
          <xsl:when test="following-sibling::*">
            <xsl:text>, </xsl:text>
          </xsl:when>
          <xsl:otherwise>
            <code>)</code>
            <xsl:text>;</xsl:text>
          </xsl:otherwise>
        </xsl:choose>
      </td>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="paramdef/parameter" mode="ansi-tabular">
  <xsl:choose>
    <xsl:when test="$funcsynopsis.decoration != 0">
      <var class="pdparam">
        <xsl:apply-templates mode="ansi-tabular"/>
      </var>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates mode="ansi-tabular"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="funcparams" mode="ansi-tabular">
  <code>(</code>
  <xsl:apply-templates/>
  <code>)</code>
</xsl:template>

<!-- ====================================================================== -->

<xsl:variable name="default-classsynopsis-language">java</xsl:variable>

<xsl:template match="classsynopsis
                     |fieldsynopsis
                     |methodsynopsis
                     |constructorsynopsis
                     |destructorsynopsis">
  <xsl:param name="language">
    <xsl:choose>
      <xsl:when test="@language">
	<xsl:value-of select="@language"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:value-of select="$default-classsynopsis-language"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>

  <xsl:choose>
    <xsl:when test="$language='java'">
      <xsl:apply-templates select="." mode="java"/>
    </xsl:when>
    <xsl:when test="$language='perl'">
      <xsl:apply-templates select="." mode="perl"/>
    </xsl:when>
    <xsl:when test="$language='idl'">
      <xsl:apply-templates select="." mode="idl"/>
    </xsl:when>
    <xsl:when test="$language='cpp'">
      <xsl:apply-templates select="." mode="cpp"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message>
	<xsl:text>Unrecognized language on </xsl:text>
        <xsl:value-of select="name(.)"/>
        <xsl:text>: </xsl:text>
	<xsl:value-of select="$language"/>
      </xsl:message>
      <xsl:apply-templates select=".">
	<xsl:with-param name="language"
	  select="$default-classsynopsis-language"/>
      </xsl:apply-templates>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="synop-break">
  <xsl:if test="parent::classsynopsis
                or (following-sibling::fieldsynopsis
                    |following-sibling::methodsynopsis
                    |following-sibling::constructorsynopsis
                    |following-sibling::destructorsynopsis)">
    <br/>
  </xsl:if>
</xsl:template>


<!-- ===== Java ======================================================== -->

<xsl:template match="classsynopsis" mode="java">
  <pre class="{name(.)}">
    <xsl:apply-templates select="ooclass[1]" mode="java"/>
    <xsl:if test="ooclass[position() &gt; 1]">
      <xsl:text> extends</xsl:text>
      <xsl:apply-templates select="ooclass[position() &gt; 1]" mode="java"/>
      <xsl:if test="oointerface|ooexception">
        <br/>
	<xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;</xsl:text>
      </xsl:if>
    </xsl:if>
    <xsl:if test="oointerface">
      <xsl:text>implements</xsl:text>
      <xsl:apply-templates select="oointerface" mode="java"/>
      <xsl:if test="ooexception">
        <br/>
	<xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;</xsl:text>
      </xsl:if>
    </xsl:if>
    <xsl:if test="ooexception">
      <xsl:text>throws</xsl:text>
      <xsl:apply-templates select="ooexception" mode="java"/>
    </xsl:if>
    <xsl:text>&nbsp;{</xsl:text>
    <br/>
    <xsl:apply-templates select="constructorsynopsis
                                 |destructorsynopsis
                                 |fieldsynopsis
                                 |methodsynopsis
                                 |classsynopsisinfo" mode="java"/>
    <xsl:text>}</xsl:text>
  </pre>
</xsl:template>

<xsl:template match="classsynopsisinfo" mode="java">
  <xsl:apply-templates mode="java"/>
</xsl:template>

<xsl:template match="ooclass|oointerface|ooexception" mode="java">
  <xsl:choose>
    <xsl:when test="position() &gt; 1">
      <xsl:text>, </xsl:text>
    </xsl:when>
    <xsl:otherwise>
      <xsl:text> </xsl:text>
    </xsl:otherwise>
  </xsl:choose>
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="modifier" mode="java">
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="classname" mode="java">
  <xsl:if test="name(preceding-sibling::*[1]) = 'classname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="interfacename" mode="java">
  <xsl:if test="name(preceding-sibling::*[1]) = 'interfacename'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="exceptionname" mode="java">
  <xsl:if test="name(preceding-sibling::*[1]) = 'exceptionname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="fieldsynopsis" mode="java">
  <code class="{name(.)}">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates mode="java"/>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<xsl:template match="type" mode="java">
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="varname" mode="java">
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="initializer" mode="java">
  <span class="{name(.)}">
    <xsl:text>=&nbsp;</xsl:text>
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="void" mode="java">
  <span class="{name(.)}">
    <xsl:text>void&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="methodname" mode="java">
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="methodparam" mode="java">
  <xsl:param name="indent">0</xsl:param>
  <xsl:if test="position() &gt; 1">
    <xsl:text>,</xsl:text>
    <br/>
    <xsl:if test="$indent &gt; 0">
      <xsl:call-template name="copy-string">
	<xsl:with-param name="string">&nbsp;</xsl:with-param>
	<xsl:with-param name="count" select="$indent + 1"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template match="parameter" mode="java">
  <span class="{name(.)}">
    <xsl:apply-templates mode="java"/>
  </span>
</xsl:template>

<xsl:template mode="java"
  match="constructorsynopsis|destructorsynopsis|methodsynopsis">
  <xsl:variable name="modifiers" select="modifier"/>
  <xsl:variable name="notmod" select="*[name(.) != 'modifier']"/>
  <xsl:variable name="decl">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="$modifiers" mode="java"/>

    <!-- type -->
    <xsl:if test="name($notmod[1]) != 'methodname'">
      <xsl:apply-templates select="$notmod[1]" mode="java"/>
    </xsl:if>

    <xsl:apply-templates select="methodname" mode="java"/>
  </xsl:variable>

  <code class="{name(.)}">
    <xsl:copy-of select="$decl"/>
    <xsl:text>(</xsl:text>
    <xsl:apply-templates select="methodparam" mode="java">
      <xsl:with-param name="indent" select="string-length($decl)"/>
    </xsl:apply-templates>
    <xsl:text>)</xsl:text>
    <xsl:if test="exceptionname">
      <br/>
      <xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;throws&nbsp;</xsl:text>
      <xsl:apply-templates select="exceptionname" mode="java"/>
    </xsl:if>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<!-- ===== C++ ========================================================= -->

<xsl:template match="classsynopsis" mode="cpp">
  <pre class="{name(.)}">
    <xsl:apply-templates select="ooclass[1]" mode="cpp"/>
    <xsl:if test="ooclass[position() &gt; 1]">
      <xsl:text>: </xsl:text>
      <xsl:apply-templates select="ooclass[position() &gt; 1]" mode="cpp"/>
      <xsl:if test="oointerface|ooexception">
        <br/>
	<xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;</xsl:text>
      </xsl:if>
    </xsl:if>
    <xsl:if test="oointerface">
      <xsl:text> implements</xsl:text>
      <xsl:apply-templates select="oointerface" mode="cpp"/>
      <xsl:if test="ooexception">
        <br/>
	<xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;</xsl:text>
      </xsl:if>
    </xsl:if>
    <xsl:if test="ooexception">
      <xsl:text> throws</xsl:text>
      <xsl:apply-templates select="ooexception" mode="cpp"/>
    </xsl:if>
    <xsl:text>&nbsp;{</xsl:text>
    <br/>
    <xsl:apply-templates select="constructorsynopsis
                                 |destructorsynopsis
                                 |fieldsynopsis
                                 |methodsynopsis
                                 |classsynopsisinfo" mode="cpp"/>
    <xsl:text>}</xsl:text>
  </pre>
</xsl:template>

<xsl:template match="classsynopsisinfo" mode="cpp">
  <xsl:apply-templates mode="cpp"/>
</xsl:template>

<xsl:template match="ooclass|oointerface|ooexception" mode="cpp">
  <xsl:if test="position() &gt; 1">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="modifier" mode="cpp">
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="classname" mode="cpp">
  <xsl:if test="name(preceding-sibling::*[1]) = 'classname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="interfacename" mode="cpp">
  <xsl:if test="name(preceding-sibling::*[1]) = 'interfacename'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="exceptionname" mode="cpp">
  <xsl:if test="name(preceding-sibling::*[1]) = 'exceptionname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="fieldsynopsis" mode="cpp">
  <code class="{name(.)}">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates mode="cpp"/>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<xsl:template match="type" mode="cpp">
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="varname" mode="cpp">
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="initializer" mode="cpp">
  <span class="{name(.)}">
    <xsl:text>=&nbsp;</xsl:text>
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="void" mode="cpp">
  <span class="{name(.)}">
    <xsl:text>void&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="methodname" mode="cpp">
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="methodparam" mode="cpp">
  <xsl:if test="position() &gt; 1">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template match="parameter" mode="cpp">
  <span class="{name(.)}">
    <xsl:apply-templates mode="cpp"/>
  </span>
</xsl:template>

<xsl:template mode="cpp"
  match="constructorsynopsis|destructorsynopsis|methodsynopsis">
  <xsl:variable name="modifiers" select="modifier"/>
  <xsl:variable name="notmod" select="*[name(.) != 'modifier']"/>

  <code class="{name(.)}">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="$modifiers" mode="cpp"/>

    <!-- type -->
    <xsl:if test="name($notmod[1]) != 'methodname'">
      <xsl:apply-templates select="$notmod[1]" mode="cpp"/>
    </xsl:if>

    <xsl:apply-templates select="methodname" mode="cpp"/>
    <xsl:text>(</xsl:text>
    <xsl:apply-templates select="methodparam" mode="cpp"/>
    <xsl:text>)</xsl:text>
    <xsl:if test="exceptionname">
      <br/>
      <xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;throws&nbsp;</xsl:text>
      <xsl:apply-templates select="exceptionname" mode="cpp"/>
    </xsl:if>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<!-- ===== IDL ========================================================= -->

<xsl:template match="classsynopsis" mode="idl">
  <pre class="{name(.)}">
    <xsl:text>interface </xsl:text>
    <xsl:apply-templates select="ooclass[1]" mode="idl"/>
    <xsl:if test="ooclass[position() &gt; 1]">
      <xsl:text>: </xsl:text>
      <xsl:apply-templates select="ooclass[position() &gt; 1]" mode="idl"/>
      <xsl:if test="oointerface|ooexception">
        <br/>
	<xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;</xsl:text>
      </xsl:if>
    </xsl:if>
    <xsl:if test="oointerface">
      <xsl:text> implements</xsl:text>
      <xsl:apply-templates select="oointerface" mode="idl"/>
      <xsl:if test="ooexception">
        <br/>
	<xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;</xsl:text>
      </xsl:if>
    </xsl:if>
    <xsl:if test="ooexception">
      <xsl:text> throws</xsl:text>
      <xsl:apply-templates select="ooexception" mode="idl"/>
    </xsl:if>
    <xsl:text>&nbsp;{</xsl:text>
    <br/>
    <xsl:apply-templates select="constructorsynopsis
                                 |destructorsynopsis
                                 |fieldsynopsis
                                 |methodsynopsis
                                 |classsynopsisinfo" mode="idl"/>
    <xsl:text>}</xsl:text>
  </pre>
</xsl:template>

<xsl:template match="classsynopsisinfo" mode="idl">
  <xsl:apply-templates mode="idl"/>
</xsl:template>

<xsl:template match="ooclass|oointerface|ooexception" mode="idl">
  <xsl:if test="position() &gt; 1">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="modifier" mode="idl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="classname" mode="idl">
  <xsl:if test="name(preceding-sibling::*[1]) = 'classname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="interfacename" mode="idl">
  <xsl:if test="name(preceding-sibling::*[1]) = 'interfacename'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="exceptionname" mode="idl">
  <xsl:if test="name(preceding-sibling::*[1]) = 'exceptionname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="fieldsynopsis" mode="idl">
  <code class="{name(.)}">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates mode="idl"/>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<xsl:template match="type" mode="idl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="varname" mode="idl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="initializer" mode="idl">
  <span class="{name(.)}">
    <xsl:text>=&nbsp;</xsl:text>
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="void" mode="idl">
  <span class="{name(.)}">
    <xsl:text>void&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="methodname" mode="idl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="methodparam" mode="idl">
  <xsl:if test="position() &gt; 1">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template match="parameter" mode="idl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="idl"/>
  </span>
</xsl:template>

<xsl:template mode="idl"
  match="constructorsynopsis|destructorsynopsis|methodsynopsis">
  <xsl:variable name="modifiers" select="modifier"/>
  <xsl:variable name="notmod" select="*[name(.) != 'modifier']"/>

  <code class="{name(.)}">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates select="$modifiers" mode="idl"/>

    <!-- type -->
    <xsl:if test="name($notmod[1]) != 'methodname'">
      <xsl:apply-templates select="$notmod[1]" mode="idl"/>
    </xsl:if>

    <xsl:apply-templates select="methodname" mode="idl"/>
    <xsl:text>(</xsl:text>
    <xsl:apply-templates select="methodparam" mode="idl"/>
    <xsl:text>)</xsl:text>
    <xsl:if test="exceptionname">
      <br/>
      <xsl:text>&nbsp;&nbsp;&nbsp;&nbsp;raises(</xsl:text>
      <xsl:apply-templates select="exceptionname" mode="idl"/>
      <xsl:text>)</xsl:text>
    </xsl:if>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<!-- ===== Perl ======================================================== -->

<xsl:template match="classsynopsis" mode="perl">
  <pre class="{name(.)}">
    <xsl:text>package </xsl:text>
    <xsl:apply-templates select="ooclass[1]" mode="perl"/>
    <xsl:text>;</xsl:text>
    <br/>

    <xsl:if test="ooclass[position() &gt; 1]">
      <xsl:text>@ISA = (</xsl:text>
      <xsl:apply-templates select="ooclass[position() &gt; 1]" mode="perl"/>
      <xsl:text>);</xsl:text>
      <br/>
    </xsl:if>

    <xsl:apply-templates select="constructorsynopsis
                                 |destructorsynopsis
                                 |fieldsynopsis
                                 |methodsynopsis
                                 |classsynopsisinfo" mode="perl"/>
  </pre>
</xsl:template>

<xsl:template match="classsynopsisinfo" mode="perl">
  <xsl:apply-templates mode="perl"/>
</xsl:template>

<xsl:template match="ooclass|oointerface|ooexception" mode="perl">
  <xsl:if test="position() &gt; 1">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="modifier" mode="perl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="classname" mode="perl">
  <xsl:if test="name(preceding-sibling::*[1]) = 'classname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="interfacename" mode="perl">
  <xsl:if test="name(preceding-sibling::*[1]) = 'interfacename'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="exceptionname" mode="perl">
  <xsl:if test="name(preceding-sibling::*[1]) = 'exceptionname'">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="fieldsynopsis" mode="perl">
  <code class="{name(.)}">
    <xsl:if test="parent::classsynopsis">
      <xsl:text>&nbsp;&nbsp;</xsl:text>
    </xsl:if>
    <xsl:apply-templates mode="perl"/>
    <xsl:text>;</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<xsl:template match="type" mode="perl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="varname" mode="perl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
    <xsl:text>&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="initializer" mode="perl">
  <span class="{name(.)}">
    <xsl:text>=&nbsp;</xsl:text>
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="void" mode="perl">
  <span class="{name(.)}">
    <xsl:text>void&nbsp;</xsl:text>
  </span>
</xsl:template>

<xsl:template match="methodname" mode="perl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="methodparam" mode="perl">
  <xsl:if test="position() &gt; 1">
    <xsl:text>, </xsl:text>
  </xsl:if>
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template match="parameter" mode="perl">
  <span class="{name(.)}">
    <xsl:apply-templates mode="perl"/>
  </span>
</xsl:template>

<xsl:template mode="perl"
  match="constructorsynopsis|destructorsynopsis|methodsynopsis">
  <xsl:variable name="modifiers" select="modifier"/>
  <xsl:variable name="notmod" select="*[name(.) != 'modifier']"/>

  <code class="{name(.)}">
    <xsl:text>sub </xsl:text>

    <xsl:apply-templates select="methodname" mode="perl"/>
    <xsl:text> { ... };</xsl:text>
  </code>
  <xsl:call-template name="synop-break"/>
</xsl:template>

<!-- ==================================================================== -->

</xsl:stylesheet>
