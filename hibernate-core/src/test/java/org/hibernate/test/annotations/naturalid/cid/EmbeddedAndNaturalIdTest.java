/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.naturalid.cid;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Donnchadh O Donnabhain
 */
public class EmbeddedAndNaturalIdTest extends BaseCoreFunctionalTestCase {
	@TestForIssue(jiraKey = "HHH-9333")
	@Test
	public void testSave() {
		A account = new A( new AId( 1 ), "testCode" );
		inTransaction(
				session ->
					session.save( account )
		);
	}

	@TestForIssue(jiraKey = "HHH-9333")
	@Test
	public void testNaturalIdCriteria() {
		inTransaction(
				s -> {
					A u = new A( new AId( 1 ), "testCode" );
					s.persist( u );
				}
		);

		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<A> criteria = criteriaBuilder.createQuery( A.class );
					Root<A> root = criteria.from( A.class );
					criteria.where( criteriaBuilder.equal( root.get( "shortCode" ), "testCode" ) );
					A u = s.createQuery( criteria ).uniqueResult();
//        u = ( A ) s.createCriteria( A.class )
//                .add( Restrictions.naturalId().set( "shortCode", "testCode" ) )
//                .uniqueResult();
					assertNotNull( u );
				}
		);
	}

	@TestForIssue(jiraKey = "HHH-9333")
	@Test
	public void testByNaturalId() {
		inTransaction(
				s -> {
					A u = new A( new AId( 1 ), "testCode" );
					s.persist( u );
				}
		);

		inTransaction(
				s -> {
					A u = s.byNaturalId( A.class ).using( "shortCode", "testCode" ).load();
					assertNotNull( u );
				}
		);
	}

	@After
	public void tearDown() {
		// clean up
		inTransaction(
				session ->
					session.createQuery( "delete A" ).executeUpdate()

		);
	}

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				A.class,
				AId.class
		};
	}

}
