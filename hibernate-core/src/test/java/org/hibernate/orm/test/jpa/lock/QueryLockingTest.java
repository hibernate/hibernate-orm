/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.internal.SessionImpl;
import org.hibernate.orm.test.jpa.BaseEntityManagerFunctionalTestCase;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialect;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import static org.hibernate.jpa.HibernateHints.HINT_NATIVE_LOCK_MODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Steve Ebersole
 */
public class QueryLockingTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Person.class, Lockable.class, LocalEntity.class};
	}

	@Test
	public void testOverallLockMode() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		org.hibernate.query.Query query = em.createQuery( "from Lockable l" ).unwrap( org.hibernate.query.Query.class );
		assertEquals( LockMode.NONE, query.getLockOptions().getLockMode() );
		assertNull( query.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.NONE, query.getLockOptions().getEffectiveLockMode( "l" ) );

		// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
		query.setLockMode( LockModeType.READ );
		assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getLockMode() );
		assertNull( query.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getEffectiveLockMode( "l" ) );

		query.setHint( HINT_NATIVE_LOCK_MODE + ".l", LockModeType.PESSIMISTIC_WRITE );
		assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getLockMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, query.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.PESSIMISTIC_WRITE, query.getLockOptions().getEffectiveLockMode( "l" ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey( value = "HHH-8756" )
	public void testNoneLockModeForNonSelectQueryAllowed() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		org.hibernate.query.Query query = em.createQuery( "delete from Lockable l" ).unwrap( org.hibernate.query.Query.class );

		assertEquals( LockMode.NONE, query.getLockOptions().getLockMode() );

		query.setLockMode( LockModeType.NONE );

		em.getTransaction().commit();
		em.clear();

		// ensure other modes still throw the exception
		em.getTransaction().begin();
		query = em.createQuery( "delete from Lockable l" ).unwrap( org.hibernate.query.Query.class );
		assertEquals( LockMode.NONE, query.getLockOptions().getLockMode() );

		try {
			// Throws IllegalStateException
			query.setLockMode( LockModeType.PESSIMISTIC_WRITE );
			fail( "IllegalStateException should have been thrown." );
		}
		catch (IllegalStateException e) {
			// expected
		}
		finally {
			em.getTransaction().rollback();
			em.close();
		}
	}

	@Test
	public void testNativeSql() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		NativeQuery query = em.createNativeQuery( "select * from lockable l" ).unwrap( NativeQuery.class );

		// the spec disallows calling setLockMode() and getLockMode()
		// on a native SQL query and requires that an IllegalStateException
		// be thrown
		try {
			query.setLockMode( LockModeType.READ );
			fail( "Should have failed" );
		}
		catch (IllegalStateException expected) {
		}
		catch (Exception e) {
			fail( "Should have thrown IllegalStateException but threw " + e.getClass().getName() );
		}
		try {
			query.getLockMode();
			fail( "Should have failed" );
		}
		catch (IllegalStateException expected) {
		}
		catch (Exception e) {
			fail( "Should have thrown IllegalStateException but threw " + e.getClass().getName() );
		}

		// however, we should be able to set it using hints
		query.setHint( HINT_NATIVE_LOCK_MODE, LockModeType.READ );
		// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
		assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getLockMode() );
		assertNull( query.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getEffectiveLockMode( "l" ) );

		query.setHint( HINT_NATIVE_LOCK_MODE +".l", LockModeType.PESSIMISTIC_WRITE );
		assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getLockMode() );
		assertEquals( LockMode.PESSIMISTIC_WRITE, query.getLockOptions().getAliasSpecificLockMode( "l" ) );
		assertEquals( LockMode.PESSIMISTIC_WRITE, query.getLockOptions().getEffectiveLockMode( "l" ) );

		em.getTransaction().commit();
		em.close();
	}

	@Test
	@SkipForDialect( value = CockroachDialect.class )
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
	@SkipForDialect( value = CockroachDialect.class )
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
				.setHint( HINT_NATIVE_LOCK_MODE + ".l", LockModeType.PESSIMISTIC_FORCE_INCREMENT )
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
				.setHint( HINT_NATIVE_LOCK_MODE + ".l", LockModeType.OPTIMISTIC_FORCE_INCREMENT )
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
				.setHint( HINT_NATIVE_LOCK_MODE + ".l", LockModeType.OPTIMISTIC )
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

	/**
	 * lock some entities via a query and check the resulting lock mode type via EntityManager
	 */
	@Test
	@RequiresDialectFeature( value = DialectChecks.DoesNotSupportFollowOnLocking.class)
	public void testEntityLockModeStateAfterQueryLocking() {
		// Create some test data
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( new LocalEntity( 1, "test" ) );
		em.getTransaction().commit();
//		em.close();

		// issue the query with locking
//		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "select l from LocalEntity l" );
		assertEquals( LockModeType.NONE, query.getLockMode() );
		query.setLockMode( LockModeType.PESSIMISTIC_READ );
		assertEquals( LockModeType.PESSIMISTIC_READ, query.getLockMode() );
		List<LocalEntity> results = query.getResultList();

		// and check the lock mode for each result
		for ( LocalEntity e : results ) {
			assertEquals( LockModeType.PESSIMISTIC_READ, em.getLockMode( e ) );
		}

		em.getTransaction().commit();
		em.close();

		// clean up test data
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from LocalEntity" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@JiraKey(value = "HHH-11376")
	@RequiresDialect( SQLServerDialect.class )
	public void testCriteriaWithPessimisticLock() {
		TransactionUtil.doInJPA( this::entityManagerFactory, entityManager -> {
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			CriteriaQuery<Person> criteria = builder.createQuery( Person.class );
			Root<Person> personRoot = criteria.from( Person.class );
			ParameterExpression<Long> personIdParameter = builder.parameter( Long.class );

			// Eagerly fetch the parent
			personRoot.fetch( "parent", JoinType.LEFT );

			criteria.select( personRoot )
					.where( builder.equal( personRoot.get( "id" ), personIdParameter ) );

			final List<Person> resultList = entityManager.createQuery( criteria )
					.setParameter( personIdParameter, 1L )
					.setLockMode( LockModeType.PESSIMISTIC_WRITE )
					.getResultList();

			resultList.isEmpty();

		} );
	}

	@Entity(name = "LocalEntity")
	@Table(name = "LocalEntity")
	public static class LocalEntity {
		private Integer id;
		private String name;

		public LocalEntity() {
		}

		public LocalEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
