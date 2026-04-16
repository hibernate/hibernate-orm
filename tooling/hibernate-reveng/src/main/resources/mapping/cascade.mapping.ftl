                <cascade>
<#list cascadeTypes as ct>
                    <cascade-${ct.name()?lower_case?replace("_", "-")}/>
</#list>
                </cascade>
