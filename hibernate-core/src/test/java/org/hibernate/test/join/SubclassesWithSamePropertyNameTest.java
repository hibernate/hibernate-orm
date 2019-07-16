/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import static org.junit.Assert.assertEquals;

/**
 * Two subclasses of Reportable each have a property with the same name:
 * Bug#detail and BlogEntry#detail. BlogEntry#detail is stored on a
 * join (secondary) table. Bug#detail is actually a collection, so its
 * values should be stored in a collection table.
 *
 * @author Gail Badner
 */
@TestForIssue(jiraKey = "HHH-11241")
public class SubclassesWithSamePropertyNameTest extends BaseCoreFunctionalTestCase {
	private Long blogEntryId;

	@Override
	public String[] getMappings() {
		return new String[] { "join/Reportable.hbm.xml" };
	}

	@Override
	protected void prepareTest() {
		BlogEntry blogEntry = new BlogEntry();
		inTransaction(
				s -> {
					blogEntry.setDetail( "detail" );
					blogEntry.setReportedBy( "John Doe" );
					s.persist( blogEntry );
				}
		);
		blogEntryId = blogEntry.getId();
	}

	@Override
	protected void cleanupTest() {
		inTransaction(
				s -> s.createQuery( "delete from BlogEntry" ).executeUpdate()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11241")
	public void testGetSuperclass() {
		inTransaction(
				s -> {
					Reportable reportable = s.get( Reportable.class, blogEntryId );
					assertEquals( "John Doe", reportable.getReportedBy() );
					assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );
				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11241")
	public void testQuerySuperclass() {
		inTransaction(
				s -> {
					Reportable reportable = (Reportable) s.createQuery(
							"from Reportable where reportedBy='John Doe'"
					).uniqueResult();
					assertEquals( "John Doe", reportable.getReportedBy() );
					assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );

				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11241")
	public void testCriteriaSuperclass() {
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Reportable> criteria = criteriaBuilder.createQuery( Reportable.class );
					Root<Reportable> root = criteria.from( Reportable.class );
					criteria.where( criteriaBuilder.equal( root.get( "reportedBy" ),"John Doe" ) );
					Reportable reportable = s.createQuery( criteria ).uniqueResult();
//					Reportable reportable =
//							(Reportable) s.createCriteria( Reportable.class, "r" )
//									.add( Restrictions.eq( "r.reportedBy", "John Doe" ) )
//									.uniqueResult();
					assertEquals( "John Doe", reportable.getReportedBy() );
					assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );

				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11241")
	public void testQuerySubclass() {
		inTransaction(
				s -> {
					BlogEntry blogEntry = (BlogEntry) s.createQuery(
							"from BlogEntry where reportedBy='John Doe'"
					).uniqueResult();
					assertEquals( "John Doe", blogEntry.getReportedBy() );
					assertEquals( "detail", ( blogEntry ).getDetail() );

				}
		);
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11241")
	public void testCriteriaSubclass() {
		inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<BlogEntry> criteria = criteriaBuilder.createQuery( BlogEntry.class );
					Root<BlogEntry> root = criteria.from( BlogEntry.class );
					criteria.where( criteriaBuilder.equal( root.get( "reportedBy" ),"John Doe" ) );
					BlogEntry blogEntry = s.createQuery( criteria ).uniqueResult();
//					BlogEntry blogEntry =
//							(BlogEntry) s.createCriteria( BlogEntry.class, "r" )
//									.add( Restrictions.eq( "r.reportedBy", "John Doe" ) )
//									.uniqueResult();
					assertEquals( "John Doe", blogEntry.getReportedBy() );
					assertEquals( "detail", ( blogEntry ).getDetail() );

				}
		);
	}
}
