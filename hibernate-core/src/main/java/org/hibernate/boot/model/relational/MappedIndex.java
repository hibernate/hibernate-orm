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
 * Contract for relational mapped indexes.
 *
 * @author Chris Cranford
 */
public interface MappedIndex {
	MappedTable getTable();

	void setTable(MappedTable table);

	String getName();

	void setName(String name);

	int getColumnSpan();

	void addColumn(Column column);

	void addColumn(Column column, String order);

	void addColumns(List<? extends MappedColumn> columns);

	List<Column> getColumns();
}
