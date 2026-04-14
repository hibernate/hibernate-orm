<#if templateHelper.hasClassDescription()>
/**
 * ${templateHelper.getClassDescription()}
 */
</#if>
<#assign classAnnotations = templateHelper.generateClassAnnotations()>
<#if classAnnotations?has_content>
${classAnnotations}
</#if>
public ${templateHelper.getDeclarationType()} ${templateHelper.getDeclarationName()} ${templateHelper.getExtendsDeclaration()}${templateHelper.getImplementsDeclaration()}