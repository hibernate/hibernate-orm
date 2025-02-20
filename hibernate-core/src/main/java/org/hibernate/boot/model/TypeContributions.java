/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import jakarta.persistence.AttributeConverter;
import org.hibernate.Incubating;
import org.hibernate.type.BasicType;
import org.hibernate.type.CustomType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * Allows custom types and type descriptors to be contributed to the eventual
 * {@link TypeConfiguration}, either by a {@link org.hibernate.dialect.Dialect}
 * or by a {@link TypeContributor}.
 *
 * @author Steve Ebersole
 * @see TypeContributor
 */
public interface TypeContributions {
	/**
	 * The {@link TypeConfiguration} to contribute to
	 */
	TypeConfiguration getTypeConfiguration();

	/**
	 * Add the given {@link JavaType} to the {@link JavaTypeRegistry}
	 * of the eventual {@link TypeConfiguration}.
	 */
	default void contributeJavaType(JavaType<?> descriptor) {
		getTypeConfiguration().getJavaTypeRegistry().addDescriptor( descriptor );
	}

	/**
	 * Add the given {@link JdbcType} to the {@link JdbcTypeRegistry}
	 * of the eventual {@link TypeConfiguration}.
	 */
	default void contributeJdbcType(JdbcType descriptor) {
		getTypeConfiguration().getJdbcTypeRegistry().addDescriptor( descriptor );
	}

	default void contributeJdbcTypeConstructor(JdbcTypeConstructor typeConstructor) {
		getTypeConfiguration().getJdbcTypeRegistry().addTypeConstructor( typeConstructor );
	}

	/**
	 * Register a {@link UserType} as the implicit (auto-applied)
	 * type for values of type {@link UserType#returnedClass()}.
	 */
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
	default void contributeType(CompositeUserType<?> type) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Register an {@link AttributeConverter} class.
	 *
	 * @since 6.2
	 */
	@Incubating
	default void contributeAttributeConverter(Class<? extends AttributeConverter<?, ?>> converterClass) {
		// default implementation for backward compatibility
		throw new UnsupportedOperationException();
	}

	/**
	 * @deprecated See discussion of {@code TypeContributor} in User Guide.
	 */
	@Deprecated(since = "6.0")
	default void contributeType(BasicType<?> type) {
		getTypeConfiguration().getBasicTypeRegistry().register( type );
		final JavaType<?> javaType = type.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType.getJavaType(), () -> javaType );
	}

	/**
	 * @deprecated Use {@link #contributeType(BasicType)} instead.
	 */
	@Deprecated(since = "5.3")
	default void contributeType(BasicType<?> type, String... keys) {
		getTypeConfiguration().getBasicTypeRegistry().register( type, keys );
		final JavaType<?> javaType = type.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType.getJavaType(), () -> javaType );
	}

	/**
	 * @deprecated Use {@link #contributeType(BasicType)} instead.
	 */
	@Deprecated(since = "5.3")
	default void contributeType(UserType<?> type, String... keys) {
		final CustomType<?> customType = getTypeConfiguration().getBasicTypeRegistry().register( type, keys );
		final JavaType<?> javaType = customType.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType.getJavaType(), () -> javaType );
	}
}
