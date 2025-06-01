/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.model;

import org.hibernate.service.JavaServiceLoadable;
import org.hibernate.service.ServiceRegistry;

/**
 * An object that contributes custom types and type descriptors, eventually to
 * a {@link org.hibernate.type.spi.TypeConfiguration}, via an instance of
 * {@link TypeContributions}.
 * <ul>
 * <li>
 *     The most common way to integrate a {@code TypeContributor} is by making
 *     it discoverable via the Java {@link java.util.ServiceLoader} facility.
 * <li>
 *     Alternatively, a {@code TypeContributor} may be programmatically supplied to
 *     {@link org.hibernate.cfg.Configuration#registerTypeContributor(TypeContributor)}
 *     or even {@link org.hibernate.boot.MetadataBuilder#applyTypes(TypeContributor)}.
 * <li>
 *     When bootstrapping Hibernate via JPA or {@link org.hibernate.cfg.Configuration},
 *
 *     Finally, in the JPA boostrap process, {@code TypeContributor}s may be
 *     listed via {@link org.hibernate.jpa.boot.spi.JpaSettings#TYPE_CONTRIBUTORS}.
 * </ul>
 *
 * @author Steve Ebersole
 *
 * @see org.hibernate.type.spi.TypeConfiguration
 */
@JavaServiceLoadable
public interface TypeContributor {
	/**
	 * Contribute types
	 *
	 * @param typeContributions The callback for adding contributed types
	 * @param serviceRegistry The service registry
	 */
	void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry);
}
