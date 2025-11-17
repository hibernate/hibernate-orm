/*
 * SPDX-License-Identifier: Apache-2.0
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
	default StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.build();
	}

	default void prepareBootstrapRegistryBuilder(BootstrapServiceRegistryBuilder bsrb) {
		// generally this is not the one we are interested in
	}
}
