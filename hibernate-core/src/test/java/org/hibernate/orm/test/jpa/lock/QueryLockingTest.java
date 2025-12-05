/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.lock;

import java.util.List;

import org.hibernate.LockMode;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.NativeQuery;

import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.hibernate.testing.orm.junit.RequiresDialect;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 */
@Jpa(
		annotatedClasses = {Person.class, Lockable.class, QueryLockingTest.LocalEntity.class}
)
public class QueryLockingTest {

	@Test
	public void testOverallLockMode(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			org.hibernate.query.Query query = em.createQuery( "from Lockable l" )
					.unwrap( org.hibernate.query.Query.class );
			assertEquals( LockMode.NONE, query.getLockOptions().getLockMode() );

			// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
			query.setLockMode( LockModeType.READ );
			assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getLockMode() );

			query.setHint( HINT_NATIVE_LOCK_MODE, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( LockMode.PESSIMISTIC_WRITE, query.getLockOptions().getLockMode() );
		} );
	}

	@Test
	@JiraKey( value = "HHH-8756" )
	public void testNoneLockModeForNonSelectQueryAllowed(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			org.hibernate.query.Query query = em.createQuery( "delete from Lockable l" )
					.unwrap( org.hibernate.query.Query.class );

			assertEquals( LockMode.NONE, query.getLockOptions().getLockMode() );

			query.setLockMode( LockModeType.NONE );

		} );
		// ensure other modes still throw the exception
		scope.inTransaction( em -> {
			org.hibernate.query.Query query = em.createQuery( "delete from Lockable l" ).unwrap( org.hibernate.query.Query.class );
			assertEquals( LockMode.NONE, query.getLockOptions().getLockMode() );
			Assertions.assertThrows(
					IllegalStateException.class,
					() -> {
						// Throws IllegalStateException
						query.setLockMode( LockModeType.PESSIMISTIC_WRITE );
					},
					"IllegalStateException should have been thrown."
			);
		} );
	}

	@Test
	public void testNativeSql(EntityManagerFactoryScope scope) {
		scope.inTransaction( em -> {
			NativeQuery query = em.createNativeQuery( "select * from lockable l" ).unwrap( NativeQuery.class );

			// the spec disallows calling setLockMode() and getLockMode()
			// on a native SQL query and requires that an IllegalStateException
			// be thrown
			Assertions.assertThrows(
					IllegalStateException.class,
					() -> query.setLockMode( LockModeType.READ ),
					"Should have thrown IllegalStateException"
			);

			Assertions.assertThrows(
					IllegalStateException.class,
					() -> query.getLockMode(),
					"Should have thrown IllegalStateException"
			);

			// however, we should be able to set it using hints
			query.setHint( HINT_NATIVE_LOCK_MODE, LockModeType.READ );
			// NOTE : LockModeType.READ should map to LockMode.OPTIMISTIC
			assertEquals( LockMode.OPTIMISTIC, query.getLockOptions().getLockMode() );

			query.setHint( HINT_NATIVE_LOCK_MODE, LockModeType.PESSIMISTIC_WRITE );
			assertEquals( LockMode.PESSIMISTIC_WRITE, query.getLockOptions().getLockMode() );

		} );
	}

	@Test
	@SkipForDialect( dialectClass = CockroachDialect.class )
	public void testPessimisticForcedIncrementOverall(EntityManagerFactoryScope scope) {
		Lockable lock = new Lockable( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		Integer id = scope.fromTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class ).setLockMode( LockModeType.PESSIMISTIC_FORCE_INCREMENT ).getSingleResult();
			assertFalse( reread.getVersion().equals( initial ) );
			return reread.getId();
		} );

		scope.inTransaction( em -> em.remove( em.getReference( Lockable.class, id ) ) );
	}

	@Test
	@SkipForDialect( dialectClass = CockroachDialect.class )
	public void testPessimisticForcedIncrementSpecific(EntityManagerFactoryScope scope) {
		Lockable lock = new Lockable( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		Integer id = scope.fromTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable l", Lockable.class )
					.setHint( HINT_NATIVE_LOCK_MODE + ".l", LockModeType.PESSIMISTIC_FORCE_INCREMENT )
					.getSingleResult();
			assertFalse( reread.getVersion().equals( initial ) );
			return reread.getId();
		} );

		scope.inTransaction( em -> em.remove( em.getReference( Lockable.class, id ) ) );
	}

	@Test
	public void testOptimisticForcedIncrementOverall(EntityManagerFactoryScope scope) {
		Lockable lock = new Lockable( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		Integer id = scope.fromTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class ).setLockMode( LockModeType.OPTIMISTIC_FORCE_INCREMENT ).getSingleResult();
			assertEquals( initial, reread.getVersion() );
			return reread.getId();
		} );
		scope.inTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class ).getSingleResult();
			assertFalse( reread.getVersion().equals( initial ) );
		} );

		scope.inTransaction( em -> em.remove( em.getReference( Lockable.class, id ) ) );
	}

	@Test
	public void testOptimisticForcedIncrementSpecific(EntityManagerFactoryScope scope) {
		Lockable lock = new Lockable( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		Integer id = scope.fromTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable l", Lockable.class )
					.setHint( HINT_NATIVE_LOCK_MODE, LockModeType.OPTIMISTIC_FORCE_INCREMENT )
					.getSingleResult();
			assertEquals( initial, reread.getVersion() );
			return reread.getId();
		} );
		scope.inTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class ).getSingleResult();
			assertFalse( reread.getVersion().equals( initial ) );
		} );

		scope.inTransaction( em -> em.remove( em.getReference( Lockable.class, id ) ) );
	}

	@Test
	public void testOptimisticOverall(EntityManagerFactoryScope scope) {
		Lockable lock = new Lockable( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		Integer id = scope.fromTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class )
					.setLockMode( LockModeType.OPTIMISTIC )
					.getSingleResult();
			assertEquals( initial, reread.getVersion() );
			assertTrue( em.unwrap( SessionImplementor.class ).getActionQueue().hasBeforeTransactionActions() );
			return reread.getId();
		} );
		scope.inTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class ).getSingleResult();
			assertEquals( initial, reread.getVersion() );
		} );

		scope.inTransaction( em -> em.remove( em.getReference( Lockable.class, id ) ) );
	}

	@Test
	public void testOptimisticSpecific(EntityManagerFactoryScope scope) {
		Lockable lock = new Lockable( "name" );
		scope.inTransaction( em -> em.persist( lock ) );
		Integer initial = lock.getVersion();
		assertNotNull( initial );

		Integer id = scope.fromTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable l", Lockable.class )
					.setHint( HINT_NATIVE_LOCK_MODE, LockModeType.OPTIMISTIC )
					.getSingleResult();
			assertEquals( initial, reread.getVersion() );
			assertTrue( em.unwrap( SessionImplementor.class ).getActionQueue().hasBeforeTransactionActions() );
			return reread.getId();
		} );
		scope.inTransaction( em -> {
			Lockable reread = em.createQuery( "from Lockable", Lockable.class ).getSingleResult();
			assertEquals( initial, reread.getVersion() );
		} );

		scope.inTransaction( em -> em.remove( em.getReference( Lockable.class, id ) ) );
	}

	/**
	 * lock some entities via a query and check the resulting lock mode type via EntityManager
	 */
	@Test
	@RequiresDialectFeature( feature = DialectFeatureChecks.SupportFollowOnLocking.class, reverse = true)
	public void testEntityLockModeStateAfterQueryLocking(EntityManagerFactoryScope scope) {
		// Create some test data
		scope.inEntityManager( em -> {
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
		} );

		// clean up test data
		scope.inTransaction( em -> em.createQuery( "delete from LocalEntity" ).executeUpdate() );
	}

	@Test
	@JiraKey(value = "HHH-11376")
	@RequiresDialect( SQLServerDialect.class )
	public void testCriteriaWithPessimisticLock(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
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

			assertTrue( resultList.isEmpty() );
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
