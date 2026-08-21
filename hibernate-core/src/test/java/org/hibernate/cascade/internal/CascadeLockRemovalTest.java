/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import org.hibernate.MappingException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Compatibility tests for removal of the deprecated cascade-lock vocabulary.
///
/// @author Steve Ebersole
class CascadeLockRemovalTest {
	@Test
	void namedLockStyleIsRejectedAtSessionFactoryBootstrap() {
		final StandardServiceRegistry serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			assertThatThrownBy( () -> new MetadataSources( serviceRegistry )
					.addResource( "org/hibernate/cascade/internal/lock-cascade.hbm.xml" )
					.buildMetadata()
					.buildSessionFactory() )
					.isInstanceOf( MappingException.class )
					.hasMessageContaining( "Unsupported cascade style: lock" );
		}
		finally {
			serviceRegistry.close();
		}
	}

	static class Parent {
		int id;
		Child child;
	}

	static class Child {
		int id;
	}
}
