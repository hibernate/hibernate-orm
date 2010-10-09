//$Id: CriteriaQueryTest.java 10976 2006-12-12 23:22:26Z steve.ebersole@jboss.com $
package org.hibernate.test.readonly;

import java.util.List;

import junit.framework.Test;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.util.SerializationHelper;

/**
 * @author Gail Badner (adapted from org.hibernate.test.criteria.CriteriaQueryTest by Gavin King)
 */
public class ReadOnlyCriteriaQueryTest extends AbstractReadOnlyTest {
	
	public ReadOnlyCriteriaQueryTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "readonly/Enrolment.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "criteriaquerytest" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}
	
	public static Test suite() {
		return new FunctionalTestClassTestSuite( ReadOnlyCriteriaQueryTest.class );
	}

	public void testModifiableSessionDefaultCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		assertInsertCount( 4 );
		assertUpdateCount( 0 );
		clearCounts();

		s = openSession();
		t = s.beginTransaction();
		Criteria criteria = s.createCriteria( Student.class );
		assertFalse( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		assertFalse( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), false );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), false );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();

		assertUpdateCount( 1 );
		assertDeleteCount( 4 );
	}

	public void testModifiableSessionReadOnlyCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( true );
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testModifiableSessionModifiableCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Criteria criteria = s.createCriteria( Student.class );
		assertFalse( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		criteria.setReadOnly( false );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		assertFalse( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), false );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), false );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testReadOnlySessionDefaultCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Criteria criteria = s.createCriteria( Student.class );
		assertTrue( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertTrue( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertTrue( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testReadOnlySessionReadOnlyCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Criteria criteria = s.createCriteria( Student.class );
		assertTrue( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		criteria.setReadOnly( true );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertTrue( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertTrue( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testReadOnlySessionModifiableCriteria() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.setDefaultReadOnly( true );
		Criteria criteria = s.createCriteria( Student.class );
		assertTrue( s.isDefaultReadOnly() );
		assertFalse( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		criteria.setReadOnly( false );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertTrue( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		assertFalse( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), false );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), false);
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertTrue( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testReadOnlyCriteriaReturnsModifiableExistingEntity() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		coursePreferred = ( Course ) s.get( Course.class, coursePreferred.getCourseCode() );
		assertFalse( s.isReadOnly( coursePreferred ) );
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( true );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertFalse( s.isReadOnly( coursePreferred ) );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testReadOnlyCriteriaReturnsExistingModifiableProxyNotInit() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		coursePreferred = ( Course ) s.load( Course.class, coursePreferred.getCourseCode() );
		assertFalse( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, false );
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( true );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, false );
		Hibernate.initialize( coursePreferred );
		checkProxyReadOnly( s, coursePreferred, false );		
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testReadOnlyCriteriaReturnsExistingModifiableProxyInit() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		coursePreferred = ( Course ) s.load( Course.class, coursePreferred.getCourseCode() );
		assertFalse( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, false );
		Hibernate.initialize( coursePreferred );
		checkProxyReadOnly( s, coursePreferred, false );
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( true );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertTrue( criteria.isReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertTrue( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, false );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testModifiableCriteriaReturnsExistingReadOnlyEntity() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		coursePreferred = ( Course ) s.get( Course.class, coursePreferred.getCourseCode() );
		assertFalse( s.isReadOnly( coursePreferred ) );
		s.setReadOnly( coursePreferred, true );
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( false );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		assertFalse( s.isReadOnly( gavin ) );
		assertTrue( s.isReadOnly( coursePreferred ) );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testModifiableCriteriaReturnsExistingReadOnlyProxyNotInit() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		coursePreferred = ( Course ) s.load( Course.class, coursePreferred.getCourseCode() );
		assertFalse( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, false );
		s.setReadOnly( coursePreferred, true );
		checkProxyReadOnly( s, coursePreferred, true );
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( false );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		assertFalse( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, true );
		Hibernate.initialize( coursePreferred );
		checkProxyReadOnly( s, coursePreferred, true );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}

	public void testModifiableCriteriaReturnsExistingReadOnlyProxyInit() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);

		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist(  coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add( enrolment );
		s.persist( enrolment );

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		assertFalse( s.isDefaultReadOnly() );
		coursePreferred = ( Course ) s.load( Course.class, coursePreferred.getCourseCode() );
		assertFalse( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, false );
		Hibernate.initialize( coursePreferred );
		checkProxyReadOnly( s, coursePreferred, false );
		s.setReadOnly( coursePreferred, true );
		checkProxyReadOnly( s, coursePreferred, true );
		Criteria criteria = s.createCriteria( Student.class ).setReadOnly( false );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		gavin = ( Student ) criteria.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( criteria.isReadOnlyInitialized() );
		assertFalse( criteria.isReadOnly() );
		assertFalse( s.isReadOnly( gavin ) );
		assertTrue( Hibernate.isInitialized( coursePreferred ) );
		checkProxyReadOnly( s, coursePreferred, true );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();
	}
	
	public void testScrollCriteria() {
		Session session = openSession();
		Transaction t = session.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		session.persist(course);
		session.flush();
		session.clear();
		ScrollableResults sr = session.createCriteria(Course.class).setReadOnly( true ).scroll();
		assertTrue( sr.next() );
		course = (Course) sr.get(0);
		assertNotNull(course);
		assertTrue( session.isReadOnly( course ) );
		sr.close();
		session.delete(course);
		
		t.commit();
		session.close();
		
	}
	
	public void testSubselect() {

		Session s = openSession();
		Transaction t = s.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.persist(course);
		
		Course coursePreferred = new Course();
		coursePreferred.setCourseCode( "JBOSS" );
		coursePreferred.setDescription( "JBoss" );
		s.persist( coursePreferred );

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse( coursePreferred );
		s.persist(gavin);

		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add(enrolment);
		s.persist(enrolment);

		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		DetachedCriteria dc = DetachedCriteria.forClass(Student.class)
			.add( Property.forName("studentNumber").eq( new Long(232) ) )
			.setProjection( Property.forName("name") );
		gavin = ( Student ) s.createCriteria(Student.class)
			.add( Subqueries.exists(dc) )
			.setReadOnly( true )
			.uniqueResult();
		assertFalse( s.isDefaultReadOnly() );
		assertTrue( s.isReadOnly( gavin ) );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		Hibernate.initialize( gavin.getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( gavin.getPreferredCourse() ) );
		checkProxyReadOnly( s, gavin.getPreferredCourse(), true );
		assertFalse( Hibernate.isInitialized( gavin.getEnrolments() ) );
		Hibernate.initialize( gavin.getEnrolments() );
		assertTrue( Hibernate.isInitialized( gavin.getEnrolments() ) );
		assertEquals( 1, gavin.getEnrolments().size() );
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		assertFalse( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		Hibernate.initialize( enrolment.getCourse() );
		checkProxyReadOnly( s, enrolment.getCourse(), false );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		DetachedCriteria dc2 = DetachedCriteria.forClass(Student.class, "st")
			.add( Property.forName("st.studentNumber").eqProperty("e.studentNumber") )
			.setProjection( Property.forName("name") );
		enrolment = ( Enrolment ) s.createCriteria(Enrolment.class, "e")
			.add( Subqueries.eq("Gavin King", dc2) )
			.setReadOnly( true )
			.uniqueResult();
		assertTrue( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		Hibernate.initialize( enrolment.getCourse() );
		assertTrue( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		assertFalse( Hibernate.isInitialized( enrolment.getStudent() ) );
		checkProxyReadOnly( s, enrolment.getStudent(), true );
		Hibernate.initialize( enrolment.getStudent() );
		assertTrue( Hibernate.isInitialized( enrolment.getStudent() ) );
		checkProxyReadOnly( s, enrolment.getStudent(), true );
		assertFalse( Hibernate.isInitialized( enrolment.getStudent().getPreferredCourse() ) );
		checkProxyReadOnly( s, enrolment.getStudent().getPreferredCourse(), false );
		Hibernate.initialize( enrolment.getStudent().getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( enrolment.getStudent().getPreferredCourse() ) );
		checkProxyReadOnly( s, enrolment.getStudent().getPreferredCourse(), false );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		DetachedCriteria dc3 = DetachedCriteria.forClass(Student.class, "st")
			.createCriteria("enrolments")
				.createCriteria("course")
					.add( Property.forName("description").eq("Hibernate Training") )
					.setProjection( Property.forName("st.name") );
		enrolment = ( Enrolment ) s.createCriteria(Enrolment.class, "e")
			.add( Subqueries.eq("Gavin King", dc3) )
			.setReadOnly( true )
			.uniqueResult();
		assertTrue( s.isReadOnly( enrolment ) );
		assertFalse( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		Hibernate.initialize( enrolment.getCourse() );
		assertTrue( Hibernate.isInitialized( enrolment.getCourse() ) );
		checkProxyReadOnly( s, enrolment.getCourse(), true );
		assertFalse( Hibernate.isInitialized( enrolment.getStudent() ) );
		checkProxyReadOnly( s, enrolment.getStudent(), true );
		Hibernate.initialize( enrolment.getStudent() );
		assertTrue( Hibernate.isInitialized( enrolment.getStudent() ) );
		checkProxyReadOnly( s, enrolment.getStudent(), true );
		assertFalse( Hibernate.isInitialized( enrolment.getStudent().getPreferredCourse() ) );
		checkProxyReadOnly( s, enrolment.getStudent().getPreferredCourse(), false );
		Hibernate.initialize( enrolment.getStudent().getPreferredCourse() );
		assertTrue( Hibernate.isInitialized( enrolment.getStudent().getPreferredCourse() ) );
		checkProxyReadOnly( s, enrolment.getStudent().getPreferredCourse(), false );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.delete(gavin.getPreferredCourse());
		s.delete(gavin);
		enrolment = ( Enrolment ) gavin.getEnrolments().iterator().next();
		s.delete( enrolment.getCourse() );
		s.delete(enrolment);
		t.commit();
		s.close();		
	}
	
	public void testDetachedCriteria() {
		
		DetachedCriteria dc = DetachedCriteria.forClass(Student.class)
			.add( Property.forName("name").eq("Gavin King") )
			.addOrder( Order.asc("studentNumber") );
		
		byte[] bytes = SerializationHelper.serialize(dc);
		
		dc = (DetachedCriteria) SerializationHelper.deserialize(bytes);
		
		Session session = openSession();
		Transaction t = session.beginTransaction();

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		Student bizarroGavin = new Student();
		bizarroGavin.setName("Gavin King");
		bizarroGavin.setStudentNumber(666);
		session.persist(bizarroGavin);
		session.persist(gavin);

		t.commit();
		session.close();

		session = openSession();
		t = session.beginTransaction();

		List result = dc.getExecutableCriteria(session)
			.setMaxResults(3)
			.setReadOnly( true )
			.list();

		assertEquals( result.size(), 2 );
		gavin = ( Student ) result.get( 0 );
		bizarroGavin = ( Student ) result.get( 1 );
		assertEquals( 232, gavin.getStudentNumber() );
		assertEquals( 666, bizarroGavin.getStudentNumber() );
		assertTrue( session.isReadOnly( gavin ) );
		assertTrue( session.isReadOnly( bizarroGavin ) );

		session.delete(gavin);
		session.delete(bizarroGavin);
		t.commit();
		session.close();
	}
	
		public void testTwoAliasesCache() {
			Session s = openSession();
			Transaction t = s.beginTransaction();
			
			Course course = new Course();
			course.setCourseCode("HIB");
			course.setDescription("Hibernate Training");
			s.save(course);
			
			Student gavin = new Student();
			gavin.setName("Gavin King");
			gavin.setStudentNumber(666);
			s.save(gavin);
			
			Student xam = new Student();
			xam.setName("Max Rydahl Andersen");
			xam.setStudentNumber(101);
			s.save(xam);
			
			Enrolment enrolment1 = new Enrolment();
			enrolment1.setCourse(course);
			enrolment1.setCourseCode(course.getCourseCode());
			enrolment1.setSemester((short) 1);
			enrolment1.setYear((short) 1999);
			enrolment1.setStudent(xam);
			enrolment1.setStudentNumber(xam.getStudentNumber());
			xam.getEnrolments().add(enrolment1);
			s.save(enrolment1);
			
			Enrolment enrolment2 = new Enrolment();
			enrolment2.setCourse(course);
			enrolment2.setCourseCode(course.getCourseCode());
			enrolment2.setSemester((short) 3);
			enrolment2.setYear((short) 1998);
			enrolment2.setStudent(gavin);
			enrolment2.setStudentNumber(gavin.getStudentNumber());
			gavin.getEnrolments().add(enrolment2);
			s.save(enrolment2);
			t.commit();
			s.close();

			s = openSession();
			t = s.beginTransaction();
			
			List list = s.createCriteria(Enrolment.class)
				.createAlias("student", "s")
				.createAlias("course", "c")
				.add( Restrictions.isNotEmpty("s.enrolments") )
				.setCacheable(true)
				.setReadOnly( true )
				.list();
			
			assertEquals( list.size(), 2 );

			Enrolment e = ( Enrolment ) list.get( 0 );
			if ( e.getStudent().getStudentNumber() == xam.getStudentNumber() ) {
				enrolment1 = e;
				enrolment2 = ( Enrolment ) list.get( 1 );
			}
			else if ( e.getStudent().getStudentNumber() == xam.getStudentNumber() ) {
				enrolment2 = e;
				enrolment1 = ( Enrolment ) list.get( 1 );
			}
			else {
				fail( "Enrolment has unknown student number: " + e.getStudent().getStudentNumber() );
			}

			assertTrue( s.isReadOnly( enrolment1 ) );
			assertTrue( s.isReadOnly( enrolment2 ) );
			assertTrue( s.isReadOnly( enrolment1.getCourse() ) );
			assertTrue( s.isReadOnly( enrolment2.getCourse() ) );
			assertSame( enrolment1.getCourse(), enrolment2.getCourse() );
			assertTrue( s.isReadOnly( enrolment1.getStudent() ) );
			assertTrue( s.isReadOnly( enrolment2.getStudent() ) );

			t.commit();
			s.close();
	
			s = openSession();
			t = s.beginTransaction();
			
			list = s.createCriteria(Enrolment.class)
				.createAlias("student", "s")
				.createAlias("course", "c")
				.setReadOnly( true )
				.add( Restrictions.isNotEmpty("s.enrolments") )
				.setCacheable(true)
				.setReadOnly( true )
				.list();
		
			assertEquals( list.size(), 2 );

			e = ( Enrolment ) list.get( 0 );
			if ( e.getStudent().getStudentNumber() == xam.getStudentNumber() ) {
				enrolment1 = e;
				enrolment2 = ( Enrolment ) list.get( 1 );
			}
			else if ( e.getStudent().getStudentNumber() == xam.getStudentNumber() ) {
				enrolment2 = e;
				enrolment1 = ( Enrolment ) list.get( 1 );
			}
			else {
				fail( "Enrolment has unknown student number: " + e.getStudent().getStudentNumber() );
			}

			assertTrue( s.isReadOnly( enrolment1 ) );
			assertTrue( s.isReadOnly( enrolment2 ) );
			assertTrue( s.isReadOnly( enrolment1.getCourse() ) );
			assertTrue( s.isReadOnly( enrolment2.getCourse() ) );
			assertSame( enrolment1.getCourse(), enrolment2.getCourse() );
			assertTrue( s.isReadOnly( enrolment1.getStudent() ) );
			assertTrue( s.isReadOnly( enrolment2.getStudent() ) );

			t.commit();
			s.close();
	
			s = openSession();
			t = s.beginTransaction();
			
			list = s.createCriteria(Enrolment.class)
				.setReadOnly( true )
				.createAlias("student", "s")
				.createAlias("course", "c")
				.add( Restrictions.isNotEmpty("s.enrolments") )
				.setCacheable(true)
				.list();
			
			assertEquals( list.size(), 2 );

			e = ( Enrolment ) list.get( 0 );
			if ( e.getStudent().getStudentNumber() == xam.getStudentNumber() ) {
				enrolment1 = e;
				enrolment2 = ( Enrolment ) list.get( 1 );
			}
			else if ( e.getStudent().getStudentNumber() == xam.getStudentNumber() ) {
				enrolment2 = e;
				enrolment1 = ( Enrolment ) list.get( 1 );
			}
			else {
				fail( "Enrolment has unknown student number: " + e.getStudent().getStudentNumber() );
			}

			assertTrue( s.isReadOnly( enrolment1 ) );
			assertTrue( s.isReadOnly( enrolment2 ) );
			assertTrue( s.isReadOnly( enrolment1.getCourse() ) );
			assertTrue( s.isReadOnly( enrolment2.getCourse() ) );
			assertSame( enrolment1.getCourse(), enrolment2.getCourse() );
			assertTrue( s.isReadOnly( enrolment1.getStudent() ) );
			assertTrue( s.isReadOnly( enrolment2.getStudent() ) );

			s.delete( enrolment1 );
			s.delete( enrolment2 );
			s.delete( enrolment1.getCourse() );
			s.delete( enrolment1.getStudent() );
			s.delete( enrolment2.getStudent() );
		
			t.commit();
			s.close();
	}

	/*
	public void testProjectionsUsingProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		
		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		s.save(course);
		
		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(667);
		s.save(gavin);
		
		Student xam = new Student();
		xam.setName("Max Rydahl Andersen");
		xam.setStudentNumber(101);
		s.save(xam);
		
		Enrolment enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 1);
		enrolment.setYear((short) 1999);
		enrolment.setStudent(xam);
		enrolment.setStudentNumber(xam.getStudentNumber());
		xam.getEnrolments().add(enrolment);
		s.save(enrolment);
		
		enrolment = new Enrolment();
		enrolment.setCourse(course);
		enrolment.setCourseCode(course.getCourseCode());
		enrolment.setSemester((short) 3);
		enrolment.setYear((short) 1998);
		enrolment.setStudent(gavin);
		enrolment.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add(enrolment);
		s.save(enrolment);
		
		s.flush();
		
		Long count = (Long) s.createCriteria(Enrolment.class)
			.setProjection( Property.forName("studentNumber").count().setDistinct() )
			.uniqueResult();
		assertEquals(count, new Long(2));
		
		Object object = s.createCriteria(Enrolment.class)
			.setProjection( Projections.projectionList()
					.add( Property.forName("studentNumber").count() )
					.add( Property.forName("studentNumber").max() )
					.add( Property.forName("studentNumber").min() )
					.add( Property.forName("studentNumber").avg() )
			)
			.uniqueResult();
		Object[] result = (Object[])object; 
		
		assertEquals(new Long(2),result[0]);
		assertEquals(new Long(667),result[1]);
		assertEquals(new Long(101),result[2]);
		assertEquals(384.0, ( (Double) result[3] ).doubleValue(), 0.01);
		
		
		s.createCriteria(Enrolment.class)
		    .add( Property.forName("studentNumber").gt( new Long(665) ) )
		    .add( Property.forName("studentNumber").lt( new Long(668) ) )
		    .add( Property.forName("courseCode").like("HIB", MatchMode.START) )
		    .add( Property.forName("year").eq( new Short( (short) 1999 ) ) )
		    .addOrder( Property.forName("studentNumber").asc() )
			.uniqueResult();
	
		List resultWithMaps = s.createCriteria(Enrolment.class)
			.setProjection( Projections.projectionList()
					.add( Property.forName("studentNumber").as("stNumber") )
					.add( Property.forName("courseCode").as("cCode") )
			)
		    .add( Property.forName("studentNumber").gt( new Long(665) ) )
		    .add( Property.forName("studentNumber").lt( new Long(668) ) )
		    .addOrder( Property.forName("studentNumber").asc() )
			.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
			.list();
		
		assertEquals(1, resultWithMaps.size());
		Map m1 = (Map) resultWithMaps.get(0);
		
		assertEquals(new Long(667), m1.get("stNumber"));
		assertEquals(course.getCourseCode(), m1.get("cCode"));		

		resultWithMaps = s.createCriteria(Enrolment.class)
			.setProjection( Property.forName("studentNumber").as("stNumber") )
		    .addOrder( Order.desc("stNumber") )
			.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
			.list();
		
		assertEquals(2, resultWithMaps.size());
		Map m0 = (Map) resultWithMaps.get(0);
		m1 = (Map) resultWithMaps.get(1);
		
		assertEquals(new Long(101), m1.get("stNumber"));
		assertEquals(new Long(667), m0.get("stNumber"));

	
		List resultWithAliasedBean = s.createCriteria(Enrolment.class)
			.createAlias("student", "st")
			.createAlias("course", "co")
			.setProjection( Projections.projectionList()
					.add( Property.forName("st.name").as("studentName") )
					.add( Property.forName("co.description").as("courseDescription") )
			)
			.addOrder( Order.desc("studentName") )
			.setResultTransformer( Transformers.aliasToBean(StudentDTO.class) )
			.list();
		
		assertEquals(2, resultWithAliasedBean.size());
		
		StudentDTO dto = (StudentDTO) resultWithAliasedBean.get(0);
		assertNotNull(dto.getDescription());
		assertNotNull(dto.getName());
	
		s.createCriteria(Student.class)
			.add( Restrictions.like("name", "Gavin", MatchMode.START) )
			.addOrder( Order.asc("name") )
			.createCriteria("enrolments", "e")
				.addOrder( Order.desc("year") )
				.addOrder( Order.desc("semester") )
			.createCriteria("course","c")
				.addOrder( Order.asc("description") )
				.setProjection( Projections.projectionList()
					.add( Property.forName("this.name") )
					.add( Property.forName("e.year") )
					.add( Property.forName("e.semester") )
					.add( Property.forName("c.courseCode") )
					.add( Property.forName("c.description") )
				)
			.uniqueResult();
			
		Projection p1 = Projections.projectionList()
			.add( Property.forName("studentNumber").count() )
			.add( Property.forName("studentNumber").max() )
			.add( Projections.rowCount() );
		
		Projection p2 = Projections.projectionList()
			.add( Property.forName("studentNumber").min() )
			.add( Property.forName("studentNumber").avg() )
			.add( Projections.sqlProjection(
					"1 as constOne, count(*) as countStar", 
					new String[] { "constOne", "countStar" }, 
					new Type[] { Hibernate.INTEGER, Hibernate.INTEGER }
			) );
	
		Object[] array = (Object[]) s.createCriteria(Enrolment.class)
			.setProjection( Projections.projectionList().add(p1).add(p2) )
			.uniqueResult();
		
		assertEquals( array.length, 7 );
		
		List list = s.createCriteria(Enrolment.class)
			.createAlias("student", "st")
			.createAlias("course", "co")
			.setProjection( Projections.projectionList()
					.add( Property.forName("co.courseCode").group() )
					.add( Property.forName("st.studentNumber").count().setDistinct() )
					.add( Property.forName("year").group() )
			)
			.list();
		
		assertEquals( list.size(), 2 );
		
		s.delete(gavin);
		s.delete(xam);
		s.delete(course);
		
		t.commit();
		s.close();
	}

	public void testRestrictionOnSubclassCollection() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		s.createCriteria( Reptile.class )
				.add( Restrictions.isEmpty( "offspring" ) )
				.list();

		s.createCriteria( Reptile.class )
				.add( Restrictions.isNotEmpty( "offspring" ) )
				.list();

		t.rollback();
		s.close();
	}

	public void testClassProperty() {
		Session s = openSession();
		Transaction t = s.beginTransaction();

		// HQL: from Animal a where a.mother.class = Reptile
		Criteria c = s.createCriteria(Animal.class,"a")
			.createAlias("mother","m")
			.add( Property.forName("m.class").eq(Reptile.class) );
		c.list();
		t.rollback();
		s.close();
	}

	public void testProjectedId() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		s.createCriteria(Course.class).setProjection( Projections.property("courseCode") ).list();
		s.createCriteria(Course.class).setProjection( Projections.id() ).list();
		t.rollback();
		s.close();
	}

	public void testSubcriteriaJoinTypes() {
		Session session = openSession();
		Transaction t = session.beginTransaction();

		Course courseA = new Course();
		courseA.setCourseCode("HIB-A");
		courseA.setDescription("Hibernate Training A");
		session.persist(courseA);

		Course courseB = new Course();
		courseB.setCourseCode("HIB-B");
		courseB.setDescription("Hibernate Training B");
		session.persist(courseB);

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse(courseA);
		session.persist(gavin);

		Student leonardo = new Student();
		leonardo.setName("Leonardo Quijano");
		leonardo.setStudentNumber(233);
		leonardo.setPreferredCourse(courseB);
		session.persist(leonardo);

		Student johnDoe = new Student();
		johnDoe.setName("John Doe");
		johnDoe.setStudentNumber(235);
		johnDoe.setPreferredCourse(null);
		session.persist(johnDoe);

		List result = session.createCriteria( Student.class )
				.setProjection( Property.forName("preferredCourse.courseCode") )
				.createCriteria( "preferredCourse", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "courseCode" ) )
						.list();
		assertEquals( 3, result.size() );
		// can't be sure of NULL comparison ordering aside from they should
		// either come first or last
		if ( result.get( 0 ) == null ) {
			assertEquals( "HIB-A", result.get(1) );
			assertEquals( "HIB-B", result.get(2) );
		}
		else {
			assertNull( result.get(2) );
			assertEquals( "HIB-A", result.get(0) );
			assertEquals( "HIB-B", result.get(1) );
		}

		result = session.createCriteria( Student.class )
				.setFetchMode( "preferredCourse", FetchMode.JOIN )
				.createCriteria( "preferredCourse", Criteria.LEFT_JOIN )
						.addOrder( Order.asc( "courseCode" ) )
						.list();
		assertEquals( 3, result.size() );
		assertNotNull( result.get(0) );
		assertNotNull( result.get(1) );
		assertNotNull( result.get(2) );

		result = session.createCriteria( Student.class )
				.setFetchMode( "preferredCourse", FetchMode.JOIN )
				.createAlias( "preferredCourse", "pc", Criteria.LEFT_JOIN )
				.addOrder( Order.asc( "pc.courseCode" ) )
				.list();
		assertEquals( 3, result.size() );
		assertNotNull( result.get(0) );
		assertNotNull( result.get(1) );
		assertNotNull( result.get(2) );

		session.delete(gavin);
		session.delete(leonardo);
		session.delete(johnDoe);
		session.delete(courseA);
		session.delete(courseB);
		t.commit();
		session.close();
	}
	
	public void testAliasJoinCriterion() {
		Session session = openSession();
		Transaction t = session.beginTransaction();

		Course courseA = new Course();
		courseA.setCourseCode("HIB-A");
		courseA.setDescription("Hibernate Training A");
		session.persist(courseA);

		Course courseB = new Course();
		courseB.setCourseCode("HIB-B");
		courseB.setDescription("Hibernate Training B");
		session.persist(courseB);

		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		gavin.setPreferredCourse(courseA);
		session.persist(gavin);

		Student leonardo = new Student();
		leonardo.setName("Leonardo Quijano");
		leonardo.setStudentNumber(233);
		leonardo.setPreferredCourse(courseB);
		session.persist(leonardo);

		Student johnDoe = new Student();
		johnDoe.setName("John Doe");
		johnDoe.setStudentNumber(235);
		johnDoe.setPreferredCourse(null);
		session.persist(johnDoe);

		// test == on one value exists
		List result = session.createCriteria( Student.class )
			.createAlias( "preferredCourse", "pc", Criteria.LEFT_JOIN, Restrictions.eq("pc.courseCode", "HIB-A") )
			.setProjection( Property.forName("pc.courseCode") )
			.addOrder(Order.asc("pc.courseCode"))
			.list();
		
		assertEquals( 3, result.size() );
		
		// can't be sure of NULL comparison ordering aside from they should
		// either come first or last
		if ( result.get( 0 ) == null ) {
			assertNull(result.get(1));
			assertEquals( "HIB-A", result.get(2) );
		}
		else {
			assertNull( result.get(2) );
			assertNull( result.get(1) );
			assertEquals( "HIB-A", result.get(0) );
		}
		
		// test == on non existent value
		result = session.createCriteria( Student.class )
		.createAlias( "preferredCourse", "pc", Criteria.LEFT_JOIN, Restrictions.eq("pc.courseCode", "HIB-R") )
		.setProjection( Property.forName("pc.courseCode") )
		.addOrder(Order.asc("pc.courseCode"))
		.list();
	
		assertEquals( 3, result.size() );
		assertNull( result.get(2) );
		assertNull( result.get(1) );
		assertNull(result.get(0) );
		
		// test != on one existing value
		result = session.createCriteria( Student.class )
		.createAlias( "preferredCourse", "pc", Criteria.LEFT_JOIN, Restrictions.ne("pc.courseCode", "HIB-A") )
		.setProjection( Property.forName("pc.courseCode") )
		.addOrder(Order.asc("pc.courseCode"))
		.list();
	
		assertEquals( 3, result.size() );
		// can't be sure of NULL comparison ordering aside from they should
		// either come first or last
		if ( result.get( 0 ) == null ) {
			assertNull( result.get(1) );
			assertEquals( "HIB-B", result.get(2) );
		}
		else {
			assertEquals( "HIB-B", result.get(0) );
			assertNull( result.get(1) );
			assertNull( result.get(2) );
		}

		session.delete(gavin);
		session.delete(leonardo);
		session.delete(johnDoe);
		session.delete(courseA);
		session.delete(courseB);
		t.commit();
		session.close();
	}
	*/
	
	private void checkProxyReadOnly(Session s, Object proxy, boolean expectedReadOnly) {
		assertTrue( proxy instanceof HibernateProxy );
		LazyInitializer li = ( ( HibernateProxy ) proxy ).getHibernateLazyInitializer();
		assertSame( s, li.getSession() );
		assertEquals( expectedReadOnly, s.isReadOnly( proxy ) );
		assertEquals( expectedReadOnly, li.isReadOnly() );
		assertEquals( Hibernate.isInitialized( proxy ), ! li.isUninitialized() );
		if ( Hibernate.isInitialized( proxy ) ) {
			assertEquals( expectedReadOnly, s.isReadOnly( li.getImplementation() ) );
		}
	}

}

