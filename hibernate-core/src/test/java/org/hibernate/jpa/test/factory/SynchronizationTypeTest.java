/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.factory;

import javax.persistence.SynchronizationType;

import java.util.Collections;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class SynchronizationTypeTest extends BaseEntityManagerFunctionalTestCase {
	@Test
	public void testPassingSynchronizationType() {
		try {
			entityManagerFactory().createEntityManager( SynchronizationType.SYNCHRONIZED );
			fail( "Call should have throw ISE" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			entityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED );
			fail( "Call should have throw ISE" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			entityManagerFactory().createEntityManager( SynchronizationType.SYNCHRONIZED, Collections.emptyMap() );
			fail( "Call should have throw ISE" );
		}
		catch (IllegalStateException expected) {
		}

		try {
			entityManagerFactory().createEntityManager( SynchronizationType.UNSYNCHRONIZED, Collections.emptyMap() );
			fail( "Call should have throw ISE" );
		}
		catch (IllegalStateException expected) {
		}
	}
}
