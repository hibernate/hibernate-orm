/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry;

import org.hibernate.service.ServiceRegistry;

/**
 * Specialization of the {@link ServiceRegistry} contract mainly for type safety.
 * <p>
 * An instance may be obtained using {@link StandardServiceRegistryBuilder#build()}.
 *
 * @author Steve Ebersole
 */
public interface StandardServiceRegistry extends ServiceRegistry {
}
