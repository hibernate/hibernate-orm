/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.relational.naming.spi.PhysicalName;
import org.hibernate.relational.naming.spi.IdentifierComparisonPolicy;

/// Ordered column ownership shared by relational mappings and mapped-superclass declarations.
///
/// @author Steve Ebersole
public abstract class ColumnContainer implements Serializable {
	private final List<Column> columns = new ArrayList<>();
	private transient Map<PhysicalName, Column> columnIndex;

	/// Require a materialized relational mapping at a SQL or key-processing boundary.
	public final Table requireTable() {
		if ( this instanceof Table table ) {
			return table;
		}
		throw new MappingException( "Declaration column container has no relational table: " + this );
	}

	public Column getColumn(Column column) {
		return column == null ? null : getColumn( column.getPhysicalName() );
	}

	public Column getColumn(PhysicalName name) {
		return name == null ? null : index().get( name );
	}

	public Column getColumnByDatabaseName(String name, IdentifierComparisonPolicy policy) {
		Column match = null;
		for ( var column : getColumns() ) {
			if ( policy.matchesDatabaseName( column.getPhysicalName(), name ) ) {
				if ( match != null && match != column ) {
					throw new MappingException( "Ambiguous database column '" + name + "' in " + this );
				}
				match = column;
			}
		}
		return match;
	}

	public Column getColumn(int n) {
		return columns.get( n - 1 );
	}

	public void addColumn(Column column) {
		final var oldColumn = getColumn( column );
		if ( oldColumn == null ) {
			columns.add( column );
			column.registerContainer( this );
			columnIndex = null;
			column.uniqueInteger = columns.size();
		}
		else {
			if ( !column.isNullable() ) {
				oldColumn.setNullable( false );
			}
			else if ( !oldColumn.isNullable() ) {
				column.setNullable( false );
			}
			column.uniqueInteger = oldColumn.uniqueInteger;
		}
	}

	/// Internal boot operation; callers coordinate logical correspondences through BindingState.
	@org.hibernate.Internal
	public final void renameColumn(Column column, PhysicalName replacement) {
		java.util.Objects.requireNonNull( replacement );
		if ( columns.stream().noneMatch( candidate -> candidate == column ) ) {
			throw new MappingException( "Cannot rename an unregistered column in " + this );
		}
		column.validateRename( replacement );
		column.replacePhysicalName( replacement );
	}

	void validateRename(Column column, PhysicalName replacement) {
		final var collision = getColumn( replacement );
		if ( collision != null && collision != column ) {
			throw new MappingException( "Column rename collides with '" + replacement + "' in " + this );
		}
	}

	void invalidateColumnIndex() {
		columnIndex = null;
	}

	private Map<PhysicalName, Column> index() {
		if ( columnIndex == null ) {
			final Map<PhysicalName, Column> rebuilt = new LinkedHashMap<>();
			for ( var column : columns ) {
				final var previous = rebuilt.putIfAbsent( column.getPhysicalName(), column );
				if ( previous != null && previous != column ) {
					throw new MappingException( "Physical column collision between '" + previous.getName()
							+ "' and '" + column.getName() + "' in " + this );
				}
				column.registerContainer( this );
			}
			columnIndex = rebuilt;
		}
		return columnIndex;
	}

	@org.hibernate.Internal
	public void validateColumnIndex() {
		columnIndex = null;
		index();
	}

	public int getColumnSpan() { return columns.size(); }
	public Collection<Column> getColumns() { return java.util.Collections.unmodifiableList( columns ); }
	public boolean containsColumn(Column column) { return getColumn( column ) != null; }

	public void reorderColumns(List<Column> ordering) {
		if ( columns.size() != ordering.size()
				|| ordering.stream().anyMatch( column -> columns.stream().noneMatch( current -> current == column ) )
				|| ordering.stream().distinct().count() != columns.size() ) {
			throw new MappingException( "Column reordering must preserve the registered columns in " + this );
		}
		columns.clear();
		columns.addAll( ordering );
		columnIndex = null;
	}
}
