<#if helper.hasInheritance()>
        <inheritance strategy="${helper.getInheritanceStrategy()}"/>
<#if helper.getDiscriminatorColumnName()??>
        <discriminator-column name="${helper.getDiscriminatorColumnName()}"<#if helper.getDiscriminatorType()??> discriminator-type="${helper.getDiscriminatorType()}"</#if><#if (helper.getDiscriminatorColumnLength() > 0)> length="${helper.getDiscriminatorColumnLength()?c}"</#if>/>
</#if>
</#if>
<#if helper.getDiscriminatorValue()??>
        <discriminator-value>${helper.getDiscriminatorValue()}</discriminator-value>
</#if>
<#if helper.getPrimaryKeyJoinColumnName()??>
        <primary-key-join-column name="${helper.getPrimaryKeyJoinColumnName()}"/>
</#if>
