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

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;

import org.junit.Test;

import org.hibernate.LockMode;
import org.hibernate.ejb.AvailableSettings;
import org.hibernate.ejb.QueryImpl;
import org.hibernate.ejb.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.internal.SessionImpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class QueryLockingTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Lockable.class };
	}

	@Override
	@SuppressWarnings({ "unchecked" })
	protected void addConfigOptions(Map options) {
		options.put( org.hibernate.cfg.AvailableSettings.USE_NEW_ID_GENERATOR_MAPPINGS, "true" );
	}

	@Test
	public void testOverallLockMode() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		QueryImpl jpaQuery = em.createQuery( "from Lockable l" ).unwrap( QueryImpl.class );

		org.hibernate.internal.QueryImpl hqlQuery = (org.hibernate.internal.QueryImpl) jpaQuery.getHibernateQuery();
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

	@Test
	public void testNativeSql() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		QueryImpl query = em.createNativeQuery( "select * from lockable l" ).unwrap( QueryImpl.class );

		org.hibernate.internal.SQLQueryImpl hibernateQuery = (org.hibernate.internal.SQLQueryImpl) query.getHibernateQuery();
//		assertEquals( LockMode.NONE, hibernateQuery.getLockOptions().getLockMode() );
//		assertNull( hibernateQuery.getLockOptions().getAliasSpecificLockMode( "l" ) );
//		assertEquals( LockMode.NONE, hibernateQuery.getLockOptions().getEffectiveLockMode( "l" ) );

		// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
		query.setLockMode( LockModeType.READ );
		assertEquals( LockMode.OPTIMISTIC, hibernateQuery.getLockOptions().getLockMode() );
		assertNull( hibernateQuery.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.OPTIMISTIC, hibernateQuery.getLockOptions().getEffectiveLockMode( "l" ) );

		query.setHint( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE+".l", LockModeType.PESSIMISTIC_WRITE );
		assertEquals( LockMode.OPTIMISTIC, hibernateQuery.getLockOptions().getLockMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.PESSIMISTIC_WRITE, hibernateQuery.getLockOptions().getEffectiveLockMode( "l" ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testPessimisticForcedIncrementOverall() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable lock = new Lockable( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable reread = em.createQuery( "from Lockable", Lockable.class ).setLockMode( LockModeType.PESSIMISTIC_FORCE_INCREMENT ).getSingleResult();
		assertFalse( reread.getVersion().equals( initial ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lockable.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testPessimisticForcedIncrementSpecific() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable lock = new Lockable( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable reread = em.createQuery( "from Lockable l", Lockable.class )
				.setHint( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE+".l", LockModeType.PESSIMISTIC_FORCE_INCREMENT )
				.getSingleResult();
		assertFalse( reread.getVersion().equals( initial ) );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lockable.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testOptimisticForcedIncrementOverall() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable lock = new Lockable( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable reread = em.createQuery( "from Lockable", Lockable.class ).setLockMode( LockModeType.OPTIMISTIC_FORCE_INCREMENT ).getSingleResult();
		assertEquals( initial, reread.getVersion() );
		em.getTransaction().commit();
		em.close();
		assertFalse( reread.getVersion().equals( initial ) );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lockable.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testOptimisticForcedIncrementSpecific() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable lock = new Lockable( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable reread = em.createQuery( "from Lockable l", Lockable.class )
				.setHint( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE+".l", LockModeType.OPTIMISTIC_FORCE_INCREMENT )
				.getSingleResult();
		assertEquals( initial, reread.getVersion() );
		em.getTransaction().commit();
		em.close();
		assertFalse( reread.getVersion().equals( initial ) );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lockable.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testOptimisticOverall() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable lock = new Lockable( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable reread = em.createQuery( "from Lockable", Lockable.class )
				.setLockMode( LockModeType.OPTIMISTIC )
				.getSingleResult();
		assertEquals( initial, reread.getVersion() );
		assertTrue( em.unwrap( SessionImpl.class ).getActionQueue().hasBeforeTransactionActions() );
		em.getTransaction().commit();
		em.close();
		assertEquals( initial, reread.getVersion() );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lockable.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	public void testOptimisticSpecific() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable lock = new Lockable( "name" );
		em.persist( lock );
		em.getTransaction().commit();
		em.close();
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Lockable reread = em.createQuery( "from Lockable l", Lockable.class )
				.setHint( AvailableSettings.ALIAS_SPECIFIC_LOCK_MODE+".l", LockModeType.OPTIMISTIC )
				.getSingleResult();
		assertEquals( initial, reread.getVersion() );
		assertTrue( em.unwrap( SessionImpl.class ).getActionQueue().hasBeforeTransactionActions() );
		em.getTransaction().commit();
		em.close();
		assertEquals( initial, reread.getVersion() );

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.remove( em.getReference( Lockable.class, reread.getId() ) );
		em.getTransaction().commit();
		em.close();
	}
}
