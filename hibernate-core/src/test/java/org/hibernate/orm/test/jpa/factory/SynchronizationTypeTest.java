/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.factory;

import jakarta.persistence.SynchronizationType;

import java.util.Collections;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa
public class SynchronizationTypeTest {
	@Test
	public void testPassingSynchronizationType(EntityManagerFactoryScope scope) {
		try {
			scope.getEntityManagerFactory().createEntityManager( SynchronizationType.SYNCHRONIZED );
			fail( "Call should have thrown ISE" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			scope.getEntityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED );
			fail( "Call should have thrown ISE" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			scope.getEntityManagerFactory().createEntityManager( SynchronizationType.SYNCHRONIZED, Collections.emptyMap() );
			fail( "Call should have thrown ISE" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			scope.getEntityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, Collections.emptyMap() );
			fail( "Call should have thrown ISE" );
		}
		catch (IllegalStateException expected) {
		}
	}
}
