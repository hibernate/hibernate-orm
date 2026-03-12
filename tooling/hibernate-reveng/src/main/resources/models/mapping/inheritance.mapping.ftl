<#if table.getInheritance()??>
<#assign inh = table.getInheritance()>
        <inheritance strategy="${inh.getStrategy().name()}"/>
<#if inh.getDiscriminatorColumnName()??>
        <discriminator-column name="${inh.getDiscriminatorColumnName()}"<#if inh.getDiscriminatorType()??> discriminator-type="${inh.getDiscriminatorType().name()}"</#if><#if (inh.getDiscriminatorColumnLength() > 0)> length="${inh.getDiscriminatorColumnLength()?c}"</#if>/>
</#if>
</#if>
<#if table.getDiscriminatorValue()??>
        <discriminator-value>${table.getDiscriminatorValue()}</discriminator-value>
</#if>
<#if table.getPrimaryKeyJoinColumnName()??>
        <primary-key-join-column name="${table.getPrimaryKeyJoinColumnName()}"/>
</#if>
