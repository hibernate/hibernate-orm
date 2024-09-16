/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;

import org.hibernate.query.sqm.SqmExpressible;

import jakarta.persistence.metamodel.EmbeddableType;

/**
 * Hibernate extension to the JPA {@link EmbeddableType} contract.
 *
 * @apiNote Temporarily extends the deprecated EmbeddableType.  See the {@link EmbeddableType}
 * Javadocs for more information
 *
 * @author Steve Ebersole
 */
public interface EmbeddableDomainType<J>
		extends TreatableDomainType<J>, EmbeddableType<J>, SqmExpressible<J> {
	@Override
	default EmbeddableDomainType<J> getSqmType() {
		return this;
	}

	@Override
	Collection<? extends EmbeddableDomainType<? extends J>> getSubTypes();

	default boolean isPolymorphic() {
		return getSuperType() != null || !getSubTypes().isEmpty();
	}
}
