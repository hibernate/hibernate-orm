
<#--  /** default constructor */ -->
    public ${pojo.getDeclarationName()}() {
    }

<#if pojo.needsMinimalConstructor()>	<#-- /** minimal constructor */ -->
    public ${pojo.getDeclarationName()}(${c2j.asParameterList(pojo.getPropertyClosureForMinimalConstructor(), jdk5, pojo)}) {
<#if pojo.isSubclass() && !pojo.getPropertyClosureForSuperclassMinimalConstructor().isEmpty()>
        super(${c2j.asArgumentList(pojo.getPropertyClosureForSuperclassMinimalConstructor())});        
</#if>
<#list pojo.getPropertiesForMinimalConstructor() as field>
        this.${c2j.keyWordCheck(field.name)} = ${c2j.keyWordCheck(field.name)};
</#list>
    }
</#if>    
<#if pojo.needsFullConstructor()>
<#-- /** full constructor */ -->
    public ${pojo.getDeclarationName()}(${c2j.asParameterList(pojo.getPropertyClosureForFullConstructor(), jdk5, pojo)}) {
<#if pojo.isSubclass() && !pojo.getPropertyClosureForSuperclassFullConstructor().isEmpty()>
        super(${c2j.asArgumentList(pojo.getPropertyClosureForSuperclassFullConstructor())});        
</#if>
<#list pojo.getPropertiesForFullConstructor() as field>
       this.${c2j.keyWordCheck(field.name)} = ${c2j.keyWordCheck(field.name)};
</#list>
    }
</#if>    
