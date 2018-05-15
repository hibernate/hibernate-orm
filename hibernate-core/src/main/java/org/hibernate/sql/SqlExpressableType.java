/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql;

import org.hibernate.type.descriptor.java.spi.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * Models the type of a thing that can be used in a SQL query
 *
 * @author Steve Ebersole
 */
public interface SqlExpressableType {
	/**
	 * The descriptor for the Java type represented by this
	 * expressable type
	 */
	BasicJavaDescriptor getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressable type
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();

	/**
	 * The strategy for extracting values of this expressable
	 * type from JDBC ResultSets, CallableStatements, etc
	 */
	JdbcValueExtractor getJdbcValueExtractor();

	/**
	 * The strategy for binding values of this expressable
	 * type to JDBC PreparedStatements, CallableStatements, etc
	 */
	JdbcValueBinder getJdbcValueBinder();
}
