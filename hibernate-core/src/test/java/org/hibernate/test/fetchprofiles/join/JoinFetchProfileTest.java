/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.fetchprofiles.join;

import java.util.List;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.UnknownProfileException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Various tests related to join-style fetch profiles.
 *
 * @author Steve Ebersole
 */
public class JoinFetchProfileTest extends BaseCoreFunctionalTestCase {
	public String[] getMappings() {
		return new String[] { "fetchprofiles/join/Mappings.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public void configure(Configuration cfg) {
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@SuppressWarnings({ "UnusedDeclaration" })
	private interface TestData {
		Long getStudentId();

		Long getDepartmentId();

		Long getCourseId();

		Long getSectionId();

		Long getEnrollmentId();
	}

	private interface TestCode {
		void perform(TestData data);
	}

	@SuppressWarnings({ "unchecked" })
	private void performWithStandardData(TestCode testCode) {
		final Department literatureDepartment = new Department( "lit", "Literature" );
		final Student me = new Student( "Steve" );
		final Course lit101 = new Course( new Course.Code( literatureDepartment, 101 ), "Introduction to Literature" );
		final CourseOffering section = new CourseOffering( lit101, 1, 2008 );
		final Enrollment enrollment = new Enrollment( section, me );
		inTransaction(
				session -> {
					session.save( literatureDepartment );
					session.save( lit101 );
					session.save( section );
					session.save( me );
					section.getEnrollments().add( enrollment );
					session.save( enrollment );
				}
		);

		sessionFactory().getStatistics().clear();

		testCode.perform(
				new TestData() {
					public Long getStudentId() {
						return me.getId();
					}

					public Long getDepartmentId() {
						return literatureDepartment.getId();
					}

					public Long getCourseId() {
						return lit101.getId();
					}

					public Long getSectionId() {
						return section.getId();
					}

					public Long getEnrollmentId() {
						return enrollment.getId();
					}
				}
		);

		inTransaction(
				session -> {
					session.delete( enrollment );
					session.delete( me );
					session.delete( enrollment.getOffering() );
					session.delete( enrollment.getOffering().getCourse() );
					session.delete( enrollment.getOffering().getCourse().getCode().getDepartment() );
				}
		);
	}

	@Test
	public void testNormalLoading() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									CourseOffering section = session.get( CourseOffering.class, data.getSectionId() );
									assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertFalse( Hibernate.isInitialized( section.getCourse() ) );
									assertFalse( Hibernate.isInitialized( section.getEnrollments() ) );
									assertFalse( Hibernate.isInitialized( section.getCourse()
																				  .getCode()
																				  .getDepartment() ) );
									assertTrue( Hibernate.isInitialized( section.getCourse() ) );
									assertEquals( 1, sessionFactory().getStatistics().getEntityFetchCount() );
								}
						)

		);
	}

	@Test
	public void testNormalCriteria() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
									CriteriaQuery<CourseOffering> criteria = criteriaBuilder.createQuery( CourseOffering.class );
									criteria.from( CourseOffering.class );
									CourseOffering section = session.createQuery( criteria ).uniqueResult();
//						CourseOffering section = ( CourseOffering ) session.createCriteria( CourseOffering.class ).uniqueResult();
									assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertFalse( Hibernate.isInitialized( section.getCourse() ) );
									assertFalse( Hibernate.isInitialized( section.getEnrollments() ) );
									assertFalse( Hibernate.isInitialized( section.getCourse()
																				  .getCode()
																				  .getDepartment() ) );
									assertTrue( Hibernate.isInitialized( section.getCourse() ) );
									assertEquals( 1, sessionFactory().getStatistics().getEntityFetchCount() );
								}
						)
		);
	}

	@Test
	public void testBasicFetchProfileOperation() {
		assertTrue(
				"fetch profile not parsed properly",
				sessionFactory().containsFetchProfileDefinition( "enrollment.details" )
		);
		assertTrue(
				"fetch profile not parsed properly",
				sessionFactory().containsFetchProfileDefinition( "offering.details" )
		);
		assertTrue(
				"fetch profile not parsed properly",
				sessionFactory().containsFetchProfileDefinition( "course.details" )
		);
		Session s = openSession();
		SessionImplementor si = (SessionImplementor) s;
		s.enableFetchProfile( "enrollment.details" );
		assertTrue( si.getLoadQueryInfluencers().hasEnabledFetchProfiles() );
		s.disableFetchProfile( "enrollment.details" );
		assertFalse( si.getLoadQueryInfluencers().hasEnabledFetchProfiles() );
		try {
			s.enableFetchProfile( "never-gonna-get-it" );
			fail( "expecting failure on undefined fetch-profile" );
		}
		catch (UnknownProfileException expected) {
		}
		s.close();
	}

	@Test
	public void testLoadManyToOneFetchProfile() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									session.enableFetchProfile( "enrollment.details" );
									Enrollment enrollment = session.get(
											Enrollment.class,
											data.getEnrollmentId()
									);
									assertEquals(
											3,
											sessionFactory().getStatistics().getEntityLoadCount()
									); // enrollment + (section + student)
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertTrue( Hibernate.isInitialized( enrollment.getOffering() ) );
									assertTrue( Hibernate.isInitialized( enrollment.getStudent() ) );
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
								}
						)
		);
	}

	@Test
	public void testCriteriaManyToOneFetchProfile() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									session.enableFetchProfile( "enrollment.details" );
									CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
									CriteriaQuery<Enrollment> criteria = criteriaBuilder.createQuery( Enrollment.class );
									criteria.from( Enrollment.class );
									Enrollment enrollment = session.createQuery( criteria ).uniqueResult();
//								Enrollment enrollment = ( Enrollment ) session.createCriteria( Enrollment.class ).uniqueResult();
									assertEquals(
											3,
											sessionFactory().getStatistics().getEntityLoadCount()
									); // enrollment + (section + student)
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertTrue( Hibernate.isInitialized( enrollment.getOffering() ) );
									assertTrue( Hibernate.isInitialized( enrollment.getStudent() ) );
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );

								}
						)

		);
	}

	@Test
	public void testLoadOneToManyFetchProfile() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									session.enableFetchProfile( "offering.details" );
									CourseOffering section = session.get(
											CourseOffering.class,
											data.getSectionId()
									);
									assertEquals(
											3,
											sessionFactory().getStatistics().getEntityLoadCount()
									); // section + (enrollments + course)
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertTrue( Hibernate.isInitialized( section.getEnrollments() ) );
								}
						)

		);
	}

	@Test
	public void testLoadDeepFetchProfile() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									// enable both enrollment and offering detail profiles;
									// then loading the section/offering should fetch the enrollment
									// which in turn should fetch student (+ offering).
									session.enableFetchProfile( "offering.details" );
									session.enableFetchProfile( "enrollment.details" );
									CourseOffering section = session.get(
											CourseOffering.class,
											data.getSectionId()
									);
									assertEquals(
											4,
											sessionFactory().getStatistics().getEntityLoadCount()
									); // section + (course + enrollments + (student))
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertTrue( Hibernate.isInitialized( section.getEnrollments() ) );
								}
						)

		);
	}

	@Test
	public void testLoadComponentDerefFetchProfile() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									session.enableFetchProfile( "course.details" );
									Course course = session.get( Course.class, data.getCourseId() );
									assertEquals(
											2,
											sessionFactory().getStatistics().getEntityLoadCount()
									); // course + department
									assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
									assertTrue( Hibernate.isInitialized( course.getCode().getDepartment() ) );
								}
						)

		);
	}

	@Test
	public void testHQL() {
		performWithStandardData(
				data ->
						inTransaction(
								session -> {
									session.enableFetchProfile( "offering.details" );
									session.enableFetchProfile( "enrollment.details" );
									List sections = session.createQuery( "from CourseOffering" ).list();
									int sectionCount = sections.size();
									assertEquals( "unexpected CourseOffering count", 1, sectionCount );
									assertEquals( 4, sessionFactory().getStatistics().getEntityLoadCount() );
									assertEquals( 2, sessionFactory().getStatistics().getEntityFetchCount() );
								}
						)

		);
	}
}
