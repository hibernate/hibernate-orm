/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.revisionentity;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.ExceptionListenerRevEntity;
import org.junit.Assert;

import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class ExceptionListenerTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, ExceptionListenerRevEntity.class };
	}

	@DynamicTest
	public void testTransactionRollbackWithNoDataPersisted() {
		// Trying to persist an entity - however the listener should throw an exception, so the entity
		// shouldn't be persisted
		try {
			inTransaction(
					entityManager -> {
						final StrTestEntity entity = new StrTestEntity( "x" );
						entityManager.persist( entity );
					}
			);

			Assert.fail( "This should have failed with a RuntimeException" );
		}
		catch ( RuntimeException e ) {
			// This is to be expected.
		}

		inTransaction(
				entityManager -> {
					final String query = "select count(s) from StrTestEntity s WHERE s.str = \"x\"";
					assertThat(
							entityManager.createQuery( query, Long.class ).getSingleResult(),
							equalTo( 0L )
					);
				}
		);
	}
}