package org.hibernate.cfg;

import org.hibernate.AnnotationException;
import org.hibernate.AssertionFailure;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.Table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.unmodifiableList;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;

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
    private final List<AnnotatedColumn> columns = new ArrayList<>();
    private Table table;
    private PropertyHolder propertyHolder;
    private Map<String, Join> joins = Collections.emptyMap();
    private MetadataBuildingContext buildingContext;

    public List<AnnotatedColumn> getColumns() {
        return unmodifiableList( columns );
    }

    public PropertyHolder getPropertyHolder() {
        return propertyHolder;
    }

    public void setPropertyHolder(PropertyHolder propertyHolder) {
        this.propertyHolder = propertyHolder;
    }

    public void setBuildingContext(MetadataBuildingContext buildingContext) {
        this.buildingContext = buildingContext;
    }

    public MetadataBuildingContext getBuildingContext() {
        return buildingContext;
    }

    public void setJoins(Map<String, Join> joins) {
        this.joins = joins;
    }

    public Join getJoin() {
        final AnnotatedColumn firstColumn = columns.get(0);
        final String explicitTableName = firstColumn.getExplicitTableName();
        Join join = joins.get( explicitTableName );
        if ( join == null ) {
            // annotation binding seems to use logical and physical naming somewhat inconsistently...
            final String physicalTableName = getBuildingContext().getMetadataCollector()
                    .getPhysicalTableName( explicitTableName );
            if ( physicalTableName != null ) {
                join = joins.get( physicalTableName );
            }
        }
        if ( join == null ) {
            throw new AnnotationException(
                    "Secondary table '" + explicitTableName + "' for property '" + getPropertyHolder().getClassName()
                            + "' is not declared (use '@SecondaryTable' to declare the secondary table)"
            );
        }
        return join;
    }

    public boolean isSecondary() {
        if ( getPropertyHolder() == null ) {
            throw new AssertionFailure( "Should not call isSecondary() on column w/o persistent class defined" );
        }
        final AnnotatedColumn firstColumn = columns.get(0);
        final String explicitTableName = firstColumn.getExplicitTableName();
        return isNotEmpty( explicitTableName )
                && !getPropertyHolder().getTable().getName().equals( explicitTableName );
    }

    public Table getTable() {
        if ( table != null ) {
            return table;
        }
        else {
            // all the columns have to be mapped to the same table
            // even though at the annotation level it looks like
            // they could each specify a different table
            final AnnotatedColumn firstColumn = columns.get(0);
            return firstColumn.isSecondary() ? getJoin().getTable() : getPropertyHolder().getTable();
        }
    }

    public void setTable(Table table) {
        this.table = table;
    }

    @Deprecated
    void setTableInternal(Table table) {
        this.table = table;
    }

    public void addColumn(AnnotatedColumn child) {
        columns.add( child );
    }
}
