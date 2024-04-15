/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.Service;

/**
 * @author Steve Ebersole
 */
public interface ServiceRegistryScope {
	/**
	 * Generalized support for running exception-safe code using a ServiceRegistry to
	 * ensure proper shutdown
	 */
	static void using(Supplier<StandardServiceRegistry> ssrProducer, Consumer<ServiceRegistryScope> action) {
		try (final StandardServiceRegistry ssr = ssrProducer.get()) {
			action.accept( () -> ssr );
		}
	}

	StandardServiceRegistry getRegistry();

	default <S extends Service> void withService(Class<S> role, Consumer<S> action) {
		assert role != null;

		final S service = getRegistry().getService( role );

		if ( service == null ) {
			throw new IllegalArgumentException( "Could not locate requested service - " + role.getName() );
		}

		action.accept( service );
	}

	default <R, S extends Service> R fromService(Class<S> role, Function<S,R> action) {
		assert role != null;

		final S service = getRegistry().getService( role );

		if ( service == null ) {
			throw new IllegalArgumentException( "Could not locate requested service - " + role.getName() );
		}

		return action.apply( service );
	}
}
