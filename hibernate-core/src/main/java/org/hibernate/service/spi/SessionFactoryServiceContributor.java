/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import jakarta.annotation.Nonnull;

import org.hibernate.Remove;
import org.hibernate.service.JavaServiceLoadable;

/**
 * @author Steve Ebersole
 *
 * @apiNote This contract will be removed in 9.0.
 */
@Remove
@JavaServiceLoadable
public interface SessionFactoryServiceContributor {
	/**
	 * Contribute services to the indicated registry builder.
	 *
	 * @param serviceRegistryBuilder The builder to which services (or initiators) should be contributed.
	 */
	void contribute(@Nonnull SessionFactoryServiceRegistryBuilder serviceRegistryBuilder);
}
