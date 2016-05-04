/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.lock;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.hibernate.dialect.H2Dialect;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * no need to run this on DB matrix
 *
 * @author Strong Liu <stliu@hibernate.org>
 */
@RequiresDialect(H2Dialect.class)
public class LockTimeoutPropertyTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected void addConfigOptions(Map options) {
		options.put( AvailableSettings.LOCK_TIMEOUT, "2000" );
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
	@TestForIssue( jiraKey = "HHH-6256")
	public void testTimeoutHint(){
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		boolean b= em.getProperties().containsKey( AvailableSettings.LOCK_TIMEOUT );
		assertTrue( b );
		int timeout = Integer.valueOf( em.getProperties().get( AvailableSettings.LOCK_TIMEOUT ).toString() );
		assertEquals( 2000, timeout);
		org.hibernate.query.Query q = (org.hibernate.query.Query) em.createQuery( "select u from UnversionedLock u" );
		timeout = q.getLockOptions().getTimeOut();
		assertEquals( 2000, timeout );

		Query query = em.createQuery( "select u from UnversionedLock u" );
		query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		query.setHint( AvailableSettings.LOCK_TIMEOUT, 3000 );
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
