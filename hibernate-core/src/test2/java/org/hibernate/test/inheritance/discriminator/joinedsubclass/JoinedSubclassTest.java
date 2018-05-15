/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.inheritance.discriminator.joinedsubclass;

import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
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
			SubSubEntity loaded = (SubSubEntity) session.createCriteria( SubSubEntity.class )
					.add( Restrictions.idEq( subSubEntityId ) )
					.uniqueResult();
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
			SubSubSubEntity loaded = (SubSubSubEntity) session.createCriteria( SubSubSubEntity.class )
					.add( Restrictions.idEq( subSubEntityId ) )
					.uniqueResult();
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
					.setLong( "id", subSubEntityId )
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
					.setLong( "id", subSubEntityId )
					.uniqueResult();
			assertNull( loaded );
		}
		finally {
			session.close();
		}
	}

}
