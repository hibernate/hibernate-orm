package org.hibernate.ejb.criteria.jpaMapMode;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class Table {
    private final String schemaName;
    private final String tableName;
    private final String idColumnName;
    private DocumentTableMapping documentMapping;
    private final Set<Column> columns = new HashSet<>();

    public Table(String schemaName, String tableName, String idColumnName,
                 DocumentTableMapping documentMapping, Set<Column> columns) {
        super();
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.idColumnName = idColumnName;
        this.documentMapping = documentMapping;
        this.columns.addAll(columns);
    }

    public DocumentTableMapping getDocumentMapping() {
        return documentMapping;
    }

    public void setDocumentMapping(DocumentTableMapping documentMapping) {
        assert this.documentMapping == null;
        this.documentMapping = documentMapping;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getTableName() {
        return tableName;
    }

    public String getIdColumnName() {
        return idColumnName;
    }

    /**
     * This should be compliant with any database out there.
     *
     * @return The SQL name of the table, either "schema.table" or "table".
     */
    public String getSqlName() {
        return StringUtils.isBlank(schemaName) ? tableName : schemaName + "."
                + tableName;
    }

    public Set<Column> getColumns() {
        return Collections.unmodifiableSet(columns);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(schemaName).append(tableName)
                .toHashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Table)) {
            return false;
        }

        Table that = (Table) o;

        return new EqualsBuilder().append(this.schemaName, that.schemaName)
                .append(this.tableName, that.tableName).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

}