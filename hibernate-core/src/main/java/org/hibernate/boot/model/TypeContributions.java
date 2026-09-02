/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;


import jakarta.persistence.AttributeConverter;
import org.hibernate.Incubating;
import org.hibernate.SPI;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.SPI.Role.USE;

/// Registration callbacks used by a [TypeContributor] or
/// [org.hibernate.dialect.Dialect] to contribute custom types and descriptors
/// to the eventual [TypeConfiguration].
///
/// Use only the callbacks needed by the contributed contracts and complete all
/// registration during bootstrap. Implementations and contributors must not
/// retain this lifecycle-scoped object.
///
/// @see TypeContributor
///
/// @author Steve Ebersole
@SPI(USE)
public interface TypeContributions {
	/**
	 * The {@link TypeConfiguration} to contribute to
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Add the given {@link JavaType} to the {@link JavaTypeRegistry}
	 * of the eventual {@link TypeConfiguration}.
	 * @see JavaType
	 */
	@SPI(SUPPLY)
	default void contributeJavaType(JavaType<?> descriptor) {
		getTypeConfiguration().getJavaTypeRegistry().addDescriptor( descriptor );
	}

	/**
	 * Add the given {@link JdbcType} to the {@link JdbcTypeRegistry}
	 * of the eventual {@link TypeConfiguration}.
	 * @see JdbcType
	 */
	/// Contribute a parameterized JDBC type constructor.
	/// @see JdbcTypeConstructor
	@SPI(SUPPLY)
	default void contributeJdbcType(JdbcType descriptor) {
		getTypeConfiguration().getJdbcTypeRegistry().addDescriptor( descriptor );
	}

	@SPI(SUPPLY)
	default void contributeJdbcTypeConstructor(JdbcTypeConstructor typeConstructor) {
		getTypeConfiguration().getJdbcTypeRegistry().addTypeConstructor( typeConstructor );
	}

	/**
	 * Register a {@link UserType} as the implicit (auto-applied)
	 * type for values of type {@link UserType#returnedClass()}.
	 */
	@SPI(SUPPLY)
	default void contributeType(UserType<?> type) {
		contributeType( type, type.returnedClass().getTypeName() );
	}

	/**
	 * Register a {@link CompositeUserType} as the implicit (auto-applied)
	 * type for values of type {@link CompositeUserType#returnedClass()}.
	 *
	 * @since 6.4
	 */
	@Incubating
	@SPI(SUPPLY)
	default void contributeType(CompositeUserType<?> type) {
		// default implementation for backward compatibility
		throw new UnsupportedOperationException();
	}

	/**
	 * Register an {@link AttributeConverter} class.
	 *
	 * @since 6.2
	 */
	@Incubating
	@SPI(SUPPLY)
	default void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass)  {
		// default implementation for backward compatibility
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated See discussion of {@link TypeContributor} in User Guide.
	 */
	@Deprecated(since = "6.0")
	@SPI(SUPPLY)
	default void contributeType(BasicType<?> type) {
		getTypeConfiguration().getBasicTypeRegistry().register( type );
		final JavaType<?> javaType = type.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType );
	}

	/**
	 * @deprecated Use {@link #contributeType(BasicType)} instead.
	 */
	@Deprecated(since = "5.3")
	@SPI(SUPPLY)
	default void contributeType(BasicType<?> type, String... keys) {
		getTypeConfiguration().getBasicTypeRegistry().register( type, keys );
		final JavaType<?> javaType = type.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType );
	}

	/**
	 * @deprecated Use {@link #contributeType(BasicType)} instead.
	 */
	@Deprecated(since = "5.3")
	@SPI(SUPPLY)
	default void contributeType(UserType<?> type, String... keys) {
		final CustomType<?> customType = getTypeConfiguration().getBasicTypeRegistry().register( type, keys );
		final JavaType<?> javaType = customType.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType );
	}
}
