/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.boot;

import org.hibernate.boot.internal.BootstrapRegistryLifecycle;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.service.ServiceRegistry;

import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/// Ensures cleanup failures preserve the original error and do not prevent parent cleanup.
///
/// @author Steve Ebersole
@BaseUnitTest
class BootstrapRegistryLifecycleTest {
	@Test
	void preservesBootstrapFailureAndAttemptsBothRegistries() {
		final var parent = mock( ServiceRegistry.class );
		final var standard = mock( StandardServiceRegistry.class );
		final var childFailure = new AssertionError( "child cleanup" );
		final var parentFailure = new IllegalStateException( "parent cleanup" );
		doThrow( childFailure ).when( standard ).close();
		doThrow( parentFailure ).when( parent ).close();
		final var lifecycle = new BootstrapRegistryLifecycle( parent );
		lifecycle.register( standard );
		final var original = new IllegalArgumentException( "bootstrap" );
		lifecycle.close( original );
		lifecycle.close();
		assertThat( original.getSuppressed() ).containsExactly( childFailure, parentFailure );
		final var order = inOrder( standard, parent );
		order.verify( standard ).close();
		order.verify( parent ).close();
		verifyNoMoreInteractions( standard, parent );
	}

	@Test
	void cancellationReportsFirstCleanupFailureAfterAttemptingParent() {
		final var parent = mock( ServiceRegistry.class );
		final var standard = mock( StandardServiceRegistry.class );
		final var childFailure = new IllegalStateException( "child cleanup" );
		final var parentFailure = new AssertionError( "parent cleanup" );
		doThrow( childFailure ).when( standard ).close();
		doThrow( parentFailure ).when( parent ).close();
		final var lifecycle = new BootstrapRegistryLifecycle( parent );
		lifecycle.register( standard );
		assertThatThrownBy( lifecycle::close ).isSameAs( childFailure );
		assertThat( childFailure.getSuppressed() ).containsExactly( parentFailure );
		lifecycle.close();
		final var order = inOrder( standard, parent );
		order.verify( standard ).close();
		order.verify( parent ).close();
		verifyNoMoreInteractions( standard, parent );
	}
}
