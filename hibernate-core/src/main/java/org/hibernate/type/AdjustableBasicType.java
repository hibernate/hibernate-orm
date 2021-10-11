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
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptorIndicators;

/**
 * Extension contract for BasicType implementations that understand how to
 * adjust themselves relative to where/how they are used (e.g. accounting
 * for LOB, nationalized, primitive/wrapper, etc).
 */
public interface AdjustableBasicType<J> extends BasicType<J> {
	/**
	 * Perform the adjustment
	 */
	default <X> BasicType<X> resolveIndicatedType(JdbcTypeDescriptorIndicators indicators, JavaType<X> domainJtd) {
		final JdbcType jdbcType = getJdbcTypeDescriptor();
		if ( jdbcType instanceof AdjustableJdbcType ) {
			final JdbcType resolvedJdbcType = ( (AdjustableJdbcType) jdbcType ).resolveIndicatedType(
					indicators,
					domainJtd
			);
			if ( resolvedJdbcType != jdbcType ) {
				return indicators.getTypeConfiguration().getBasicTypeRegistry()
						.resolve( domainJtd, resolvedJdbcType, getName() );
			}
		}
		return (BasicType<X>) this;
	}
}
