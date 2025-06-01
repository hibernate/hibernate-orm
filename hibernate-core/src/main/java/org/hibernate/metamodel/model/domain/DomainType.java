/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Describes any type forming part of the application domain model.
 * <p>
 * This is the base type for Hibernate's extension of the standard metamodel
 * of the {@linkplain jakarta.persistence.metamodel.Type JPA type system}.
 * <p>
 * Encapsulates a {@link JavaType} describing the more rudimentary aspects
 * of the Java type. The {@code DomainType} is a higher-level construct
 * incorporating information such as bean properties, constructors, etc.
 *
 * @implNote The actual JPA type system is more akin to {@link SimpleDomainType}.
 *           This contract represents a "higher level" abstraction, allowing
 *           descriptors for collections (which JPA does not define) as well
 *           as Hibernate-specific features (like dynamic models and
 *           {@link org.hibernate.annotations.Any @Any}).
 *
 * @author Steve Ebersole
 */
public interface DomainType<J> {

	/**
	 * The {@link JavaType} representing this domain type.
	 */
	JavaType<J> getExpressibleJavaType();

	/**
	 * The Java class which represents by this domain type.
	 *
	 * @see jakarta.persistence.metamodel.Type#getJavaType
	 */
	Class<J> getJavaType();

	/**
	 * The name of the type. Usually, but not always, the name of a Java class.
	 *
	 * @see ManagedDomainType#getTypeName()
	 * @see org.hibernate.query.sqm.SqmExpressible#getTypeName()
	 */
	default String getTypeName() {
		return getExpressibleJavaType().getTypeName();
	}
}
