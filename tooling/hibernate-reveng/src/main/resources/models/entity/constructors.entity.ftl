    public ${templateHelper.getDeclarationName()}() {
    }
<#if templateHelper.needsMinimalConstructor()>

    public ${templateHelper.getDeclarationName()}(${templateHelper.getMinimalConstructorParameterList()}) {
<#if (templateHelper.getSuperclassMinimalConstructorProperties()?size > 0)>
        super(${templateHelper.getSuperclassMinimalConstructorArgumentList()});
</#if>
<#list templateHelper.getMinimalConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>
<#if templateHelper.needsFullConstructor()>

    public ${templateHelper.getDeclarationName()}(${templateHelper.getFullConstructorParameterList()}) {
<#if (templateHelper.getSuperclassFullConstructorProperties()?size > 0)>
        super(${templateHelper.getSuperclassFullConstructorArgumentList()});
</#if>
<#list templateHelper.getFullConstructorProperties() as prop>
        this.${prop.fieldName()} = ${prop.fieldName()};
</#list>
    }
</#if>