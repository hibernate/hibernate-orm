/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.readonly;

import java.util.List;

import org.junit.Test;

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
import org.hibernate.dialect.SybaseASE15Dialect;
import org.hibernate.internal.util.SerializationHelper;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.testing.SkipForDialect;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * (adapted from org.hibernate.test.criteria.CriteriaQueryTest by Gavin King)
 *
 * @author Gail Badner
 */
public class ReadOnlyCriteriaQueryTest extends AbstractReadOnlyTest {
	@Override
	public String[] getMappings() {
		return new String[] { "readonly/Enrolment.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "criteriaquerytest" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	@Test
	public void testModifiableSessionDefaultCriteria() {
		clearCounts();

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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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

	@Test
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
	
	@Test
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
	
	@Test
    @SkipForDialect( value = SybaseASE15Dialect.class, jiraKey = "HHH-3032", strictMatching = true)
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

	@Test
	public void testDetachedCriteria() {
		DetachedCriteria dc = DetachedCriteria.forClass(Student.class)
			.add( Property.forName("name").eq("Gavin King") )
			.addOrder( Order.asc("studentNumber") );

		byte[] bytes = SerializationHelper.serialize(dc);

		dc = (DetachedCriteria) SerializationHelper.deserialize( bytes );

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

	@Test
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
		if ( e.getStudent().getStudentNumber() == gavin.getStudentNumber() ) {
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
		if ( e.getStudent().getStudentNumber() == gavin.getStudentNumber() ) {
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
		if ( e.getStudent().getStudentNumber() == gavin.getStudentNumber() ) {
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

