/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.jta;

import java.util.Map;

import javax.transaction.RollbackException;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.StrTestEntity;
import org.hibernate.envers.test.support.domains.revisionentity.ExceptionListenerRevEntity;

import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Same as {@link org.hibernate.envers.test.revisionentity.ExceptionListenerTest}, but in a JTA environment.
 *
 * @author Adam Warski (adam at warski dot org)
 */
public class JtaExceptionListenerTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { StrTestEntity.class, ExceptionListenerRevEntity.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		super.addSettings( settings );

		TestingJtaBootstrap.prepare( settings );
	}

	@DynamicBeforeAll(expected = RollbackException.class)
	public void testTransactionRollback() throws Exception {
		inJtaTransaction(
				entityManager -> {
					// Trying to persist an entity - however the listener should throw an exception, so the entity
					// shouldn't be persisted
					StrTestEntity te = new StrTestEntity( "x" );
					entityManager.persist( te );
				}
		);
	}

	@DynamicTest
	public void testDataNotPersisted() throws Exception {
		final String hql = "from StrTestEntity s where s.str = \"x\"";
		inJtaTransaction(
				entityManager -> {
					assertThat(
							entityManager.createQuery( hql ).getResultList().size(),
							equalTo( 0 )
					);
				}
		);
	}
}
