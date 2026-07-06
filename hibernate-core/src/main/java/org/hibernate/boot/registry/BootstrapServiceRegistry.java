package org.hibernate.boot.registry;

import org.hibernate.service.ServiceRegistry;

/**
 * Provides the most basic services such as class loading.
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
