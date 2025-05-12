/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.sql.Types;

import org.hibernate.Incubating;
import org.hibernate.type.spi.TypeConfiguration;

import jakarta.persistence.TemporalType;

/**
 * Specialized JavaType for temporal types.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface TemporalJavaType<T> extends BasicJavaType<T> {

	static int resolveJdbcTypeCode(TemporalType requestedTemporalPrecision) {
		return switch ( requestedTemporalPrecision ) {
			case DATE -> Types.DATE;
			case TIME -> Types.TIME;
			case TIMESTAMP -> Types.TIMESTAMP;
		};
	}

	static Class<?> resolveJavaTypeClass(TemporalType requestedTemporalPrecision) {
		return switch ( requestedTemporalPrecision ) {
			case DATE -> java.sql.Date.class;
			case TIME -> java.sql.Time.class;
			case TIMESTAMP -> java.sql.Timestamp.class;
		};
	}

	/**
	 * The precision represented by this type
	 */
	TemporalType getPrecision();

	/**
	 * Resolve the appropriate TemporalJavaType for the given precision
	 * "relative" to this type.
	 */
	<X> TemporalJavaType<X> resolveTypeForPrecision(
			TemporalType precision,
			TypeConfiguration typeConfiguration);

	@Override
	default boolean isTemporalType() {
		return true;
	}
}
