/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.annotation.Nonnull;
import jakarta.persistence.criteria.Root;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
public interface JpaRoot<T> extends JpaFrom<T,T>, Root<T> {
	/**
	 * Return the entity model of this root.
	 */
	@Nonnull
	@Override
	EntityDomainType<T> getModel();

	/**
	 * Downcast this root to the specified subtype.
	 */
	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	default <S extends T> JpaRoot<S> treat(@Nonnull Class<S> treatJavaType) {
		return (JpaRoot<S>) treatAs( treatJavaType );
	}

	// todo: deprecate and remove?
	/**
	 * Return the managed type of this root.
	 */
	EntityDomainType<T> getManagedType();
}
