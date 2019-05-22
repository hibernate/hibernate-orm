/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * Describes any type that occurs in the application's domain model.
 *
 * The base for Hibernate's extension of the JPA type system.
 *
 * Encapsulates a {@link JavaTypeDescriptor} describing the more rudimentary
 * aspects of the Java type.  The DomainType is a higher-level construct
 * incorporating information such as bean properties, constructors, etc
 *
 * @implNote The actual JPA type system is more akin to {@link SimpleDomainType}.
 * This contract represents a "higher level" than JPA
 * including descriptors for collections (which JPA does not define) as well as
 * Hibernate-specific features (like dynamic models or ANY mappings).
 *
 * @author Steve Ebersole
 */
public interface DomainType<J> extends SqmExpressable<J> {
	/**
	 * The name of the type.
	 *
	 * @apiNote This is the Hibernate notion of the type name.  For most types
	 * this will simply be the Java type (i.e. {@link Class}) name.  However
	 * using the String allows for Hibernate's dynamic model feature.
	 */
	default String getTypeName() {
		// default impl to handle the general case returning the Java type name
		return getExpressableJavaTypeDescriptor().getJavaType().getName();
	}

	/**
	 * The descriptor for the Java type (i.e. {@link Class}) represented by this
	 * DomainType.
	 *
	 * @see #getTypeName
	 */
	JavaTypeDescriptor<J> getExpressableJavaTypeDescriptor();
}
