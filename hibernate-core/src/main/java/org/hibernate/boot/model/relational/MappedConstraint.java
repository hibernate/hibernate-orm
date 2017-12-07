/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.relational;

import java.util.List;

import org.hibernate.mapping.Column;

/**
 * Contract for a relational constraint.
 *
 * @author Chris Cranford
 */
public interface MappedConstraint {
	/**
	 * Return the name of the constraint.
	 */
	String getName();

	/**
	 * Set the name of the constraint.
	 */
	void setName(String name);

	/**
	 * Get the column span.
	 */
	default int getColumnSpan() {
		return getColumns().size();
	}

	/**
	 * Get the columns associated with the constraint.
	 */
	List<Column> getColumns();

	/**
	 * Add a column to the constraint.
	 *
	 * @param column The column
	 */
	void addColumn(Column column);

	/**
	 * Add a list fo columns to the constraint.
	 *
	 * @param columns The list of mapped columns.
	 */
	void addColumns(List<? extends MappedColumn> columns);

	/**
	 * Get a specific column at an index.
	 *
	 * @param columnIndex The column index.
	 */
	Column getColumn(int columnIndex);
}
