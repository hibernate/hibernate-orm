/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Collection;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.models.spi.ClassDetails;


/**
 * Metadata about a {@linkplain jakarta.persistence.metamodel.ManagedType managed type}
 *
 * @author Steve Ebersole
 */
public interface ManagedTypeMetadata {

	enum Kind { ENTITY, MAPPED_SUPER, EMBEDDABLE }

	Kind getManagedTypeKind();

	/**
	 * The underlying managed-class
	 */
	ClassDetails getClassDetails();

	/**
	 * The class-level access type
	 */
	ClassAttributeAccessType getClassLevelAccessType();

	/**
	 * Get the number of declared attributes
	 */
	int getNumberOfAttributes();

	/**
	 * Get the declared attributes
	 */
	Collection<AttributeMetadata> getAttributes();

	AttributeMetadata findAttribute(String name);

	/**
	 * Visit each declared attributes
	 */
	void forEachAttribute(IndexedConsumer<AttributeMetadata> consumer);
}
