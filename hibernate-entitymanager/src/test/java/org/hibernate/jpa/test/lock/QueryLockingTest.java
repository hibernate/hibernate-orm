/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.lock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Table;

import org.hibernate.LockMode;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.AvailableSettings;
import org.hibernate.jpa.QueryHints;
import org.hibernate.jpa.internal.QueryImpl;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

/**
 * @author Steve Ebersole
 */
public class QueryLockingTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Lockable.class, LocalEntity.class };
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
	@TestForIssue( jiraKey = "HHH-8756" )
	public void testNoneLockModeForNonSelectQueryAllowed() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		QueryImpl jpaQuery = em.createQuery( "delete from Lockable l" ).unwrap( QueryImpl.class );

		org.hibernate.internal.QueryImpl hqlQuery = (org.hibernate.internal.QueryImpl) jpaQuery.getHibernateQuery();
		assertEquals( LockMode.NONE, hqlQuery.getLockOptions().getLockMode() );

		jpaQuery.setLockMode( LockModeType.NONE );

		em.getTransaction().commit();
		em.clear();
		
		// ensure other modes still throw the exception
		em.getTransaction().begin();
		jpaQuery = em.createQuery( "delete from Lockable l" ).unwrap( QueryImpl.class );

		hqlQuery = (org.hibernate.internal.QueryImpl) jpaQuery.getHibernateQuery();
		assertEquals( LockMode.NONE, hqlQuery.getLockOptions().getLockMode() );

		try {
			// Throws IllegalStateException
			jpaQuery.setLockMode( LockModeType.PESSIMISTIC_WRITE );
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
		QueryImpl query = em.createNativeQuery( "select * from lockable l" ).unwrap( QueryImpl.class );

		org.hibernate.internal.SQLQueryImpl hibernateQuery = (org.hibernate.internal.SQLQueryImpl) query.getHibernateQuery();

		// the spec disallows calling setLockMode in a native SQL query
		try {
			query.setLockMode( LockModeType.READ );
			fail( "Should have failed" );
		}
		catch (IllegalStateException expected) {
		}

		// however, we should be able to set it using hints
		query.setHint( QueryHints.HINT_NATIVE_LOCKMODE, LockModeType.READ );
		// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
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
	@TestForIssue(jiraKey = "HHH-9419")
	public void testNoVersionCheckAfterRemove() {
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
		em.remove( reread );
		em.getTransaction().commit();
		em.close();
		assertEquals( initial, reread.getVersion() );
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

	/**
	 * lock some entities via a query and check the resulting lock mode type via EntityManager
	 */
	@Test
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

	@Entity( name = "LocalEntity" )
	@Table( name = "LocalEntity" )
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
