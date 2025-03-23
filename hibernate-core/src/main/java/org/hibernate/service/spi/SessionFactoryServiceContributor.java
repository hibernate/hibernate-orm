/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.service.spi;

import org.hibernate.service.JavaServiceLoadable;

/**
 * @author Steve Ebersole
 */
@JavaServiceLoadable
public interface SessionFactoryServiceContributor {
	/**
	 * Contribute services to the indicated registry builder.
	 *
	 * @param serviceRegistryBuilder The builder to which services (or initiators) should be contributed.
	 */
	void contribute(SessionFactoryServiceRegistryBuilder serviceRegistryBuilder);
}
