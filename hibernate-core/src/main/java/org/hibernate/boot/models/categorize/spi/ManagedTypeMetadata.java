/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Collection;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.models.spi.ClassDetails;

import jakarta.persistence.AccessType;

/// Categorized metadata about a {@linkplain jakarta.persistence.metamodel.ManagedType managed type}.
///
/// This contract describes the information shared by entity, mapped-superclass,
/// and embeddable types after annotation and XML sources have been interpreted.
///
/// @since 9.0
/// @author Steve Ebersole
public interface ManagedTypeMetadata {

	enum Kind { ENTITY, MAPPED_SUPER, EMBEDDABLE }

	Kind getManagedTypeKind();

	/// The underlying managed-class
	ClassDetails getClassDetails();

	/// The class-level access type
	AccessType getAccessType();

	/// Get the number of declared attributes
	int getNumberOfAttributes();

	/// Get the declared attributes
	Collection<AttributeMetadata> getAttributes();

	AttributeMetadata findAttribute(String name);

	/// Visit each declared attributes
	void forEachAttribute(IndexedConsumer<AttributeMetadata> consumer);
}
