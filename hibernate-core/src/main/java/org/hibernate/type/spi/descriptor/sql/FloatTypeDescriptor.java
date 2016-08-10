/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.spi.descriptor.sql;

import java.sql.Types;

import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.type.spi.descriptor.java.JavaTypeDescriptor;

/**
 * Descriptor for {@link Types#FLOAT FLOAT} handling.
 *
 * @author Steve Ebersole
 */
public class FloatTypeDescriptor extends RealTypeDescriptor {
	public static final FloatTypeDescriptor INSTANCE = new FloatTypeDescriptor();

	public FloatTypeDescriptor() {
	}

	@Override
	public int getSqlType() {
		return Types.FLOAT;
	}

	@Override
	public JavaTypeDescriptor getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( Double.class );
	}
}
