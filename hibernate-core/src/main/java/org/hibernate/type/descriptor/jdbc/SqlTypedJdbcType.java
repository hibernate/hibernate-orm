/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

/**
 * A {@link JdbcType} with a fixed SQL type name.
 *
 * @see StructJdbcType
 * @see org.hibernate.dialect.OracleArrayJdbcType
 * @see org.hibernate.dialect.OracleNestedTableJdbcType
 */
public interface SqlTypedJdbcType extends JdbcType {

	String getSqlTypeName();
}
