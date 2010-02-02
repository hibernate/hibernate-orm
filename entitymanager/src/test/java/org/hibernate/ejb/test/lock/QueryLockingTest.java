/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.ejb.test.lock;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.QueryImpl;
import org.hibernate.ejb.test.TestCase;

/**
 * TODO : javadoc
 *
 * @author Steve Ebersole
 */
public class QueryLockingTest extends TestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Lock.class };
	}

	public void testOverallLockMode() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		QueryImpl jpaQuery = em.createQuery( "from Lock_ l" ).unwrap( QueryImpl.class );

		org.hibernate.impl.QueryImpl hqlQuery = (org.hibernate.impl.QueryImpl) jpaQuery.getHibernateQuery();
		assertEquals( LockMode.NONE, hqlQuery.getLockOptions().getLockMode() );
		assertNull( hqlQuery.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.NONE, hqlQuery.getLockOptions().getEffectiveLockMode( "l" ) );

		// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
		jpaQuery.setLockMode( LockModeType.READ );
		assertEquals( LockMode.OPTIMISTIC, hqlQuery.getLockOptions().getLockMode() );
		assertNull( hqlQuery.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.OPTIMISTIC, hqlQuery.getLockOptions().getEffectiveLockMode( "l" ) );

		jpaQuery.setHint( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE+".l", LockModeType.PESSIMISTIC_WRITE );
		assertEquals( LockMode.OPTIMISTIC, hqlQuery.getLockOptions().getLockMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hqlQuery.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hqlQuery.getLockOptions().getEffectiveLockMode( "l" ) );

		em.getTransaction().commit();
		em.close();
	}

	public void testForcedIncrementOverall() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lock lock = new Lock( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lock reread = em.createQuery( "from Lock_", Lock.class ).setLockMode( LockModeType.PESSIMISTIC_FORCE_INCREMENT ).getSingleResult();
		em.getTransaction().commit();
		em.close();
		assertFalse( reread.getVersion().equals( initial ) );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lock.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}
}
