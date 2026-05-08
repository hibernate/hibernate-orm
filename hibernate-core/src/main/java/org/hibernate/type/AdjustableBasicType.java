/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		if ( jdbcType instanceof AdjustableJdbcType ) {
			final JdbcType resolvedJdbcType = ( (AdjustableJdbcType) jdbcType ).resolveIndicatedType(
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
