package org.hibernate.cfg;

import org.hibernate.mapping.Table;

/**
 * A list of columns that are mapped to a single Java property
 * or field. This is a slightly uncomfortable abstraction here,
 * because this concept is arguably missing from JPA (where
 * there's no equivalent of the Hibernate-defined
 * {@link org.hibernate.annotations.Columns} annotation) and
 * so JPA lets each {@link jakarta.persistence.Column} specify
 * its own {@link jakarta.persistence.Column#table table}.
 * That leaves us having to enforce the requirement that every
 * column mapped to a given property must belong to the same
 * table.
 *
 * @author Gavin King
 */
public class AnnotatedColumns {
    private AnnotatedColumn[] columns;
    private Table table;
    private PropertyHolder propertyHolder;

    public void setColumns(AnnotatedColumn[] columns) {
        this.columns = columns;
        if ( columns != null ) {
            for ( AnnotatedColumn column : columns ) {
                column.setParent( this );
            }
        }
    }

    public AnnotatedColumn[] getColumns() {
        return columns;
    }

    public PropertyHolder getPropertyHolder() {
        return propertyHolder;
    }

    public void setPropertyHolder(PropertyHolder propertyHolder) {
        this.propertyHolder = propertyHolder;
    }

    public Table getTable() {
        if ( table != null ) {
            return table;
        }
        else {
            // all the columns have to be mapped to the same table
            // even though at the annotation level it looks like
            // they could each specify a different table
            final AnnotatedColumn firstColumn = columns[0];
            return firstColumn.isSecondary()
                    ? firstColumn.getJoin().getTable()
                    : firstColumn.getPropertyHolder().getTable();
        }
    }

    public void setTable(Table table) {
        this.table = table;
    }

    @Deprecated
    void setTableInternal(Table table) {
        this.table = table;
    }
}
