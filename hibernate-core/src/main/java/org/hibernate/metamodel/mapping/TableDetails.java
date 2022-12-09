/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.metamodel.mapping;

import java.util.List;

/**
 * Details about a table
 *
 * @author Steve Ebersole
 */
public interface TableDetails {
	/**
	 * The name of the table
	 */
	String getTableName();

	/**
	 * Whether this table is the root for a given {@link ModelPartContainer}
	 */
	boolean isIdentifierTable();

	/**
	 * Details about the primary key of a table
	 */
	interface KeyDetails {
		/**
		 * Number of columns
		 */
		int getColumnCount();

		/**
		 * Group of columns defined on the primary key
		 */
		List<KeyColumn> getKeyColumns();

		/**
		 * Get a key column by relative position
		 */
		KeyColumn getKeyColumn(int position);

		/**
		 * Visit each key column
		 */
		void forEachKeyColumn(KeyColumnConsumer consumer);
	}

	/**
	 * Details about a column within the key group
	 */
	interface KeyColumn {
		/**
		 * The name of the column
		 */
		String getColumnName();

		/**
		 * Describes the mapping between object and relational for this column
		 */
		JdbcMapping getJdbcMapping();
	}

	@FunctionalInterface
	interface KeyColumnConsumer {
		/**
		 * Callback a particular key column
		 *
		 * @param position The position of the column within the key group
		 * @param column The column details
		 */
		void consume(int position, KeyColumn column);
	}
}
