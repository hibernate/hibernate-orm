/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.jdbc.Size;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Models the type of a thing that can be used as an expression in a SQL query
 *
 * @author Christian Beikov
 */
public interface SqlTypedMapping {
	@Nullable String getColumnDefinition();
	@Nullable Long getLength();
	@Nullable Integer getPrecision();
	@Nullable Integer getScale();
	@Nullable Integer getTemporalPrecision();
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
