/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;

import org.hibernate.service.spi.SessionFactoryServiceRegistry;

import java.io.Serializable;

/**
 * Marker interface for services. Services usually belong to a {@link ServiceRegistry}.
 * <ul>
 * <li>Services may be contributed to a {@link SessionFactoryServiceRegistry} using an
 *     {@link org.hibernate.integrator.spi.Integrator}, which is automatically discoved
 *     via the Java {@link java.util.ServiceLoader} facility.
 * <li>Alternatively, a service may be directly contributed to a
 *     {@link org.hibernate.service.spi.SessionFactoryServiceRegistryBuilder} either
 *     by registering an actual instance of the {@code Service}, or by registering a
 *     {@link org.hibernate.service.spi.ServiceInitiator}.
 * <li>Other ways to contribute service implementations include
 *     {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder} and
 *     {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}.
 * </ul>
 * <p>
 * All services must be {@link Serializable}!
 *
 * @author Steve Ebersole
 */
public interface Service extends Serializable {
}
