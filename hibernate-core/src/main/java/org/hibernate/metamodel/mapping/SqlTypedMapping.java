/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.jdbc.Size;

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
	Integer getTemporalPrecision();
	default boolean isLob() {
		return getJdbcMapping().getJdbcType().isLob();
	}
	JdbcMapping getJdbcMapping();

	default Size toSize() {
		final Size size = new Size();
		size.setLength( getLength() );
		if ( getTemporalPrecision() != null ) {
			size.setPrecision( getTemporalPrecision() );
		}
		else {
			size.setPrecision( getPrecision() );
		}
		size.setScale( getScale() );
		return size;
	}
}
