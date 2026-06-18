/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.internal;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Timeout;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.query.named.spi.NamedSqmQueryMemento;
import org.hibernate.query.spi.JpaTypedQueryReference;
import org.hibernate.query.sqm.tree.spi.select.SqmSelectStatement;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class AugmentedTypedQueryReference<T> implements JpaTypedQueryReference<T> {
	private final TypedQueryReference<?> reference;
	private final Class<? extends T> augmentedResultType;
	private final SqmSelectStatement<T> criteriaQuery;
	private final @Nullable NamedSqmQueryMemento<?> sqmMemento;

	AugmentedTypedQueryReference(
			TypedQueryReference<T> reference,
			SqmSelectStatement<T> criteriaQuery,
			@Nullable NamedSqmQueryMemento<?> sqmMemento) {
		this.reference = reference;
		this.augmentedResultType = reference.getResultType();
		this.criteriaQuery = criteriaQuery;
		this.sqmMemento = sqmMemento;
	}

	AugmentedTypedQueryReference(
			TypedQueryReference<?> reference,
			Class<T> augmentedResultType,
			SqmSelectStatement<T> criteriaQuery,
			@Nullable NamedSqmQueryMemento<?> sqmMemento) {
		this.reference = reference;
		this.augmentedResultType = augmentedResultType;
		this.criteriaQuery = criteriaQuery;
		this.sqmMemento = sqmMemento;
	}

	public SqmSelectStatement<T> getCriteriaQuery() {
		return criteriaQuery;
	}

	public @Nullable NamedSqmQueryMemento<?> getSqmMemento() {
		return sqmMemento;
	}

	@Override
	public String getName() {
		return reference.getName();
	}

	@Override
	@Nonnull
	public Class<? extends T> getResultType() {
		return augmentedResultType;
	}

	@Override
	@Nonnull
	public Map<String, Object> getHints() {
		return reference.getHints();
	}

	@Override
	public String getEntityGraphName() {
		return reference.getEntityGraphName();
	}

	@Override
	public List<Class<?>> getParameterTypes() {
		return reference.getParameterTypes();
	}

	@Override
	public List<String> getParameterNames() {
		return reference.getParameterNames();
	}

	@Override
	public List<Object> getArguments() {
		return reference.getArguments();
	}

	@Override
	@Nonnull
	public Set<TypedQuery.Option> getOptions() {
		return reference.getOptions();
	}

	@Override
	public Timeout getTimeout() {
		return reference instanceof JpaTypedQueryReference<?> jpaReference
				? jpaReference.getTimeout()
				: null;
	}
}
