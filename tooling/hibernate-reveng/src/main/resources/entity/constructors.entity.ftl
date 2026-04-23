    public ${templateHelper.getDeclarationName()}() {
    }
<#if constructors.needsMinimalConstructor()>

    public ${templateHelper.getDeclarationName()}(${constructors.getMinimalConstructorParameterList()}) {
<#if (constructors.getSuperclassMinimalConstructorProperties()?size > 0)>
        super(${constructors.getSuperclassMinimalConstructorArgumentList()});
</#if>
<#list constructors.getMinimalConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>
<#if constructors.needsFullConstructor()>

    public ${templateHelper.getDeclarationName()}(${constructors.getFullConstructorParameterList()}) {
<#if (constructors.getSuperclassFullConstructorProperties()?size > 0)>
        super(${constructors.getSuperclassFullConstructorArgumentList()});
</#if>
<#list constructors.getFullConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>