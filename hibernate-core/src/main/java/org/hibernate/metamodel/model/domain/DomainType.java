/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.sqm.SqmExpressible;
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
public interface DomainType<J> extends SqmExpressible<J> {
	@Override
	default DomainType<J> getSqmType() {
		return this;
	}

	default int getTupleLength() {
		return 1;
	}
}
