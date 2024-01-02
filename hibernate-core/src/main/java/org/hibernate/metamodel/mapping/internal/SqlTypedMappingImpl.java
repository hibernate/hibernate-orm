/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SqlTypedMapping;

/**
 * @author Christian Beikov
 */
public class SqlTypedMappingImpl implements SqlTypedMapping {

	private final String columnDefinition;
	private final Long length;
	private final Integer precision;
	private final Integer scale;
	private final Integer temporalPrecision;
	private final JdbcMapping jdbcMapping;

	public SqlTypedMappingImpl(
			String columnDefinition,
			Long length,
			Integer precision,
			Integer scale,
			Integer temporalPrecision,
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
	public String getColumnDefinition() {
		return columnDefinition;
	}

	@Override
	public Long getLength() {
		return length;
	}

	@Override
	public Integer getPrecision() {
		return precision;
	}

	@Override
	public Integer getTemporalPrecision() {
		return temporalPrecision;
	}

	@Override
	public Integer getScale() {
		return scale;
	}

	@Override
	public JdbcMapping getJdbcMapping() {
		return jdbcMapping;
	}
}
