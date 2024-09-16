/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

/**
 * Specialized {@link org.hibernate.service.ServiceRegistry} implementation that
 * holds services which need access to the {@link org.hibernate.SessionFactory}
 * during initialization.
 *
 * @author Steve Ebersole
 */
public interface SessionFactoryServiceRegistry extends ServiceRegistryImplementor {
}
