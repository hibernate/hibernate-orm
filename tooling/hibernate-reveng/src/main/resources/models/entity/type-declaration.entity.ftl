<#assign classAnnotations = templateHelper.generateClassAnnotations()>
<#if classAnnotations?has_content>
${classAnnotations}
</#if>
public class ${templateHelper.getDeclarationName()} ${templateHelper.getExtendsDeclaration()}${templateHelper.getImplementsDeclaration()}