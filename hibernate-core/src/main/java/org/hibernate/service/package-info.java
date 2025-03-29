/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

/**
 * Defines a framework for pluggable {@linkplain org.hibernate.service.Service services},
 * allowing for customization of key components of Hibernate, and abstraction of these
 * components as SPI interfaces.
 * <p>
 * Services usually belong to a {@link org.hibernate.service.ServiceRegistry}, since
 * that's where Hibernate goes to find them.
 * <p>
 * Libraries may even contribute {@linkplain org.hibernate.integrator.spi.Integrator
 * discoverable} service implementations via the Java {@link java.util.ServiceLoader}
 * facility.
 *
 * @see org.hibernate.service.Service
 * @see org.hibernate.service.ServiceRegistry
 * @see org.hibernate.service.spi.ServiceInitiator
 * @see org.hibernate.integrator.spi.Integrator
 * @see org.hibernate.service.spi.SessionFactoryServiceRegistryBuilder
 */
package org.hibernate.service;
