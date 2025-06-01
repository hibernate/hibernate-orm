/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.mapping;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Internal;

import static org.hibernate.internal.util.StringHelper.qualify;

/**
 * A mapping model object representing a primary key constraint.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class PrimaryKey extends Constraint {

	private UniqueKey orderingUniqueKey = null;
	private int[] originalOrder;

	public PrimaryKey(Table table) {
		super( table );
	}

	@Deprecated(since = "7")
	public PrimaryKey() {
	}

	@Override
	public void addColumn(Column column) {
		// force primary key columns to not-null
		for ( Column next : getTable().getColumns() ) {
			if ( next.getCanonicalName().equals( column.getCanonicalName() ) ) {
				next.setNullable( false );
			}
		}
		super.addColumn( column );
	}

	@Override
	public String getExportIdentifier() {
		return qualify( getTable().getExportIdentifier(), "PK-" + getName() );
	}

	public List<Column> getColumnsInOriginalOrder() {
		final List<Column> columns = getColumns();
		if ( originalOrder == null ) {
			return columns;
		}
		final Column[] columnsInOriginalOrder = new Column[columns.size()];
		for ( int i = 0; i < columnsInOriginalOrder.length; i++ ) {
			columnsInOriginalOrder[originalOrder[i]] = columns.get( i );
		}
		return Arrays.asList( columnsInOriginalOrder );
	}

	public void setOrderingUniqueKey(UniqueKey uniqueKey) {
		orderingUniqueKey = uniqueKey;
	}

	public UniqueKey getOrderingUniqueKey() {
		return orderingUniqueKey;
	}

	@Internal
	public void reorderColumns(List<Column> reorderedColumns) {
		final List<Column> columns = getColumns();
		if ( originalOrder != null ) {
			assert columns.equals( reorderedColumns );
			return;
		}
		assert columns.size() == reorderedColumns.size() && columns.containsAll( reorderedColumns );
		originalOrder = new int[columns.size()];
		final UniqueKey orderingUniqueKey = getOrderingUniqueKey();
		final List<Column> newColumns =
				orderingUniqueKey != null
						? orderingUniqueKey.getColumns()
						: reorderedColumns;
		for ( int i = 0; i < newColumns.size(); i++ ) {
			final Column reorderedColumn = newColumns.get( i );
			originalOrder[i] = columns.indexOf( reorderedColumn );
		}
		columns.clear();
		columns.addAll( newColumns );
	}

	@Internal
	public int[] getOriginalOrder() {
		return originalOrder;
	}
}
