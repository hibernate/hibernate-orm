/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa;

import jakarta.persistence.Entity;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TemporalType;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gail Badner
 */
@Jpa(annotatedClasses = {EntityManagerClosedTest.AnEntity.class})
public class EntityManagerClosedTest {

	private static final String ERRMSG = "should have thrown IllegalStateException";

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetMetamodel(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getMetamodel,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetMetamodelWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getMetamodel,
					ERRMSG
			);
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetCriteriaBuilder(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getCriteriaBuilder,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetCriteriaBuilderWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getCriteriaBuilder,
					ERRMSG
			);
			// make sure transaction is set for rollback
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetEntityManagerFactory(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getEntityManagerFactory,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetEntityManagerFactoryWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getEntityManagerFactory,
					ERRMSG
			);
			// make sure transaction is set for rollback
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testCreateNamedQuery(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> entityManager.createNamedQuery( "abc" ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testCreateNamedQueryWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> entityManager.createNamedQuery( "abc" ),
					ERRMSG
			);
			// make sure transaction is set for rollback
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getFlushMode,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testSetFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> entityManager.setFlushMode( FlushModeType.AUTO ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testGetDelegate(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					entityManager::getDelegate,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetParametersWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			query.setParameter( "name", "AName" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getParameters,
					ERRMSG
			);
			// txn should not be set for rollback
			assertFalse( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetParameterByPositionWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = ?1" );
			query.setParameter( 1, "AName" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.getParameter( 1 ),
					ERRMSG
			);
			// txn should not be set for rollback
			assertFalse( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetParameterByNameWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.getParameter( "name" ),
					ERRMSG
			);
			// txn should not be set for rollback
			assertFalse( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetParameterValueByParameterWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = ?1" );
			query.setParameter( 1, "AName" );
			Parameter p = query.getParameter( 1 );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.getParameterValue( p ),
					ERRMSG
			);
			// txn should not be set for rollback
			assertFalse( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetParameterValueByStringWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			query.setParameter( "name", "AName" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.getParameterValue( "name" ),
					ERRMSG
			);
			// txn should not be set for rollback
			assertFalse( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryIsBound(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			query.setParameter( "name", "AName" );
			Parameter parameter = query.getParameter( "name" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.isBound( parameter ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetFirstResult(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			query.setParameter( "name", "AName" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setFirstResult( 1 ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			query.setParameter( "name", "AName" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setFlushMode( FlushModeType.AUTO ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetLockMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where name = :name" );
			query.setParameter( "name", "AName" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setLockMode( LockModeType.OPTIMISTIC ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetCalendarDateParameterByPosition(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where birthDay = ?1" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( 1, new GregorianCalendar( 2000, 4, 1 ), TemporalType.DATE ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetDateParameterByPosition(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where birthDay = ?1" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( 1, new Date(), TemporalType.DATE ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetIntParameterByPosition(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where intValue = ?1" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( 1, 1 ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetCalendarDateParameterByParameter(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where birthDay = ?1" );
			Parameter parameter = query.getParameter( 1 );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( parameter, new GregorianCalendar( 2000, 4, 1 ), TemporalType.DATE ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetDateParameterByParameter(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where birthDay = ?1" );
			Parameter parameter = query.getParameter( 1 );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( parameter, new Date(), TemporalType.DATE ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetIntParameterByParameter(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where intValue = ?1" );
			Parameter parameter = query.getParameter( 1 );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( parameter, 1 ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetCalendarParameterByName(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where birthDay = :bday" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( "bday", new GregorianCalendar( 2000, 4, 1 ), TemporalType.DATE ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetSingleResult(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getSingleResult,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetDateParameterByName(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where birthDay = :bday" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( "bday", new Date(), TemporalType.DATE ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQuerySetIntParameterByName(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity where intValue = :ival" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					() -> query.setParameter( "ival", 1 ),
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetFlushMode(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getFlushMode,
					ERRMSG
			);
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetFlushModeWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getFlushMode,
					ERRMSG
			);
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetLockModeWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getLockMode,
					ERRMSG
			);
			// transaction should not be set for rollback
			assertFalse( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetMaxResultsWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getMaxResults,
					ERRMSG
			);
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-12110")
	public void testQueryGetFirstResultWithTransaction(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			Query query = entityManager.createQuery( "from AnEntity" );
			entityManager.close();
			assertThrows(
					IllegalStateException.class,
					query::getFirstResult,
					ERRMSG
			);
			assertTrue( entityManager.getTransaction().getRollbackOnly() );
		} );
	}

	@Entity(name = "AnEntity")
	public static class AnEntity {
		@Id
		private long id;

		private String name;
		private Date birthDay;
		private int intValue;
	}
}
