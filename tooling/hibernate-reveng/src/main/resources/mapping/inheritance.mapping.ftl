<#if entityInfo.hasInheritance()>
        <inheritance strategy="${entityInfo.getInheritanceStrategy()}"/>
<#if entityInfo.getDiscriminatorColumnName()??>
        <discriminator-column name="${entityInfo.getDiscriminatorColumnName()}"<#if entityInfo.getDiscriminatorType()??> discriminator-type="${entityInfo.getDiscriminatorType()}"</#if><#if (entityInfo.getDiscriminatorColumnLength() > 0)> length="${entityInfo.getDiscriminatorColumnLength()?c}"</#if>/>
</#if>
</#if>
<#if entityInfo.getDiscriminatorValue()??>
        <discriminator-value>${entityInfo.getDiscriminatorValue()}</discriminator-value>
</#if>
<#list entityInfo.getPrimaryKeyJoinColumnNames() as pkjcName>
        <primary-key-join-column name="${pkjcName}"/>
</#list>
