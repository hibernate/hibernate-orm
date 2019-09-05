/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;

/**
 * Models the type of a thing that can be used as an expression in a SQL query
 *
 * @author Steve Ebersole
 */
public interface JdbcMapping {
	/**
	 * The descriptor for the Java type represented by this
	 * expressable type
	 */
	JavaTypeDescriptor getJavaTypeDescriptor();

	/**
	 * The descriptor for the SQL type represented by this
	 * expressable type
	 */
	SqlTypeDescriptor getSqlTypeDescriptor();


	/**
	 * The strategy for extracting values of this expressable
	 * type from JDBC ResultSets, CallableStatements, etc
	 */
	ValueExtractor getJdbcValueExtractor();

	/**
	 * The strategy for binding values of this expressable
	 * type to JDBC PreparedStatements, CallableStatements, etc
	 */
	ValueBinder getJdbcValueBinder();
}
