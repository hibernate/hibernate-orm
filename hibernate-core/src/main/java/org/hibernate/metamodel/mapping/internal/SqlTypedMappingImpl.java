/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * @author Christian Beikov
 */
public class SqlTypedMappingImpl implements SqlTypedMapping {

	private final @Nullable String columnDefinition;
	private final @Nullable Long length;
	private final @Nullable Integer precision;
	private final @Nullable Integer scale;
	private final @Nullable Integer temporalPrecision;
	private final JdbcMapping jdbcMapping;

	public SqlTypedMappingImpl(JdbcMapping jdbcMapping) {
		this( null, null, null, null, null, jdbcMapping );
	}

	public SqlTypedMappingImpl(
			@Nullable String columnDefinition,
			@Nullable Long length,
			@Nullable Integer precision,
			@Nullable Integer scale,
			@Nullable Integer temporalPrecision,
			JdbcMapping jdbcMapping) {
		// Save memory by using interned strings. Probability is high that we have multiple duplicate strings
		this.columnDefinition = columnDefinition == null ? null : columnDefinition.intern();
		this.length = length;
		this.precision = precision;
		this.scale = scale;
		this.temporalPrecision = temporalPrecision;
		this.jdbcMapping = jdbcMapping;
	}

	@Override
	public @Nullable String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public @Nullable Long getLength() {
		return length;
	}

	@Override
	public @Nullable Integer getPrecision() {
		return precision;
	}

	@Override
	public @Nullable Integer getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public @Nullable Integer getScale() {
		return scale;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
