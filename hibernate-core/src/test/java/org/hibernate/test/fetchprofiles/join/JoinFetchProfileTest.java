/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.fetchprofiles.join;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.UnknownProfileException;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * Various tests related to join-style fetch profiles.
 *
 * @author Steve Ebersole
 */
@FailureExpectedWithNewUnifiedXsd(message = "appears using component.field as the association path is failing")
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

	@SuppressWarnings( {"UnusedDeclaration"})
	private static interface TestData {
		public Long getStudentId();
		public Long getDepartmentId();
		public Long getCourseId();
		public Long getSectionId();
		public Long getEnrollmentId();
	}

	private interface TestCode {
		public void perform(TestData data);
	}

	@SuppressWarnings( {"unchecked"})
	private void performWithStandardData(TestCode testCode) {
		Session session = openSession();
		session.beginTransaction();
		final Department literatureDepartment = new Department( "lit", "Literature" );
		session.save( literatureDepartment );
		final Course lit101 = new Course( new Course.Code( literatureDepartment, 101 ), "Introduction to Literature" );
		session.save( lit101 );
		final CourseOffering section = new CourseOffering( lit101, 1, 2008 );
		session.save( section );
		final Student me = new Student( "Steve" );
		session.save( me );
		final Enrollment enrollment = new Enrollment( section, me );
		section.getEnrollments().add( enrollment );
		session.save( enrollment );
		session.getTransaction().commit();
		session.close();

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

		session = openSession();
		session.beginTransaction();
		session.delete( enrollment );
		session.delete( me );
		session.delete( enrollment.getOffering() );
		session.delete( enrollment.getOffering().getCourse() );
		session.delete( enrollment.getOffering().getCourse().getCode().getDepartment() );
		session.getTransaction().commit();
		session.close();
	}

	@Test
	public void testNormalLoading() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						CourseOffering section = ( CourseOffering ) session.get( CourseOffering.class, data.getSectionId() );
						assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertFalse( Hibernate.isInitialized( section.getCourse() ) );
						assertFalse( Hibernate.isInitialized( section.getEnrollments() ) );
						assertFalse( Hibernate.isInitialized( section.getCourse().getCode().getDepartment() ) );
						assertTrue( Hibernate.isInitialized( section.getCourse() ) );
						assertEquals( 1, sessionFactory().getStatistics().getEntityFetchCount() );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	@Test
	public void testNormalCriteria() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						CourseOffering section = ( CourseOffering ) session.createCriteria( CourseOffering.class ).uniqueResult();
						assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertFalse( Hibernate.isInitialized( section.getCourse() ) );
						assertFalse( Hibernate.isInitialized( section.getEnrollments() ) );
						assertFalse( Hibernate.isInitialized( section.getCourse().getCode().getDepartment() ) );
						assertTrue( Hibernate.isInitialized( section.getCourse() ) );
						assertEquals( 1, sessionFactory().getStatistics().getEntityFetchCount() );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	@Test
	public void testBasicFetchProfileOperation() {
		assertTrue( "fetch profile not parsed properly", sessionFactory().containsFetchProfileDefinition( "enrollment.details" ) );
		assertTrue( "fetch profile not parsed properly", sessionFactory().containsFetchProfileDefinition( "offering.details" ) );
		assertTrue( "fetch profile not parsed properly", sessionFactory().containsFetchProfileDefinition( "course.details" ) );
		Session s = openSession();
		SessionImplementor si = ( SessionImplementor ) s;
		s.enableFetchProfile( "enrollment.details" );
		assertTrue( si.getLoadQueryInfluencers().hasEnabledFetchProfiles() );
		s.disableFetchProfile( "enrollment.details" );
		assertFalse( si.getLoadQueryInfluencers().hasEnabledFetchProfiles() );
		try {
			s.enableFetchProfile( "never-gonna-get-it" );
			fail( "expecting failure on undefined fetch-profile" );
		}
		catch ( UnknownProfileException expected ) {
		}
		s.close();
	}

	@Test
	public void testLoadManyToOneFetchProfile() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						session.enableFetchProfile( "enrollment.details" );
						Enrollment enrollment = ( Enrollment ) session.get( Enrollment.class, data.getEnrollmentId() );
						assertEquals( 3, sessionFactory().getStatistics().getEntityLoadCount() ); // enrollment + (section + student)
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertTrue( Hibernate.isInitialized( enrollment.getOffering() ) );
						assertTrue( Hibernate.isInitialized( enrollment.getStudent() ) );
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	@Test
	public void testCriteriaManyToOneFetchProfile() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						session.enableFetchProfile( "enrollment.details" );
						Enrollment enrollment = ( Enrollment ) session.createCriteria( Enrollment.class ).uniqueResult();
						assertEquals( 3, sessionFactory().getStatistics().getEntityLoadCount() ); // enrollment + (section + student)
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertTrue( Hibernate.isInitialized( enrollment.getOffering() ) );
						assertTrue( Hibernate.isInitialized( enrollment.getStudent() ) );
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	@Test
	public void testLoadOneToManyFetchProfile() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						session.enableFetchProfile( "offering.details" );
						CourseOffering section = ( CourseOffering ) session.get( CourseOffering.class, data.getSectionId() );
						assertEquals( 3, sessionFactory().getStatistics().getEntityLoadCount() ); // section + (enrollments + course)
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertTrue( Hibernate.isInitialized( section.getEnrollments() ) );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	@Test
	public void testLoadDeepFetchProfile() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						// enable both enrollment and offering detail profiles;
						// then loading the section/offering should fetch the enrollment
						// which in turn should fetch student (+ offering).
						session.enableFetchProfile( "offering.details" );
						session.enableFetchProfile( "enrollment.details" );
						CourseOffering section = ( CourseOffering ) session.get( CourseOffering.class, data.getSectionId() );
						assertEquals( 4, sessionFactory().getStatistics().getEntityLoadCount() ); // section + (course + enrollments + (student))
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertTrue( Hibernate.isInitialized( section.getEnrollments() ) );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	@Test
	public void testLoadComponentDerefFetchProfile() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						session.enableFetchProfile( "course.details" );
						Course course = ( Course ) session.get( Course.class, data.getCourseId() );
						assertEquals( 2, sessionFactory().getStatistics().getEntityLoadCount() ); // course + department
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						assertTrue( Hibernate.isInitialized( course.getCode().getDepartment() ) );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}

	/**
	 * fetch-profiles should have no effect what-so-ever on the direct results of the HQL query.
	 *
	 * TODO : this is actually not strictly true.  what we should have happen is to subsequently load those fetches
	 */
	@Test
	public void testHQL() {
		performWithStandardData(
				new TestCode() {
					public void perform(TestData data) {
						Session session = openSession();
						session.beginTransaction();
						session.enableFetchProfile( "offering.details" );
						session.enableFetchProfile( "enrollment.details" );
						List sections = session.createQuery( "from CourseOffering" ).list();
						int sectionCount = sections.size();
						assertEquals( "unexpected CourseOffering count", 1, sectionCount );
						assertEquals( 1, sessionFactory().getStatistics().getEntityLoadCount() );
						assertEquals( 0, sessionFactory().getStatistics().getEntityFetchCount() );
						session.getTransaction().commit();
						session.close();
					}
				}
		);
	}
}
