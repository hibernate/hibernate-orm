/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * @author Steve Ebersole
 */
public interface ServiceRegistryProducer {
	StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder);

	void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder bsrb);
}
