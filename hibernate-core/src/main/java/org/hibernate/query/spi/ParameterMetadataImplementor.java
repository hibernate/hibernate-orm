/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.function.Consumer;
import java.util.function.Predicate;
import jakarta.persistence.Parameter;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.ParameterMetadata;
import org.hibernate.query.QueryParameter;

/**
 * @author Steve Ebersole
 */
public interface ParameterMetadataImplementor extends ParameterMetadata {
	void visitParameters(Consumer<QueryParameter<?>> consumer);

	default void collectAllParameters(Consumer<QueryParameter<?>> collector) {
		visitParameters( collector );
	}

	@Override
	default void visitRegistrations(Consumer<QueryParameter<?>> action) {
		visitParameters( action );
	}

	boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter);

	@Override
	QueryParameterImplementor<?> findQueryParameter(String name);

	@Override
	QueryParameterImplementor<?> getQueryParameter(String name);

	@Override
	QueryParameterImplementor<?> findQueryParameter(int positionLabel);

	@Override
	QueryParameterImplementor<?> getQueryParameter(int positionLabel);

	@Override
	<P> QueryParameterImplementor<P> resolve(Parameter<P> param);

	QueryParameterBindings createBindings(SessionFactoryImplementor sessionFactory);
}
