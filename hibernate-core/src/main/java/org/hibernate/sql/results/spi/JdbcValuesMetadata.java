/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Access to information about the underlying JDBC values
 * such as type, position, column name, etc
 */
public interface JdbcValuesMetadata {
	/**
	 * Number of values in the underlying result
	 */
	int getColumnCount();

	/**
	 * Position of a particular result value by name
	 */
	int resolveColumnPosition(String columnName);

	/**
	 * Name of a particular result value by position
	 */
	String resolveColumnName(int position);

	/**
	 * Descriptor of the JDBC/SQL type of a particular result value by
	 * position
	 */
	SqlTypeDescriptor resolveSqlTypeDescriptor(int position);

}
