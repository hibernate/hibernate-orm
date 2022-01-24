/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.java.BasicJavaType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#REAL REAL} handling.
 *
 * @author Steve Ebersole
 *
 * @deprecated use {@link FloatJdbcType}
 */
@Deprecated
public class RealJdbcType extends FloatJdbcType {
	public static final RealJdbcType INSTANCE = new RealJdbcType();

	public RealJdbcType() {
	}

	@Override
	public int getJdbcTypeCode() {
		return Types.REAL;
	}

	@Override
	public String getFriendlyName() {
		return "REAL";
	}

	@Override
	public String toString() {
		return "RealTypeDescriptor";
	}

	@Override
	public <T> BasicJavaType<T> getJdbcRecommendedJavaTypeMapping(
			Integer length,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return (BasicJavaType<T>) typeConfiguration.getJavaTypeRegistry().getDescriptor( Float.class );
	}
}
