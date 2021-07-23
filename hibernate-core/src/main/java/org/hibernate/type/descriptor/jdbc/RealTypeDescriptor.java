/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc;

import java.sql.Types;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Descriptor for {@link Types#REAL REAL} handling.
 *
 * @author Steve Ebersole
 *
 * @deprecated use {@link FloatTypeDescriptor}
 */
@Deprecated
public class RealTypeDescriptor extends FloatTypeDescriptor {
	public static final RealTypeDescriptor INSTANCE = new RealTypeDescriptor();

	public RealTypeDescriptor() {
	}

	@Override
	public int getJdbcType() {
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
	public <T> BasicJavaDescriptor<T> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<T>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Float.class );
	}
}
