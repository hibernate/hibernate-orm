/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import jakarta.persistence.Timeout;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * no need to run this on DB matrix
 *
 * @author Strong Liu
 */
@RequiresDialect(H2Dialect.class)
@Jpa(
		annotatedClasses = {UnversionedLock.class},
		integrationSettings = {@Setting(name = AvailableSettings.JPA_LOCK_TIMEOUT, value = "2000")}
)
public class LockTimeoutPropertyTest {

	@Test
	public void testLockTimeoutASNamedQueryHint(EntityManagerFactoryScope scope){
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			Query query = em.createNamedQuery( "getAll" );
			query.setLockMode( LockModeType.PESSIMISTIC_READ );

			Timeout timeout = query.unwrap( org.hibernate.query.Query.class ).getLockOptions().getTimeout();
			assertEquals( 3000, timeout.milliseconds() );
		} );
	}


	@Test
	@JiraKey( value = "HHH-6256")
	public void testTimeoutHint(EntityManagerFactoryScope scope) {
		scope.inEntityManager( em -> {
			em.getTransaction().begin();
			boolean b = em.getProperties().containsKey( AvailableSettings.JPA_LOCK_TIMEOUT );
			assertTrue( b );
			int timeout = Integer.parseInt( em.getProperties().get( AvailableSettings.JPA_LOCK_TIMEOUT ).toString() );
			assertEquals( 2000, timeout);
			org.hibernate.query.Query q = (org.hibernate.query.Query) em.createQuery( "select u from UnversionedLock u" );
			timeout = q.getLockOptions().getTimeout().milliseconds();
			assertEquals( 2000, timeout );

			Query query = em.createQuery( "select u from UnversionedLock u" );
			query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
			query.setHint( AvailableSettings.JPA_LOCK_TIMEOUT, 3000 );
			q = (org.hibernate.query.Query) query;
			timeout = q.getLockOptions().getTimeout().milliseconds();
			assertEquals( 3000, timeout );
			em.getTransaction().rollback();
		} );
	}

}
