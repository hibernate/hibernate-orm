/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.BindableType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Describes any type that occurs in the application's domain model.
 * <p>
 * The base for Hibernate's extension of the JPA type system.
 * <p>
 * Encapsulates a {@link JavaType} describing the more rudimentary
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
public interface DomainType<J> extends BindableType<J> {

	JavaType<J> getExpressibleJavaType();

	/**
	 * The name of the type.
	 *
	 * @apiNote This is the Hibernate notion of the type name. For most
	 *          types this is just the Java type ({@link Class}) name.
	 *          However, using the string allows for dynamic models.
	 */
	String getTypeName();

	default int getTupleLength() {
		return 1;
	}
}
