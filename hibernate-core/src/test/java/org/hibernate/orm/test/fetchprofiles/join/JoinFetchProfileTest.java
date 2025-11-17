/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.fetchprofiles.join;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import org.hibernate.Hibernate;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Various tests related to join-style fetch profiles.
 *
 * @author Steve Ebersole
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "mappings/fetchprofile/Mappings.hbm.xml")
@SessionFactory(generateStatistics = true)
public class JoinFetchProfileTest {
	@BeforeEach
	void createTestData(SessionFactoryScope sessions) {
		final Department literatureDepartment = new Department( 1, "lit", "Literature" );
		final Student me = new Student( 1, "Steve" );
		final Course lit101 = new Course( 1, new Course.Code( literatureDepartment, 101 ), "Introduction to Literature" );
		final CourseOffering section = new CourseOffering( 1, lit101, 1, 2008 );
		final Enrollment enrollment = new Enrollment( 1, section, me );
		sessions.inTransaction( (session) -> {
			session.persist( literatureDepartment );
			session.persist( lit101 );
			session.persist( section );
			session.persist( me );
			section.getEnrollments().add( enrollment );
			session.persist( enrollment );
		} );
	}

	@AfterEach
	void dropTestsData(SessionFactoryScope sessions) {
		sessions.dropData();
	}

	@Test
	public void testNormalLoading(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			CourseOffering section = session.get( CourseOffering.class, 1 );
			Assertions.assertEquals( 1, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertFalse( Hibernate.isInitialized( section.getCourse() ) );
			Assertions.assertFalse( Hibernate.isInitialized( section.getEnrollments() ) );
			Assertions.assertFalse( Hibernate.isInitialized( section.getCourse()
					.getCode()
					.getDepartment() ) );
			Assertions.assertTrue( Hibernate.isInitialized( section.getCourse() ) );
			Assertions.assertEquals( 1, statistics.getEntityFetchCount() );
		} );
	}

	@Test
	public void testNormalCriteria(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<CourseOffering> criteria = criteriaBuilder.createQuery( CourseOffering.class );
			criteria.from( CourseOffering.class );
			CourseOffering section = session.createQuery( criteria ).uniqueResult();
			Assertions.assertEquals( 1, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertFalse( Hibernate.isInitialized( section.getCourse() ) );
			Assertions.assertFalse( Hibernate.isInitialized( section.getEnrollments() ) );
			Assertions.assertFalse( Hibernate.isInitialized( section.getCourse()
					.getCode()
					.getDepartment() ) );
			Assertions.assertTrue( Hibernate.isInitialized( section.getCourse() ) );
			Assertions.assertEquals( 1, statistics.getEntityFetchCount() );
		} );
	}

	@Test
	public void testBasicFetchProfileOperation(SessionFactoryScope sessions) {
		final SessionFactoryImplementor sessionFactory = sessions.getSessionFactory();

		Assertions.assertTrue( sessionFactory.containsFetchProfileDefinition( "enrollment.details" ),
				"fetch profile not parsed properly" );
		Assertions.assertTrue( sessionFactory.containsFetchProfileDefinition( "offering.details" ),
				"fetch profile not parsed properly" );
		Assertions.assertTrue( sessionFactory.containsFetchProfileDefinition( "course.details" ),
				"fetch profile not parsed properly" );

		sessions.inTransaction( (session) -> {
			session.enableFetchProfile( "enrollment.details" );
			Assertions.assertTrue( session.getLoadQueryInfluencers().hasEnabledFetchProfiles() );
			session.disableFetchProfile( "enrollment.details" );
			Assertions.assertFalse( session.getLoadQueryInfluencers().hasEnabledFetchProfiles() );
			try {
				session.enableFetchProfile( "never-gonna-get-it" );
				Assertions.fail( "expecting failure on undefined fetch-profile" );
			}
			catch (UnknownProfileException expected) {
			}
		} );
	}

	@Test
	public void testLoadManyToOneFetchProfile(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			session.enableFetchProfile( "enrollment.details" );
			Enrollment enrollment = session.find( Enrollment.class, 1 );
			// enrollment + (section + student)
			Assertions.assertEquals( 3, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertTrue( Hibernate.isInitialized( enrollment.getOffering() ) );
			Assertions.assertTrue( Hibernate.isInitialized( enrollment.getStudent() ) );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
		} );
	}

	@Test
	public void testCriteriaManyToOneFetchProfile(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			session.enableFetchProfile( "enrollment.details" );
			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<Enrollment> criteria = criteriaBuilder.createQuery( Enrollment.class );
			criteria.from( Enrollment.class );
			Enrollment enrollment = session.createQuery( criteria ).uniqueResult();
			// enrollment + (section + student)
			Assertions.assertEquals( 3, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertTrue( Hibernate.isInitialized( enrollment.getOffering() ) );
			Assertions.assertTrue( Hibernate.isInitialized( enrollment.getStudent() ) );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
		} );
	}

	@Test
	public void testLoadOneToManyFetchProfile(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			session.enableFetchProfile( "offering.details" );
			CourseOffering section = session.find( CourseOffering.class, 1 );
			// section + (enrollments + course)
			Assertions.assertEquals( 3, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertTrue( Hibernate.isInitialized( section.getEnrollments() ) );
		} );
	}

	@Test
	public void testLoadDeepFetchProfile(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			// enable both enrollment and offering detail profiles;
			// then loading the section/offering should fetch the enrollment
			// which in turn should fetch student (+ offering).
			session.enableFetchProfile( "offering.details" );
			session.enableFetchProfile( "enrollment.details" );
			CourseOffering section = session.find( CourseOffering.class, 1 );
			// section + (course + enrollments + (student))
			Assertions.assertEquals( 4, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertTrue( Hibernate.isInitialized( section.getEnrollments() ) );
		} );
	}

	@Test
	public void testLoadComponentDerefFetchProfile(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			session.enableFetchProfile( "course.details" );
			Course course = session.find( Course.class, 1 );
			// course + department
			Assertions.assertEquals( 2, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
			Assertions.assertTrue( Hibernate.isInitialized( course.getCode().getDepartment() ) );
		} );
	}

	@Test
	public void testHQL(SessionFactoryScope sessions) {
		var statistics = sessions.getSessionFactory().getStatistics();
		statistics.clear();

		sessions.inTransaction( (session) -> {
			session.enableFetchProfile( "offering.details" );
			session.enableFetchProfile( "enrollment.details" );
			List<?> sections = session.createQuery( "from CourseOffering" ).list();
			int sectionCount = sections.size();
			Assertions.assertEquals( 1, sectionCount, "unexpected CourseOffering count" );
			Assertions.assertEquals( 4, statistics.getEntityLoadCount() );
			Assertions.assertEquals( 0, statistics.getEntityFetchCount() );
		} );
	}
}
