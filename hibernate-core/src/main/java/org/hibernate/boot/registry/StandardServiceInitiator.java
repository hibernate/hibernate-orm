/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.registry;

import java.util.Map;

import org.hibernate.service.Service;
import org.hibernate.service.spi.ServiceInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contract for an initiator of services that target the standard {@link org.hibernate.service.ServiceRegistry}.
 *
 * @param <R> The type of the service initiated.
 *
 * @author Steve Ebersole
 */
public interface StandardServiceInitiator<R extends Service> extends ServiceInitiator<R> {
	/**
	 * Initiates the managed service.
	 *
	 * @param configurationValues The configuration values in effect
	 * @param registry The service registry.  Can be used to locate services needed to fulfill initiation.
	 *
	 * @return The initiated service.
	 */
	@Nullable R initiateService(Map<String, Object> configurationValues, ServiceRegistryImplementor registry);
}
