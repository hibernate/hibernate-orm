/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.descriptor.jdbc.internal;

import java.time.temporal.Temporal;

import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JavaTimeJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractJavaTimeJdbcType<T extends Temporal> implements JavaTimeJdbcType {
	private final Class<T> javaTimeType;

	public AbstractJavaTimeJdbcType(Class<T> javaTimeType) {
		this.javaTimeType = javaTimeType;
	}

	@Override
	public Class<T> getPreferredJavaTypeClass(WrapperOptions options) {
		return javaTimeType;
	}

	@Override
	public <X> JavaType<X> getJdbcRecommendedJavaTypeMapping(
			Integer precision,
			Integer scale,
			TypeConfiguration typeConfiguration) {
		return typeConfiguration.getJavaTypeRegistry().getDescriptor( javaTimeType );
	}

	@Override
	public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
		return new SetObjectBinder<>( javaType, this, javaTimeType, getDdlTypeCode() );
	}

	@Override
	public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
		return new GetObjectExtractor<>( javaType, this, javaTimeType );
	}
}
