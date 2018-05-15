/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.spi;

import java.util.function.Predicate;

import org.hibernate.query.ParameterMetadata;

/**
 * @author Steve Ebersole
 */
public interface ParameterMetadataImplementor<P extends QueryParameterImplementor<?>> extends ParameterMetadata<P> {
	@Override
	boolean containsReference(P parameter);

	@FunctionalInterface
	interface ParameterCollector<P extends QueryParameterImplementor<?>> {
		void collect(P queryParameter);
	}

	void collectAllParameters(ParameterCollector<P> collector);

	boolean hasAnyMatching(Predicate<P> filter);
}
