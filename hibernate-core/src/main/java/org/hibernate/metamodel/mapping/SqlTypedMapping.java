/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

/**
 * Models the type of a thing that can be used as an expression in a SQL query
 *
 * @author Christian Beikov
 */
public interface SqlTypedMapping {
	String getColumnDefinition();
	Long getLength();
	Integer getPrecision();
	Integer getScale();
	default boolean isLob() {
		return getJdbcMapping().getJdbcType().isLob();
	}
	JdbcMapping getJdbcMapping();
}
