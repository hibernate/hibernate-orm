/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.SPI;
import org.hibernate.type.descriptor.jdbc.AdjustableJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcTypeIndicators;
import org.hibernate.type.descriptor.jdbc.JdbcTypeJavaClassMappings;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Specializes [JavaType] for basic values in the sense of
/// [jakarta.persistence.metamodel.Type.PersistenceType#BASIC].
///
/// Providers supply basic Java descriptors through the same contribution and
/// registry methods as every [JavaType].
///
/// @see org.hibernate.boot.model.TypeContributions#contributeJavaType(JavaType)
/// @see org.hibernate.type.descriptor.java.spi.JavaTypeRegistry#addDescriptor(JavaType)
/// @see org.hibernate.type.descriptor.java.spi.JavaTypeRegistry#resolveDescriptor(Class, java.util.function.Supplier)
/// @see org.hibernate.type.descriptor.java.spi.JavaTypeRegistry#resolveDescriptor(JavaType)
/// @see JavaType#createJavaType(java.lang.reflect.ParameterizedType, org.hibernate.type.spi.TypeConfiguration)
/// @see org.hibernate.annotations.AnyKeyJavaType#value()
/// @see org.hibernate.annotations.CollectionIdJavaType#value()
/// @see org.hibernate.annotations.JavaType#value()
/// @see org.hibernate.annotations.JavaTypeRegistration#descriptorClass()
/// @see org.hibernate.annotations.ListIndexJavaType#value()
/// @see org.hibernate.annotations.MapKeyJavaType#value()
/// @see org.hibernate.mapping.BasicValue#setExplicitJavaTypeAccess(java.util.function.Function)
@SPI({ USE, IMPLEMENT, SUPPLY })
public interface BasicJavaType<T> extends JavaType<T> {
	/**
	 * Obtain the "recommended" {@linkplain JdbcType SQL type descriptor}
	 * for this Java type. Often, but not always, the source of this
	 * recommendation is the JDBC specification.
	 *
	 * @param indicators Contextual information
	 *
	 * @return The recommended SQL type descriptor
	 */
	default JdbcType getRecommendedJdbcType(JdbcTypeIndicators indicators) {
		// match legacy behavior
		final int jdbcTypeCode = JdbcTypeJavaClassMappings.INSTANCE.determineJdbcTypeCodeForJavaClass( getJavaTypeClass() );
		final var descriptor = indicators.getJdbcType( indicators.resolveJdbcTypeCode( jdbcTypeCode ) );
		return descriptor instanceof AdjustableJdbcType adjustableJdbcType
				? adjustableJdbcType.resolveIndicatedType( indicators, this )
				: descriptor;
	}

	@Override
	default T fromString(CharSequence string) {
		throw new UnsupportedOperationException( "Type " + getTypeName()
						+ " does not support conversion from String");
	}
}
