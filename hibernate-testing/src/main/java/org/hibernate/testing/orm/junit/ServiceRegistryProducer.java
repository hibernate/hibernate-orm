/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/// Alternative to [@ServiceRegistry][ServiceRegistry] for defining the
/// [StandardServiceRegistry] to use for testing when programmatic building is needed.
/// Generally used in conjunction with [ServiceRegistryFunctionalTesting].
///
/// @see BootstrapServiceRegistryProducer
///
/// @author Steve Ebersole
public interface ServiceRegistryProducer {
	default StandardServiceRegistry produceServiceRegistry(StandardServiceRegistryBuilder builder) {
		return builder.build();
	}
}
