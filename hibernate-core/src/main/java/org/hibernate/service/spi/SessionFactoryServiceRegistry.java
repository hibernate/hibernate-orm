/*
 * SPDX-License-Identifier: Apache-2.0
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
