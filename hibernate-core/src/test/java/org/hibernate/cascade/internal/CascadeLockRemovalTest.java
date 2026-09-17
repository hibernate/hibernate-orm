/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.cascade.internal;

import org.hibernate.MappingException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Compatibility tests for removal of the deprecated cascade-lock vocabulary.
///
/// @author Steve Ebersole
class CascadeLockRemovalTest {
	@Test
	void namedLockStyleIsRejected() {
		assertThatThrownBy( () -> org.hibernate.cascade.spi.CascadeStyles.getCascadeStyle( "lock" ) )
				.isInstanceOf( MappingException.class )
				.hasMessageContaining( "Unsupported cascade style: lock" );
	}

	static class Parent {
		int id;
		Child child;
	}

	static class Child {
		int id;
	}
}
