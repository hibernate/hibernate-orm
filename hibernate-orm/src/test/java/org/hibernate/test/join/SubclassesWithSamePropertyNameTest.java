/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.join;

import org.junit.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
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
@TestForIssue( jiraKey = "HHH-11241" )
public class SubclassesWithSamePropertyNameTest extends BaseCoreFunctionalTestCase {
	private Long blogEntryId;

	@Override
	public String[] getMappings() {
		return new String[] { "join/Reportable.hbm.xml" };
	}

	@Override
	protected void prepareTest() {
		Session s = openSession();
		s.getTransaction().begin();
		BlogEntry blogEntry = new BlogEntry();
		blogEntry.setDetail( "detail" );
		blogEntry.setReportedBy( "John Doe" );
		s.persist( blogEntry );
		s.getTransaction().commit();
		s.close();

		blogEntryId = blogEntry.getId();
	}

	@Override
	protected void cleanupTest() {
		Session s = openSession();
		s.getTransaction().begin();
		s.createQuery( "delete from BlogEntry" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11241" )
	public void testGetSuperclass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Reportable reportable = s.get( Reportable.class, blogEntryId );
		assertEquals( "John Doe", reportable.getReportedBy() );
		assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11241" )
	public void testQuerySuperclass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Reportable reportable = (Reportable) s.createQuery(
				"from Reportable where reportedBy='John Doe'"
		).uniqueResult();
		assertEquals( "John Doe", reportable.getReportedBy() );
		assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11241" )
	public void testCriteriaSuperclass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		Reportable reportable =
				(Reportable) s.createCriteria( Reportable.class, "r" )
						.add( Restrictions.eq( "r.reportedBy", "John Doe" ) )
						.uniqueResult();
		assertEquals( "John Doe", reportable.getReportedBy() );
		assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11241" )
	public void testQuerySubclass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		BlogEntry blogEntry = (BlogEntry) s.createQuery(
				"from BlogEntry where reportedBy='John Doe'"
		).uniqueResult();
		assertEquals( "John Doe", blogEntry.getReportedBy() );
		assertEquals( "detail", ( blogEntry ).getDetail() );
		tx.commit();
		s.close();
	}

	@Test
	@TestForIssue( jiraKey = "HHH-11241" )
	public void testCriteriaSubclass() {
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		BlogEntry blogEntry =
				(BlogEntry) s.createCriteria( BlogEntry.class, "r" )
						.add( Restrictions.eq( "r.reportedBy", "John Doe" ) )
						.uniqueResult();
		assertEquals( "John Doe", blogEntry.getReportedBy() );
		assertEquals( "detail", ( blogEntry ).getDetail() );
		tx.commit();
		s.close();
	}
}
