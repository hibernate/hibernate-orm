/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-9302")
public class JoinedSubclassTest extends BaseCoreFunctionalTestCase {

	private Long subSubEntityId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {RootEntity.class, SubEntity.class, SubSubEntity.class, SubSubSubEntity.class};
	}

	@Before
	public void setup() {
		session = openSession();
		Transaction transaction = session.beginTransaction();

		final SubSubEntity subSubEntity = new SubSubEntity();
		final SubEntity subEntity = new SubSubEntity();
		try {
			session.save( subEntity );
			session.save( subSubEntity );
			transaction.commit();
			subSubEntityId = subSubEntity.getId();
		}
		finally {
			if ( transaction.getStatus() == TransactionStatus.ACTIVE ) {
				transaction.rollback();
			}
		}
		session.close();
	}

	@Test
	public void shouldRetrieveSubEntity() {
		session = openSession();
		try {
			RootEntity loaded = session.get( SubEntity.class, subSubEntityId );
			assertNotNull( loaded );
			assertTrue( loaded instanceof SubSubEntity );
		}
		finally {
			session.close();
		}
	}

	public void shouldNotRetrieveSubSubSubEntity() {
		session = openSession();
		try {
			SubSubSubEntity loaded = session.get( SubSubSubEntity.class, subSubEntityId );
			assertNull( loaded );
		}
		finally {
			session.close();
		}
	}

	// Criteria

	@Test
	public void shouldRetrieveSubSubEntityWithCriteria() {
		session = openSession();
		try {
			final CriteriaBuilder criteriaBuilder = session.getSessionFactory().getCriteriaBuilder();
			final CriteriaQuery<SubSubEntity> criteria = criteriaBuilder.createQuery( SubSubEntity.class );
			final Root<SubSubEntity> root = criteria.from( SubSubEntity.class );
			criteria.where( criteriaBuilder.equal( root.get( SubSubEntity_.id ), subSubEntityId ) );
			final SubSubEntity loaded = session.createQuery( criteria ).uniqueResult();
			assertNotNull( loaded );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void shouldNotRetrieveSubSubSubEntityWithCriteria() {
		session = openSession();
		try {
			final CriteriaBuilder criteriaBuilder = session.getSessionFactory().getCriteriaBuilder();
			final CriteriaQuery<SubSubSubEntity> criteria = criteriaBuilder.createQuery( SubSubSubEntity.class );
			final Root<SubSubSubEntity> root = criteria.from( SubSubSubEntity.class );

			criteria.where( criteriaBuilder.equal( root.get( SubSubSubEntity_.id ), subSubEntityId ) );
			final SubSubEntity loaded = session.createQuery( criteria ).uniqueResult();
			assertNull( loaded );
		}
		finally {
			session.close();
		}
	}

	// HQL

	@Test
	public void shouldRetrieveSubSubEntityWithHQL() {
		session = openSession();
		try {
			SubSubEntity loaded = (SubSubEntity) session.createQuery(
					"select se from SubSubEntity se where se.id = :id" )
					.setParameter( "id", subSubEntityId )
					.uniqueResult();
			assertNotNull( loaded );
		}
		finally {
			session.close();
		}
	}

	@Test
	public void shouldNotRetrieveSubSubSubEntityWithHQL() {
		session = openSession();
		try {
			SubSubSubEntity loaded = (SubSubSubEntity) session.createQuery(
					"select se from SubSubSubEntity se where se.id = :id" )
					.setParameter( "id", subSubEntityId )
					.uniqueResult();
			assertNull( loaded );
		}
		finally {
			session.close();
		}
	}

}
