/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.criteria;

import jakarta.persistence.criteria.Root;

import org.hibernate.metamodel.model.domain.EntityDomainType;

/**
 * @author Steve Ebersole
 */
public interface JpaRoot<T> extends JpaFrom<T,T>, Root<T> {
	@Override
	EntityDomainType<T> getModel();

	@Override
	@SuppressWarnings("unchecked")
	default <S extends T> JpaRoot<S> treat(Class<S> treatJavaType) {
		return (JpaRoot<S>) treatAs( treatJavaType );
	}

	// todo: deprecate and remove?
	EntityDomainType<T> getManagedType();
}
