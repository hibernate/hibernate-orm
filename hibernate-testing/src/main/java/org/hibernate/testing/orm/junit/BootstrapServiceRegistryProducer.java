/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.orm.junit;

import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;

/// An alternative to [@BootstrapServiceRegistry][org.hibernate.testing.orm.junit.BootstrapServiceRegistry]
/// for producing a [BootstrapServiceRegistry] when programmatic building is needed.
///
/// @see ServiceRegistryExtension
///
/// @author Steve Ebersole
public interface BootstrapServiceRegistryProducer {
	/// Produce the [BootstrapServiceRegistry]
	BootstrapServiceRegistry produceServiceRegistry(BootstrapServiceRegistryBuilder builder);
}
