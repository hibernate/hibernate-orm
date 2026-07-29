/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service;

import java.io.Serializable;

/**
 * Marker interface for services. Services usually belong to a {@link ServiceRegistry}.
 * <ul>
 * <li>Service implementations may be contributed using
 *     {@link org.hibernate.boot.registry.BootstrapServiceRegistryBuilder} and
 *     {@link org.hibernate.boot.registry.StandardServiceRegistryBuilder}.
 * <li>Factory-owned extension points use dedicated Java-loadable contracts.
 * </ul>
 * <p>
 * All services must be {@link Serializable}!
 *
 * @author Steve Ebersole
 */
public interface Service extends Serializable {
}
