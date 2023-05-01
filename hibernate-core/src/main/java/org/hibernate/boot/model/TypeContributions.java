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
import org.hibernate.type.StandardBasicTypeTemplate;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcTypeConstructor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
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
		contributeType( type, type.returnedClass().getName() );
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
	 * @deprecated Use {@link #contributeType(BasicType)} instead.  Basic
	 * types will be defined and handled much differently in 6.0 based on a combination
	 * of {@link JavaType}, {@link JdbcType} and a concept of a "value
	 * converter" (a JPA AttributeConverter, an enum value resolver, etc).  To get as
	 * close as possible in 5.3 use existing {@link JavaType} and
	 * {@link JdbcType} implementations (or write your own for custom types)
	 * and use {@link StandardBasicTypeTemplate} to combine those with
	 * registration keys and call {@link #contributeType(BasicType)} instead
	 */
	@Deprecated(since = "5.3")
	default void contributeType(BasicType<?> type, String... keys) {
		getTypeConfiguration().getBasicTypeRegistry().register( type, keys );
		final JavaType<?> javaType = type.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType.getJavaType(), () -> javaType );
	}

	/**
	 * @deprecated Use {@link #contributeType(BasicType)} instead.
	 * {@link UserType}, as currently defined, will be done very differently in 6.0.
	 * In most cases a {@link UserType} can be simply replaced with proper
	 * {@link JavaType}.  To get as close as possible to 6.0 in 5.3 use
	 * existing {@link JavaType} and {@link JdbcType}
	 * implementations (or write your own for custom impls) and use
	 * {@link StandardBasicTypeTemplate} to combine those with registration keys
	 * and call {@link #contributeType(BasicType)} instead
	 */
	@Deprecated(since = "5.3")
	default void contributeType(UserType<?> type, String... keys) {
		final CustomType<?> customType = getTypeConfiguration().getBasicTypeRegistry().register( type, keys );
		final JavaType<?> javaType = customType.getJavaTypeDescriptor();
		getTypeConfiguration().getJavaTypeRegistry().resolveDescriptor( javaType.getJavaType(), () -> javaType );
	}
}
