<#-- Composite ID field -->
<#if templateHelper.getCompositeIdField()??>
<#assign cid = templateHelper.getCompositeIdField()>
    private ${templateHelper.getJavaTypeName(cid)} ${cid.getName()};

</#if>
<#-- Basic fields (respect gen-property) -->
<#list templateHelper.getBasicFields() as field>
<#if templateHelper.isGenProperty(field)>
<#if templateHelper.hasFieldDescription(field)>
    /**
     * ${templateHelper.getFieldDescription(field)}
     */
</#if>
    private ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#if>
</#list>
<#-- ManyToOne fields -->
<#list templateHelper.getManyToOneFields() as field>
    private ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- OneToOne fields -->
<#list templateHelper.getOneToOneFields() as field>
    private ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- OneToMany fields -->
<#list templateHelper.getOneToManyFields() as field>
    private ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.importType("java.util.HashSet")}<>(0);

</#list>
<#-- ManyToMany fields -->
<#list templateHelper.getManyToManyFields() as field>
    private ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.importType("java.util.HashSet")}<>(0);

</#list>
<#-- Embedded fields -->
<#list templateHelper.getEmbeddedFields() as field>
    private ${templateHelper.getJavaTypeName(field)} ${field.getName()};

</#list>
<#-- ElementCollection fields -->
<#list templateHelper.getElementCollectionFields() as field>
    private ${templateHelper.getCollectionTypeName(field)} ${field.getName()} = new ${templateHelper.importType("java.util.HashSet")}<>(0);

</#list>