/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;

/**
 * Extension contract for {@link BasicType} implementations which understand how to
 * adjust themselves relative to where/how they're used by, for example, accounting
 * for LOB, nationalized, primitive/wrapper, etc.
 */
public interface AdjustableBasicType<J> extends BasicType<J> {
	/**
	 * Perform the adjustment
	 */
	default <X> BasicType<X> resolveIndicatedType(JdbcTypeIndicators indicators, JavaType<X> domainJtd) {
		final JdbcType jdbcType = getJdbcType();
		if ( jdbcType instanceof AdjustableJdbcType adjustableJdbcType ) {
			final JdbcType resolvedJdbcType = adjustableJdbcType.resolveIndicatedType(
					indicators,
					domainJtd
			);
			if ( getJavaTypeDescriptor() != domainJtd || resolvedJdbcType != jdbcType ) {
				return indicators.getTypeConfiguration().getBasicTypeRegistry()
						.resolve( domainJtd, resolvedJdbcType );
			}
		}
		else {
			final int resolvedJdbcTypeCode = indicators.resolveJdbcTypeCode( jdbcType.getDefaultSqlTypeCode() );
			if ( getJavaTypeDescriptor() != domainJtd || resolvedJdbcTypeCode != jdbcType.getDefaultSqlTypeCode() ) {
				return indicators.getTypeConfiguration().getBasicTypeRegistry()
						.resolve( domainJtd, indicators.getJdbcType( resolvedJdbcTypeCode ) );
			}
		}
		return (BasicType<X>) this;
	}
}
