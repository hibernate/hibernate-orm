<#if meta.hasClassDescription()>
/**
 * ${meta.getClassDescription()}
 */
</#if>
<#assign classAnnotations = classInfo.generate()>
<#if classAnnotations?has_content>
${classAnnotations}
</#if>
public ${templateHelper.getDeclarationType()} ${templateHelper.getDeclarationName()} ${templateHelper.getExtendsDeclaration()}${templateHelper.getImplementsDeclaration()}