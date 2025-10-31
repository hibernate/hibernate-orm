/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.engine.query.spi;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EmbeddableDomainType;
import org.hibernate.metamodel.model.domain.internal.EntityTypeImpl;
import org.hibernate.query.QueryParameter;
import org.hibernate.type.BindableType;

/**
 * @apiNote Consider this contract (and its subcontracts) as incubating as we transition to 6.0 and SQM.
 *
 * @author Steve Ebersole
 */
@Incubating
public abstract class AbstractParameterDescriptor<T> implements QueryParameter<T> {
	private final int[] sourceLocations;

	private @Nullable BindableType<T> expectedType;

	public AbstractParameterDescriptor(int[] sourceLocations, @Nullable BindableType<T> expectedType) {
		this.sourceLocations = sourceLocations;
		this.expectedType = expectedType;
	}

	@Override
	public @Nullable String getName() {
		return null;
	}

	@Override
	public @Nullable Integer getPosition() {
		return null;
	}

	@Override
	public @Nullable Class<T> getParameterType() {
		return expectedType == null ? null : expectedType.getJavaType();
	}

	@Override
	public @Nullable BindableType<T> getHibernateType() {
		return getExpectedType();
	}

	public @Nullable BindableType<T> getExpectedType() {
		return expectedType;
	}

	public void resetExpectedType(@Nullable BindableType<T> expectedType) {
		this.expectedType = expectedType;
	}

	@Override
	public boolean allowsMultiValuedBinding() {
		if ( expectedType instanceof EntityTypeImpl ) {
			return ( (EntityTypeImpl<T>) expectedType ).getIdType() instanceof EmbeddableDomainType;
		}
		return expectedType instanceof EmbeddableDomainType;
	}
}
