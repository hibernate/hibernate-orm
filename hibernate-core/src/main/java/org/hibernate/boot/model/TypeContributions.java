/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypeTemplate;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.spi.JavaTypeRegistry;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.UserType;

/**
 * Defines the target contributing types, whether via dialects or {@link TypeContributor}
 *
 * @author Steve Ebersole
 */
public interface TypeContributions {
	TypeConfiguration getTypeConfiguration();

	/**
	 * Add the JavaType to the {@link TypeConfiguration}'s
	 * {@link JavaTypeRegistry}
	 */
	void contributeJavaType(JavaType<?> descriptor);

	/**
	 * Add the JdbcType to the {@link TypeConfiguration}'s
	 * {@link JdbcTypeRegistry}
	 */
	void contributeJdbcType(JdbcType descriptor);

	/**
	 * Registers a UserType as the implicit (auto-applied) type
	 * for values of type {@link UserType#returnedClass()}
	 */
	<T> void contributeType(UserType<T> type);

	/**
	 * @deprecated See user-guide section `2.2.46. TypeContributor` for details - `basic_types.adoc`
	 */
	@Deprecated(since = "6.0")
	void contributeType(BasicType<?> type);

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
	void contributeType(BasicType<?> type, String... keys);

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
	void contributeType(UserType<?> type, String... keys);
}
