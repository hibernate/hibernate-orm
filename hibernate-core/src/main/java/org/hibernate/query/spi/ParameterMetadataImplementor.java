/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.function.Consumer;
import java.util.function.Predicate;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Parameter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;

/**
 * @author Steve Ebersole
 */
public interface ParameterMetadataImplementor extends ParameterMetadata {
	void visitParameters(@Nonnull Consumer<QueryParameter<?>> consumer);

	default void collectAllParameters(@Nonnull Consumer<QueryParameter<?>> collector) {
		visitParameters( collector );
	}

	@Override
	default void visitRegistrations(@Nonnull Consumer<QueryParameter<?>> action) {
		visitParameters( action );
	}

	boolean hasAnyMatching(@Nonnull Predicate<QueryParameterImplementor<?>> filter);

	@Override
	@Nullable
	QueryParameterImplementor<?> findQueryParameter(@Nonnull String name);

	@Override
	@Nonnull
	QueryParameterImplementor<?> getQueryParameter(@Nonnull String name);

	@Override
	@Nullable
	QueryParameterImplementor<?> findQueryParameter(int positionLabel);

	@Override
	@Nonnull
	QueryParameterImplementor<?> getQueryParameter(int positionLabel);

	@Override
	@Nonnull
	<P> QueryParameterImplementor<P> resolve(@Nonnull Parameter<P> param);

	@Nonnull
	QueryParameterBindings createBindings(@Nonnull SessionFactoryImplementor sessionFactory);
}
