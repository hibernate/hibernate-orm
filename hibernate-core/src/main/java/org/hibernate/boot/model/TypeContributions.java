/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model;

import org.hibernate.type.BasicType;
import org.hibernate.type.StandardBasicTypeTemplate;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptorRegistry;
import org.hibernate.type.descriptor.sql.SqlTypeDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.CompositeUserType;
import org.hibernate.usertype.UserType;

/**
 * Defines the target contributing types, whether via dialects or {@link TypeContributor}
 *
 * @author Steve Ebersole
 */
public interface TypeContributions {
	TypeConfiguration getTypeConfiguration();

	/**
	 * Add the JavaTypeDescriptor to the {@link TypeConfiguration}'s
	 * {@link JavaTypeDescriptorRegistry}
	 */
	void contributeJavaTypeDescriptor(JavaTypeDescriptor descriptor);

	/**
	 * Add the JavaTypeDescriptor to the {@link TypeConfiguration}'s
	 * {@link JavaTypeDescriptorRegistry}
	 */
	void contributeSqlTypeDescriptor(SqlTypeDescriptor descriptor);

	void contributeType(BasicType type);

	/**
	 * @deprecated (since 5.3) Use {@link #contributeType(BasicType)} instead.  Basic
	 * types will be defined and handled much differently in 6.0 based on a combination
	 * of {@link JavaTypeDescriptor}, {@link SqlTypeDescriptor} and a concept of a "value
	 * converter" (a JPA AttributeConverter, an enum value resolver, etc).  To get as
	 * close as possible in 5.3 use existing {@link JavaTypeDescriptor} and
	 * {@link SqlTypeDescriptor} implementations (or write your own for custom types)
	 * and use {@link StandardBasicTypeTemplate} to combine those with
	 * registration keys and call {@link #contributeType(BasicType)} instead
	 */
	@Deprecated
	void contributeType(BasicType type, String... keys);

	/**
	 * @deprecated (since 5.3) Use {@link #contributeType(BasicType)} instead.
	 * {@link UserType}, as currently defined, will be done very differently in 6.0.
	 * In most cases a {@link UserType} can be simply replaced with proper
	 * {@link JavaTypeDescriptor}.  To get as close as possible to 6.0 in 5.3 use
	 * existing {@link JavaTypeDescriptor} and {@link SqlTypeDescriptor}
	 * implementations (or write your own for custom impls) and use
	 * {@link StandardBasicTypeTemplate} to combine those with registration keys
	 * and call {@link #contributeType(BasicType)} instead
	 */
	@Deprecated
	void contributeType(UserType type, String... keys);

	/**
	 * @deprecated (since 5.3) Use {@link #contributeType(BasicType)} instead.
	 * {@link CompositeUserType}, as currently defined, will be done very differently
	 * in 6.0.  {@link CompositeUserType} should be replaced with a normal Hibernate
	 * component or JPA embeddable (different names, same thing.  This embeddable
	 * may contain, in turn, custom types that should be handled as described on these
	 * methods
	 */
	@Deprecated
	void contributeType(CompositeUserType type, String... keys);
}
