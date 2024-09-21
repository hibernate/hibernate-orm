/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * @author Steve Ebersole
 */
public interface ServiceRegistryProducer {
	StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder);

	void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder bsrb);
}
