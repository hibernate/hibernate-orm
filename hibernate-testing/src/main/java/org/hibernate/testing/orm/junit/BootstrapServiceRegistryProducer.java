/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;

/**
 * Producer of BootstrapServiceRegistry
 */
public interface BootstrapServiceRegistryProducer {
	BootstrapServiceRegistry produceServiceRegistry(BootstrapServiceRegistryBuilder builder);
}
