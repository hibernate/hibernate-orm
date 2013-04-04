package org.hibernate.ejb.criteria.jpaMapMode;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public class Table {
    private final String tableName;
    private final String idColumnName;
    private DocumentTableMapping documentMapping;
    private final Set<Column> columns = new HashSet<Column>();

    public Table(String tableName, String idColumnName,
                 DocumentTableMapping documentMapping, Set<Column> columns) {
        super();
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
        return tableName;
    }

    public Set<Column> getColumns() {
        return Collections.unmodifiableSet(columns);
    }

    @Override
    public int hashCode() {
        return tableName.hashCode();
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

        return this.tableName.equals(that.tableName);
    }

    @Override
    public String toString() {
        return tableName;
    }

}