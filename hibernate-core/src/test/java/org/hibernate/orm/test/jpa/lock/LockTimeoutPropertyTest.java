/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;

import org.hibernate.dialect.H2Dialect;

import org.hibernate.query.SelectionQuery;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.JAKARTA_LOCK_TIMEOUT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Strong Liu
 */
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = UnversionedLock.class,
		integrationSettings = @Setting(name = JAKARTA_LOCK_TIMEOUT, value = "2000")
)
public class LockTimeoutPropertyTest {

	@Test
	public void testLockTimeoutASNamedQueryHint(EntityManagerFactoryScope scope){
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			var query = em.createNamedQuery( "getAll" );
			query.setLockMode( LockModeType.PESSIMISTIC_READ );
			var timeout = query.unwrap(SelectionQuery.class).getLockTimeout();
			assertEquals( 3000, timeout.milliseconds() );
		} );
	}


	@Test
	@JiraKey( value = "HHH-6256")
	public void testTimeoutHint(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			assertTrue( em.getProperties().containsKey( JAKARTA_LOCK_TIMEOUT ) );
			assertEquals( 2000, Integer.parseInt( em.getProperties().get( JAKARTA_LOCK_TIMEOUT ).toString() ) );

			var query1 = em.createQuery( "select u from UnversionedLock u" ).unwrap(SelectionQuery.class);
			assertEquals( 2000, query1.getLockTimeout().milliseconds() );

			var query2 = em.createQuery( "select u from UnversionedLock u" ).unwrap(SelectionQuery.class);
			query2.setLockMode(LockModeType.PESSIMISTIC_WRITE);
			query2.setHint( JAKARTA_LOCK_TIMEOUT, 3000 );
			assertEquals( 3000, query2.getLockTimeout().milliseconds() );
			em.getTransaction().rollback();
		} );
	}

}
