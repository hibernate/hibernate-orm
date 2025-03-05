/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * no need to run this on DB matrix
 *
 * @author Strong Liu
 */
@RequiresDialect(H2Dialect.class)
public class LockTimeoutPropertyTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.JPA_LOCK_TIMEOUT, "2000" );
	}

	@Test
	public void testLockTimeoutASNamedQueryHint(){
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createNamedQuery( "getAll" );
		query.setLockMode( LockModeType.PESSIMISTIC_READ );

		int timeout = query.unwrap( org.hibernate.query.Query.class ).getLockOptions().getTimeOut();
		assertEquals( 3000, timeout );
	}


	@Test
	@JiraKey( value = "HHH-6256")
	public void testTimeoutHint(){
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		boolean b= em.getProperties().containsKey( AvailableSettings.JPA_LOCK_TIMEOUT );
		assertTrue( b );
		int timeout = Integer.valueOf( em.getProperties().get( AvailableSettings.JPA_LOCK_TIMEOUT ).toString() );
		assertEquals( 2000, timeout);
		org.hibernate.query.Query q = (org.hibernate.query.Query) em.createQuery( "select u from UnversionedLock u" );
		timeout = q.getLockOptions().getTimeOut();
		assertEquals( 2000, timeout );

		Query query = em.createQuery( "select u from UnversionedLock u" );
		query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		query.setHint( AvailableSettings.JPA_LOCK_TIMEOUT, 3000 );
		q = (org.hibernate.query.Query) query;
		timeout = q.getLockOptions().getTimeOut();
		assertEquals( 3000, timeout );
		em.getTransaction().rollback();
		em.close();
	}


	@Override
	public Class[] getAnnotatedClasses() {
		return new Class[] {
				UnversionedLock.class
		};
	}
}
