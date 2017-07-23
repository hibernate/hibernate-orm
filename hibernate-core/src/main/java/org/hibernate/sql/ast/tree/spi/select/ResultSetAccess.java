/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.select;

import java.sql.ResultSet;

import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Access to a JDBC ResultSet and information about it.
 *
 * @author Steve Ebersole
 */
public interface ResultSetAccess {
	ResultSet getResultSet();

	int getColumnCount();

	int resolveColumnPosition(String columnName);

	String resolveColumnName(int position);

	SqlTypeDescriptor resolveSqlTypeDescriptor(int position);

	void release();
}
