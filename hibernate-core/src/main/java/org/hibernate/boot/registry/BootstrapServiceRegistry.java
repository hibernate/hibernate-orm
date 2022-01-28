/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.registry;

import org.hibernate.service.ServiceRegistry;

/**
 * Provides the most basic services such as class loading. Other
 * configuration-time objects such as {@link org.hibernate.boot.MetadataSources},
 * {@link StandardServiceRegistryBuilder}, and {@link org.hibernate.cfg.Configuration}
 * all depend on an instance of {@code BootstrapServiceRegistry}.
 * <p>
 * An instance may be obtained using {@link BootstrapServiceRegistryBuilder#build()}.
 * <p>
 * Specialized from {@link ServiceRegistry} mainly for type safety.
 *
 * @see BootstrapServiceRegistryBuilder
 *
 * @author Steve Ebersole
 */
public interface BootstrapServiceRegistry extends ServiceRegistry {
}
