/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.Set;
import java.util.function.Predicate;
import javax.persistence.Parameter;

import org.hibernate.query.ParameterMetadata;

/**
 * @author Steve Ebersole
 */
public interface ParameterMetadataImplementor extends ParameterMetadata {
	@Override
	<T> QueryParameterImplementor<T> getQueryParameter(String name);

	@Override
	<T> QueryParameterImplementor<T> getQueryParameter(int positionLabel);

	@Override
	<T> QueryParameterImplementor<T> resolve(Parameter<T> param);

	@Override
	Set<? extends QueryParameterImplementor<?>> getRegistrations();

	@FunctionalInterface
	interface ParameterCollector {
		void collect(QueryParameterImplementor<?> queryParameter);
	}

	void collectAllParameters(ParameterCollector collector);

	boolean hasAnyMatching(Predicate<QueryParameterImplementor<?>> filter);
}
