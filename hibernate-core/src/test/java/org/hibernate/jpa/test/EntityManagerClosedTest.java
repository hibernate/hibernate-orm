/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test;

import java.util.Date;
import java.util.GregorianCalendar;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.Id;
import javax.persistence.LockModeType;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Gail Badner
 */
public class EntityManagerClosedTest extends BaseEntityManagerFunctionalTestCase {

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetMetamodel() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.getMetamodel();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetMetamodelWithTransaction () {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.close();
		try {
			em.getMetamodel();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// make sure transaction is set for rollback
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetCriteriaBuilder() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.getCriteriaBuilder();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetCriteriaBuilderWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.close();
		try {
			em.getCriteriaBuilder();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// make sure transaction is set for rollback
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetEntityManagerFactory() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.getEntityManagerFactory();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetEntityManagerFactoryWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.close();
		try {
			em.getEntityManagerFactory();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// make sure transaction is set for rollback
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testCreateNamedQuery() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.createNamedQuery( "abc" );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testCreateNamedQueryWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.close();
		try {
			em.createNamedQuery( "abc" );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// make sure transaction is set for rollback
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetFlushMode() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.getFlushMode();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testSetFlushMode() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.setFlushMode( FlushModeType.AUTO );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testGetDelegate() {
		EntityManager em = getOrCreateEntityManager();
		em.close();
		try {
			em.getDelegate();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetParametersWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		query.setParameter( "name", "AName" );
		em.close();
		try {
			query.getParameters();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// txn should not be set for rollback
			assertFalse( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetParameterByPositionWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity where name = ?1" );
		query.setParameter( 1, "AName" );
		em.close();
		try {
			query.getParameter( 1 );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// txn should not be set for rollback
			assertFalse( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetParameterByNameWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		em.close();
		try {
			query.getParameter( "name" );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// txn should not be set for rollback
			assertFalse( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetParameterValueByParameterWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity where name = ?1" );
		query.setParameter( 1, "AName" );
		Parameter p = query.getParameter( 1 );
		em.close();
		try {
			query.getParameterValue( p );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// txn should not be set for rollback
			assertFalse( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetParameterValueByStringWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		query.setParameter( "name", "AName" );
		em.close();
		try {
			query.getParameterValue( "name" );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// txn should not be set for rollback
			assertFalse( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryIsBound() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		query.setParameter( "name", "AName" );
		Parameter parameter = query.getParameter( "name" );
		em.close();
		try {
			query.isBound( parameter );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetFirstResult() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		query.setParameter( "name", "AName" );
		em.close();
		try {
			query.setFirstResult( 1 );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetFlushMode() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		query.setParameter( "name", "AName" );
		em.close();
		try {
			query.setFlushMode( FlushModeType.AUTO );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetLockMode() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where name = :name" );
		query.setParameter( "name", "AName" );
		em.close();
		try {
			query.setLockMode( LockModeType.OPTIMISTIC );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetCalendarDateParameterByPosition() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where birthDay = ?1" );
		em.close();
		try {
			query.setParameter( 1, new GregorianCalendar( 2000, 4, 1 ), TemporalType.DATE );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetDateParameterByPosition() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where birthDay = ?1" );
		em.close();
		try {
			query.setParameter( 1, new Date(), TemporalType.DATE );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetIntParameterByPosition() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where intValue = ?1" );
		em.close();
		try {
			query.setParameter( 1, 1 );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetCalendarDateParameterByParameter() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where birthDay = ?1" );
		Parameter parameter = query.getParameter( 1 );
		em.close();
		try {
			query.setParameter( parameter, new GregorianCalendar( 2000, 4, 1 ), TemporalType.DATE );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetDateParameterByParameter() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where birthDay = ?1" );
		Parameter parameter = query.getParameter( 1 );
		em.close();
		try {
			query.setParameter( parameter, new Date(), TemporalType.DATE );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetIntParameterByParameter() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where intValue = ?1" );
		Parameter parameter = query.getParameter( 1 );
		em.close();
		try {
			query.setParameter( parameter, 1 );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetCalendarParameterByName() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where birthDay = :bday" );
		em.close();
		try {
			query.setParameter( "bday", new GregorianCalendar( 2000, 4, 1 ), TemporalType.DATE );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetSingleResult() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity" );
		em.close();
		try {
			query.getSingleResult();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetDateParameterByName() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where birthDay = :bday" );
		em.close();
		try {
			query.setParameter( "bday", new Date(), TemporalType.DATE );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQuerySetIntParameterByName() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity where intValue = :ival" );
		em.close();
		try {
			query.setParameter( "ival", 1 );
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetFlushMode() {
		EntityManager em = getOrCreateEntityManager();
		Query query = em.createQuery( "from AnEntity" );
		em.close();
		try {
			query.getFlushMode();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetFlushModeWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity" );
		em.close();
		try {
			query.getFlushMode();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetLockModeWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity" );
		em.close();
		try {
			query.getLockMode();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			// transaction should not be set for rollback
			assertFalse( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetMaxResultsWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity" );
		em.close();
		try {
			query.getMaxResults();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Test
	@TestForIssue( jiraKey = "HHH-12110")
	public void testQueryGetFirstResultWithTransaction() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		Query query = em.createQuery( "from AnEntity" );
		em.close();
		try {
			query.getFirstResult();
			fail( "should have thrown IllegalStateException" );
		}
		catch (IllegalStateException ex) {
			// expected
			assertTrue( em.getTransaction().getRollbackOnly() );
		}
		finally {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
		}
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] { AnEntity.class };
	}

	@Entity(name = "AnEntity")
	private static class AnEntity {
		@Id
		private long id;

		private String name;
		private Date birthDay;
		private int intValue;
	}
}
