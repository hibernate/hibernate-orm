/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import java.util.Collection;

import jakarta.annotation.Nonnull;
import jakarta.persistence.metamodel.EntityType;

/**
 * Extension to the JPA {@link EntityType} contract.
 *
 * @author Steve Ebersole
 */
public interface EntityDomainType<J> extends IdentifiableDomainType<J>, EntityType<J>, TreatableDomainType<J> {
	@Nonnull
	String getHibernateEntityName();

	@Override
	@Nonnull
	Collection<? extends EntityDomainType<? extends J>> getSubTypes();
}
