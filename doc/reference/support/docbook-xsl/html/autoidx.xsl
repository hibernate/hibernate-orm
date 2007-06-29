<?xml version="1.0"?>
<!DOCTYPE xsl:stylesheet [

<!ENTITY lowercase "'abcdefghijklmnopqrstuvwxyz'">
<!ENTITY uppercase "'ABCDEFGHIJKLMNOPQRSTUVWXYZ'">

<!ENTITY primary   'normalize-space(concat(primary/@sortas, primary[not(@sortas)]))'>
<!ENTITY secondary 'normalize-space(concat(secondary/@sortas, secondary[not(@sortas)]))'>
<!ENTITY tertiary  'normalize-space(concat(tertiary/@sortas, tertiary[not(@sortas)]))'>

<!ENTITY section   '(ancestor-or-self::set
                     |ancestor-or-self::book
                     |ancestor-or-self::part
                     |ancestor-or-self::reference
                     |ancestor-or-self::partintro
                     |ancestor-or-self::chapter
                     |ancestor-or-self::appendix
                     |ancestor-or-self::preface
                     |ancestor-or-self::article
                     |ancestor-or-self::section
                     |ancestor-or-self::sect1
                     |ancestor-or-self::sect2
                     |ancestor-or-self::sect3
                     |ancestor-or-self::sect4
                     |ancestor-or-self::sect5
                     |ancestor-or-self::refentry
                     |ancestor-or-self::refsect1
                     |ancestor-or-self::refsect2
                     |ancestor-or-self::refsect3
                     |ancestor-or-self::simplesect
                     |ancestor-or-self::bibliography
                     |ancestor-or-self::glossary
                     |ancestor-or-self::index
                     |ancestor-or-self::webpage)[last()]'>

<!ENTITY section.id 'generate-id(&section;)'>
<!ENTITY sep '" "'>
<!ENTITY scope 'count(ancestor::node()|$scope) = count(ancestor::node())'>
]>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version="1.0">

<!-- ********************************************************************
     $Id$
     ********************************************************************

     This file is part of the XSL DocBook Stylesheet distribution.
     See ../README or http://nwalsh.com/docbook/xsl/ for copyright
     and other information.

     ******************************************************************** -->

<!-- ==================================================================== -->
<!-- Jeni Tennison gets all the credit for what follows.
     I think I understand it :-) Anyway, I've hacked it a bit, so the
     bugs are mine. -->

<xsl:key name="letter"
         match="indexterm"
         use="translate(substring(&primary;, 1, 1),&lowercase;,&uppercase;)"/>

<xsl:key name="primary"
         match="indexterm"
         use="&primary;"/>

<xsl:key name="secondary"
         match="indexterm"
         use="concat(&primary;, &sep;, &secondary;)"/>

<xsl:key name="tertiary"
         match="indexterm"
         use="concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;)"/>

<xsl:key name="endofrange"
         match="indexterm[@class='endofrange']"
         use="@startref"/>

<xsl:key name="primary-section"
         match="indexterm[not(secondary) and not(see)]"
         use="concat(&primary;, &sep;, &section.id;)"/>

<xsl:key name="secondary-section"
         match="indexterm[not(tertiary) and not(see)]"
         use="concat(&primary;, &sep;, &secondary;, &sep;, &section.id;)"/>

<xsl:key name="tertiary-section"
         match="indexterm[not(see)]"
         use="concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;, &sep;, &section.id;)"/>

<xsl:key name="see-also"
         match="indexterm[seealso]"
         use="concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;, &sep;, seealso)"/>

<xsl:key name="see"
         match="indexterm[see]"
         use="concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;, &sep;, see)"/>

<xsl:key name="sections" match="*[@id]" use="@id"/>

<xsl:template name="generate-index">
  <xsl:param name="scope" select="(ancestor::book|/)[last()]"/>

  <xsl:variable name="terms"
                select="//indexterm[count(.|key('letter',
                                                translate(substring(&primary;, 1, 1),
                                                          &lowercase;,
                                                          &uppercase;))[&scope;][1]) = 1
                                    and not(@class = 'endofrange')]"/>

  <xsl:variable name="alphabetical"
                select="$terms[contains(concat(&lowercase;, &uppercase;),
                                        substring(&primary;, 1, 1))]"/>

  <xsl:variable name="others" select="$terms[not(contains(concat(&lowercase;,
                                                 &uppercase;),
                                             substring(&primary;, 1, 1)))]"/>
  <div class="index">
    <xsl:if test="$others">
      <div class="indexdiv">
        <h3>
          <xsl:call-template name="gentext">
            <xsl:with-param name="key" select="'index symbols'"/>
          </xsl:call-template>
        </h3>
        <dl>
          <xsl:apply-templates select="$others[count(.|key('primary',
                                       &primary;)[&scope;][1]) = 1]"
                               mode="index-symbol-div">
            <xsl:with-param name="scope" select="$scope"/>
            <xsl:sort select="translate(&primary;, &lowercase;, &uppercase;)"/>
          </xsl:apply-templates>
        </dl>
      </div>
    </xsl:if>

    <xsl:apply-templates select="$alphabetical[count(.|key('letter',
                                 translate(substring(&primary;, 1, 1),
                                           &lowercase;,&uppercase;))[&scope;][1]) = 1]"
                         mode="index-div">
      <xsl:with-param name="scope" select="$scope"/>
      <xsl:sort select="translate(&primary;, &lowercase;, &uppercase;)"/>
    </xsl:apply-templates>
  </div>
</xsl:template>

<xsl:template match="indexterm" mode="index-div">
  <xsl:param name="scope" select="."/>

  <xsl:variable name="key"
                select="translate(substring(&primary;, 1, 1),&lowercase;,&uppercase;)"/>

  <!-- Make sure that we don't generate a div if there are no terms in scope -->
  <xsl:if test="key('letter', $key)[&scope;]
                [count(.|key('primary', &primary;)[&scope;][1]) = 1]">
    <div class="indexdiv">
      <xsl:if test="contains(concat(&lowercase;, &uppercase;), $key)">
        <h3>
          <xsl:value-of select="translate($key, &lowercase;, &uppercase;)"/>
        </h3>
      </xsl:if>
      <dl>
        <xsl:apply-templates select="key('letter', $key)[&scope;]
                                     [count(.|key('primary', &primary;)[&scope;][1])=1]"
                             mode="index-primary">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:sort select="translate(&primary;, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
      </dl>
    </div>
  </xsl:if>
</xsl:template>

<xsl:template match="indexterm" mode="index-symbol-div">
  <xsl:param name="scope" select="/"/>

  <xsl:variable name="key" select="translate(substring(&primary;, 1, 1),
                                             &lowercase;,&uppercase;)"/>

  <xsl:apply-templates select="key('letter', $key)
                               [count(.|key('primary', &primary;)[&scope;][1]) = 1]"
                       mode="index-primary">
    <xsl:with-param name="scope" select="$scope"/>
    <xsl:sort select="translate(&primary;, &lowercase;, &uppercase;)"/>
  </xsl:apply-templates>
</xsl:template>

<xsl:template match="indexterm" mode="index-primary">
  <xsl:param name="scope" select="."/>

  <xsl:variable name="key" select="&primary;"/>
  <xsl:variable name="refs" select="key('primary', $key)[&scope;]"/>
  <dt>
    <xsl:value-of select="primary"/>
    <xsl:for-each select="$refs[generate-id() = generate-id(key('primary-section', concat($key, &sep;, &section.id;))[&scope;][1])]">
      <xsl:apply-templates select="." mode="reference">
        <xsl:with-param name="scope" select="$scope"/>
      </xsl:apply-templates>
    </xsl:for-each>

    <xsl:if test="$refs[not(secondary)]/*[self::see]">
      <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see', concat(&primary;, &sep;, &sep;, &sep;, see))[&scope;][1])]"
                           mode="index-see">
        <xsl:with-param name="scope" select="$scope"/>
        <xsl:sort select="translate(see, &lowercase;, &uppercase;)"/>
      </xsl:apply-templates>
    </xsl:if>
  </dt>
  <xsl:if test="$refs/secondary or $refs[not(secondary)]/*[self::seealso]">
    <dd>
      <dl>
        <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see-also', concat(&primary;, &sep;, &sep;, &sep;, seealso))[&scope;][1])]"
                             mode="index-seealso">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:sort select="translate(seealso, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="$refs[secondary and count(.|key('secondary', concat($key, &sep;, &secondary;))[&scope;][1]) = 1]" 
                             mode="index-secondary">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:sort select="translate(&secondary;, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
      </dl>
    </dd>
  </xsl:if>
</xsl:template>

<xsl:template match="indexterm" mode="index-secondary">
  <xsl:param name="scope" select="."/>

  <xsl:variable name="key" select="concat(&primary;, &sep;, &secondary;)"/>
  <xsl:variable name="refs" select="key('secondary', $key)[&scope;]"/>
  <dt>
    <xsl:value-of select="secondary"/>
    <xsl:for-each select="$refs[generate-id() = generate-id(key('secondary-section', concat($key, &sep;, &section.id;))[&scope;][1])]">
      <xsl:apply-templates select="." mode="reference">
        <xsl:with-param name="scope" select="$scope"/>
      </xsl:apply-templates>
    </xsl:for-each>

    <xsl:if test="$refs[not(tertiary)]/*[self::see]">
      <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see', concat(&primary;, &sep;, &secondary;, &sep;, &sep;, see))[&scope;][1])]"
                           mode="index-see">
        <xsl:with-param name="scope" select="$scope"/>
        <xsl:sort select="translate(see, &lowercase;, &uppercase;)"/>
      </xsl:apply-templates>
    </xsl:if>
  </dt>
  <xsl:if test="$refs/tertiary or $refs[not(tertiary)]/*[self::seealso]">
    <dd>
      <dl>
        <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see-also', concat(&primary;, &sep;, &secondary;, &sep;, &sep;, seealso))[&scope;][1])]"
                             mode="index-seealso">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:sort select="translate(seealso, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="$refs[tertiary and count(.|key('tertiary', concat($key, &sep;, &tertiary;))[&scope;][1]) = 1]" 
                             mode="index-tertiary">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:sort select="translate(&tertiary;, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
      </dl>
    </dd>
  </xsl:if>
</xsl:template>

<xsl:template match="indexterm" mode="index-tertiary">
  <xsl:param name="scope" select="."/>

  <xsl:variable name="key" select="concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;)"/>
  <xsl:variable name="refs" select="key('tertiary', $key)[&scope;]"/>
  <dt>
    <xsl:value-of select="tertiary"/>
    <xsl:for-each select="$refs[generate-id() = generate-id(key('tertiary-section', concat($key, &sep;, &section.id;))[&scope;][1])]">
      <xsl:apply-templates select="." mode="reference">
        <xsl:with-param name="scope" select="$scope"/>
      </xsl:apply-templates>
    </xsl:for-each>

    <xsl:if test="$refs/see">
      <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see', concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;, &sep;, see))[&scope;][1])]"
                           mode="index-see">
        <xsl:with-param name="scope" select="$scope"/>
        <xsl:sort select="translate(see, &lowercase;, &uppercase;)"/>
      </xsl:apply-templates>
    </xsl:if>
  </dt>
  <xsl:if test="$refs/seealso">
    <dd>
      <dl>
        <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see', concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;, &sep;, see))[&scope;][1])]"
                             mode="index-see">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:sort select="translate(see, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="$refs[generate-id() = generate-id(key('see-also', concat(&primary;, &sep;, &secondary;, &sep;, &tertiary;, &sep;, seealso))[&scope;][1])]"
                             mode="index-seealso">
          <xsl:with-param name="scope" select="$scope"/>
	  <xsl:sort select="translate(seealso, &lowercase;, &uppercase;)"/>
        </xsl:apply-templates>
      </dl>
    </dd>
  </xsl:if>
</xsl:template>

<xsl:template match="indexterm" mode="reference">
  <xsl:param name="scope" select="."/>
  <xsl:param name="separator" select="', '"/>

  <xsl:value-of select="$separator"/>
  <xsl:choose>
    <xsl:when test="@zone and string(@zone)">
      <xsl:call-template name="reference">
        <xsl:with-param name="zones" select="normalize-space(@zone)"/>
          <xsl:with-param name="scope" select="$scope"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <a>
        <xsl:variable name="title">
          <xsl:apply-templates select="&section;" mode="title.markup"/>
        </xsl:variable>

        <xsl:attribute name="href">
          <xsl:call-template name="href.target">
            <xsl:with-param name="object" select="&section;"/>
          </xsl:call-template>
        </xsl:attribute>

        <xsl:value-of select="$title"/> <!-- text only -->
      </a>

      <xsl:if test="key('endofrange', @id)[&scope;]">
        <xsl:apply-templates select="key('endofrange', @id)[&scope;][last()]"
                             mode="reference">
          <xsl:with-param name="scope" select="$scope"/>
          <xsl:with-param name="separator" select="'-'"/>
        </xsl:apply-templates>
      </xsl:if>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="reference">
  <xsl:param name="scope" select="."/>
  <xsl:param name="zones"/>

  <xsl:choose>
    <xsl:when test="contains($zones, ' ')">
      <xsl:variable name="zone" select="substring-before($zones, ' ')"/>
      <xsl:variable name="target" select="key('sections', $zone)[&scope;]"/>

      <a>
        <xsl:attribute name="href">
          <xsl:call-template name="href.target">
            <xsl:with-param name="object" select="$target[1]"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:apply-templates select="$target[1]" mode="index-title-content"/>
      </a>
      <xsl:text>, </xsl:text>
      <xsl:call-template name="reference">
        <xsl:with-param name="zones" select="substring-after($zones, ' ')"/>
        <xsl:with-param name="scope" select="$scope"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:variable name="zone" select="$zones"/>
      <xsl:variable name="target" select="key('sections', $zone)[&scope;]"/>

      <a>
        <xsl:attribute name="href">
          <xsl:call-template name="href.target">
            <xsl:with-param name="object" select="$target[1]"/>
          </xsl:call-template>
        </xsl:attribute>
        <xsl:apply-templates select="$target[1]" mode="index-title-content"/>
      </a>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="indexterm" mode="index-see">
  <xsl:param name="scope" select="."/>

  <xsl:text> (</xsl:text>
  <xsl:call-template name="gentext">
    <xsl:with-param name="key" select="'see'"/>
  </xsl:call-template>
  <xsl:text> </xsl:text>
  <xsl:value-of select="see"/>
  <xsl:text>)</xsl:text>
</xsl:template>

<xsl:template match="indexterm" mode="index-seealso">
  <xsl:param name="scope" select="."/>

  <dt>
  <xsl:text>(</xsl:text>
  <xsl:call-template name="gentext">
    <xsl:with-param name="key" select="'seealso'"/>
  </xsl:call-template>
  <xsl:text> </xsl:text>
  <xsl:value-of select="seealso"/>
  <xsl:text>)</xsl:text>
  </dt>
</xsl:template>

<xsl:template match="*" mode="index-title-content">
  <xsl:variable name="title">
    <xsl:apply-templates select="&section;" mode="title.markup"/>
  </xsl:variable>

  <xsl:value-of select="$title"/>
</xsl:template>

</xsl:stylesheet>
