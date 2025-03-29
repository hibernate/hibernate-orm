/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JdbcTypeJavaClassMappings;

/**
 * Specializes {@link JavaType} for "basic" values, in the sense of
 * {@link jakarta.persistence.metamodel.Type.PersistenceType#BASIC}.
 */
public interface BasicJavaType<T> extends JavaType<T> {
	/**
	 * Obtain the "recommended" {@link JdbcType SQL type descriptor}
	 * for this Java type. Often, but not always, the source of this
	 * recommendation is the JDBC specification.
	 *
	 * @param indicators Contextual information
	 *
	 * @return The recommended SQL type descriptor
	 */
	default JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		// match legacy behavior
		int jdbcTypeCode = JdbcTypeJavaClassMappings.INSTANCE.determineJdbcTypeCodeForJavaClass( getJavaTypeClass() );
		final JdbcType descriptor = indicators.getJdbcType( indicators.resolveJdbcTypeCode( jdbcTypeCode ) );
		return descriptor instanceof AdjustableJdbcType adjustableJdbcType
				? adjustableJdbcType.resolveIndicatedType( indicators, this )
				: descriptor;
	}

	@Override
	default T fromString(CharSequence string) {
		throw new UnsupportedOperationException();
	}
}
