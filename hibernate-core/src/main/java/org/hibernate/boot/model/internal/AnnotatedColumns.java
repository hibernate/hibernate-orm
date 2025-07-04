/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.AnnotationException;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.mapping.Join;
import org.hibernate.mapping.Property;
import org.hibernate.mapping.Table;

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
	// this is really a .-separated property path
	private String propertyName;
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

	/**
	 * A property path relative to the {@link #getPropertyHolder() PropertyHolder}.
	 */
	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

	Property resolveProperty() {
		return buildingContext.getMetadataCollector().getEntityBindingMap()
				.get( propertyHolder.getPersistentClass().getEntityName() )
				.getReferencedProperty( propertyName );
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
		final AnnotatedColumn firstColumn = columns.get( 0 );
		final String explicitTableName = firstColumn.getExplicitTableName();
		//note: checkPropertyConsistency() is responsible for ensuring they all have the same table name
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
					"Secondary table '" + explicitTableName + "' for property '" + propertyName + "' of entity'" + getPropertyHolder().getClassName()
							+ "' is not declared (use '@SecondaryTable' to declare the secondary table)"
			);
		}
		else {
			return join;
		}
	}

	public boolean isSecondary() {
		final AnnotatedColumn firstColumn = columns.get( 0 );
		final String explicitTableName = firstColumn.getExplicitTableName();
		//note: checkPropertyConsistency() is responsible for ensuring they all have the same table name
		return isNotEmpty( explicitTableName )
			&& !getOwnerTable().getName().equals( explicitTableName );
	}

	/**
	 * Find the table to which these columns belong, taking into account secondary tables.
	 *
	 * @return the {@link Table} given in the code by {@link jakarta.persistence.Column#table()}
	 * 		and held here by {@code explicitTableName}.
	 * @throws AnnotationException if the table named by {@code explicitTableName} is not found
	 * 		among the secondary tables in {@code joins}.
	 */
	public Table getTable() {
		if ( table != null ) {
			return table;
		}
		else {
			// all the columns have to be mapped to the same table
			// even though at the annotation level it looks like
			// they could each specify a different table
			return isSecondary() ? getJoin().getTable() : getOwnerTable();
		}
	}

	private Table getOwnerTable() {
		PropertyHolder holder = getPropertyHolder();
		while ( holder instanceof ComponentPropertyHolder componentPropertyHolder ) {
			holder = componentPropertyHolder.parent;
		}
		return holder.getTable();
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void addColumn(AnnotatedColumn child) {
		columns.add( child );
	}

	public void checkPropertyConsistency() {
		if ( columns.size() > 1 ) {
			for ( int currentIndex = 1; currentIndex < columns.size(); currentIndex++ ) {
				final AnnotatedColumn current = columns.get( currentIndex );
				final AnnotatedColumn previous = columns.get( currentIndex - 1 );
				if ( !current.isFormula() && !previous.isFormula() ) {
					if ( current.isNullable() != previous.isNullable() ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix nullable with 'not null'"
						);
					}
					if ( current.isInsertable() != previous.isInsertable() ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix insertable with 'insertable=false'"
						);
					}
					if ( current.isUpdatable() != previous.isUpdatable() ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix updatable with 'updatable=false'"
						);
					}
					if ( !current.getExplicitTableName().equals( previous.getExplicitTableName() ) ) {
						throw new AnnotationException(
								"Column mappings for property '" + propertyName + "' mix distinct secondary tables"
						);
					}
				}
			}
		}
	}
}
