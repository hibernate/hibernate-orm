/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import java.util.function.Consumer;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.Service;

/**
 * @author Steve Ebersole
 */
public interface ServiceRegistryScope {
	StandardServiceRegistry getRegistry();

	default <S extends Service> void withService(Class<S> role, Consumer<S> action) {
		assert role != null;

		final S service = getRegistry().getService( role );

		if ( service == null ) {
			throw new IllegalArgumentException( "Could not locate requested service - " + role.getName() );
		}

		action.accept( service );
	}
}
