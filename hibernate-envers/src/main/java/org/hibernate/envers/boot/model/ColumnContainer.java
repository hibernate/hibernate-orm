/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.boot.model;

import java.util.List;

import org.hibernate.mapping.Selectable;
import org.hibernate.mapping.Value;

/**
 * Contract for a container of columns.
 *
 * @author Chris Cranford
 */
public interface ColumnContainer {

	/**
	 * Get all columns that are part of this property
	 * @return unmodifiable list of property columns
	 */
	List<Column> getColumns();

	/**
	 * Add a column to the container.
	 *
	 * @param column the column, must not be {@code null}
	 */
	void addColumn(Column column);

	/**
	 * Takes a {@link Value} and injects its columns into the Envers container mapping.
	 *
	 * @param value the value mapping
	 */
	default void addColumnsFromValue(Value value) {
		final List<Selectable> selectables = value.getSelectables();
		for ( Selectable s : selectables ) {
			addColumn( Column.from( s ) );
		}
	}
}
