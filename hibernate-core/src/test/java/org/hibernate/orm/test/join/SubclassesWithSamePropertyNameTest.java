/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.join;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Two subclasses of Reportable each have a property with the same name:
 * Bug#detail and BlogEntry#detail. BlogEntry#detail is stored on a
 * join (secondary) table. Bug#detail is actually a collection, so its
 * values should be stored in a collection table.
 *
 * @author Gail Badner
 */
@JiraKey(value = "HHH-11241")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/join/Reportable.hbm.xml"
)
@SessionFactory
public class SubclassesWithSamePropertyNameTest {
	private Long blogEntryId;

	@BeforeEach
	public void prepareTest(SessionFactoryScope scope) {
		BlogEntry blogEntry = new BlogEntry();
		scope.inTransaction(
				s -> {
					blogEntry.setDetail( "detail" );
					blogEntry.setReportedBy( "John Doe" );
					s.persist( blogEntry );
				}
		);
		blogEntryId = blogEntry.getId();
	}

	@AfterEach
	public void cleanupTest(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-11241")
	public void testGetSuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					Reportable reportable = s.get( Reportable.class, blogEntryId );
					assertEquals( "John Doe", reportable.getReportedBy() );
					assertEquals( "detail", ( (BlogEntry) reportable ).getDetail() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-11241")
	public void testQuerySuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
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
	@JiraKey(value = "HHH-11241")
	public void testCriteriaSuperclass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<Reportable> criteria = criteriaBuilder.createQuery( Reportable.class );
					Root<Reportable> root = criteria.from( Reportable.class );
					criteria.where( criteriaBuilder.equal( root.get( "reportedBy" ), "John Doe" ) );
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
	@JiraKey(value = "HHH-11241")
	public void testQuerySubclass(SessionFactoryScope scope) {
		scope.inTransaction(
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
	@JiraKey(value = "HHH-11241")
	public void testCriteriaSubclass(SessionFactoryScope scope) {
		scope.inTransaction(
				s -> {
					CriteriaBuilder criteriaBuilder = s.getCriteriaBuilder();
					CriteriaQuery<BlogEntry> criteria = criteriaBuilder.createQuery( BlogEntry.class );
					Root<BlogEntry> root = criteria.from( BlogEntry.class );
					criteria.where( criteriaBuilder.equal( root.get( "reportedBy" ), "John Doe" ) );
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
