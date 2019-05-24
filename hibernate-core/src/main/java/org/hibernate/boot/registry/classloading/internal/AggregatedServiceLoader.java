/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.boot.registry.classloading.internal;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * A service loader bound to an {@link AggregatedClassLoader}.
 * @param <S> The type of the service contract.
 */
final class AggregatedServiceLoader<S> {

	private final ServiceLoader<S> delegate;

	AggregatedServiceLoader(AggregatedClassLoader aggregatedClassLoader, Class<S> serviceContract) {
		this.delegate = ServiceLoader.load( serviceContract, aggregatedClassLoader );
	}

	/**
	 * @return All the loaded services.
	 */
	public Collection<S> getAll() {
		final Set<S> services = new LinkedHashSet<>();
		for ( S service : delegate ) {
			services.add( service );
		}
		return services;
	}

	/**
	 * Release all resources.
	 */
	public void close() {
		// Clear service providers
		delegate.reload();
	}

}
