//$Id: CriteriaQueryTest.java 10976 2006-12-12 23:22:26Z steve.ebersole@jboss.com $
package org.hibernate.test.criteria;

import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Hibernate;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Property;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.test.hql.Animal;
import org.hibernate.test.hql.Reptile;
import org.hibernate.transform.Transformers;
import org.hibernate.type.Type;
import org.hibernate.util.SerializationHelper;

/**
 * @author Gavin King
 */
public class CriteriaQueryTest extends FunctionalTestCase {
	
	public CriteriaQueryTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "criteria/Enrolment.hbm.xml", "hql/Animal.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_QUERY_CACHE, "true" );
		cfg.setProperty( Environment.CACHE_REGION_PREFIX, "criteriaquerytest" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( Environment.GENERATE_STATISTICS, "true" );
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( CriteriaQueryTest.class );
	}

	public void testEscapeCharacter() {
		Session session = openSession();
		Transaction t = session.beginTransaction();
		Course c1 = new Course();
		c1.setCourseCode( "course-1" );
		c1.setDescription( "%1" );
		Course c2 = new Course();
		c2.setCourseCode( "course-2" );
		c2.setDescription( "%2" );
		Course c3 = new Course();
		c3.setCourseCode( "course-3" );
		c3.setDescription( "control" );
		session.persist( c1 );
		session.persist( c2 );
		session.persist( c3 );
		session.flush();
		session.clear();

		// finds all courses which have a description equal to '%1'
		Course example = new Course();
		example.setDescription( "&%1" );
		List result = session.createCriteria( Course.class )
				.add( Example.create( example ).ignoreCase().enableLike().setEscapeCharacter( new Character( '&' ) ) )
				.list();
		assertEquals( 1, result.size() );
		// finds all courses which contain '%' as the first char in the description 
		example.setDescription( "&%%" );
		result = session.createCriteria( Course.class )
				.add( Example.create( example ).ignoreCase().enableLike().setEscapeCharacter( new Character( '&' ) ) )
				.list();
		assertEquals( 2, result.size() );

		session.createQuery( "delete Course" ).executeUpdate();
		t.commit();
		session.close();
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
		ScrollableResults sr = session.createCriteria(Course.class).scroll();
		assertTrue( sr.next() );
		course = (Course) sr.get(0);
		assertNotNull(course);
		sr.close();
		session.delete(course);
		
		t.commit();
		session.close();
		
	}
	
	public void testSubselect() {

		Session session = openSession();
		Transaction t = session.beginTransaction();

		Course course = new Course();
		course.setCourseCode("HIB");
		course.setDescription("Hibernate Training");
		session.persist(course);
		
		Student gavin = new Student();
		gavin.setName("Gavin King");
		gavin.setStudentNumber(232);
		session.persist(gavin);

		Enrolment enrolment2 = new Enrolment();
		enrolment2.setCourse(course);
		enrolment2.setCourseCode(course.getCourseCode());
		enrolment2.setSemester((short) 3);
		enrolment2.setYear((short) 1998);
		enrolment2.setStudent(gavin);
		enrolment2.setStudentNumber(gavin.getStudentNumber());
		gavin.getEnrolments().add(enrolment2);
		session.persist(enrolment2);
		
		DetachedCriteria dc = DetachedCriteria.forClass(Student.class)
			.add( Property.forName("studentNumber").eq( new Long(232) ) )
			.setProjection( Property.forName("name") );

		session.createCriteria(Student.class)
			.add( Subqueries.propertyEqAll("name", dc) )
			.list();
		
		session.createCriteria(Student.class)
			.add( Subqueries.exists(dc) )
			.list();
	
		session.createCriteria(Student.class)
			.add( Property.forName("name").eqAll(dc) )
			.list();
	
		session.createCriteria(Student.class)
			.add( Subqueries.in("Gavin King", dc) )
			.list();
		
		DetachedCriteria dc2 = DetachedCriteria.forClass(Student.class, "st")
			.add( Property.forName("st.studentNumber").eqProperty("e.studentNumber") )
			.setProjection( Property.forName("name") );
		
		session.createCriteria(Enrolment.class, "e")
			.add( Subqueries.eq("Gavin King", dc2) )
			.list();

		DetachedCriteria dc3 = DetachedCriteria.forClass(Student.class, "st")
			.createCriteria("enrolments")
				.createCriteria("course")
					.add( Property.forName("description").eq("Hibernate Training") )
					.setProjection( Property.forName("st.name") );
	
		session.createCriteria(Enrolment.class, "e")
			.add( Subqueries.eq("Gavin King", dc3) )
			.list();

		session.delete(enrolment2);
		session.delete(gavin);
		session.delete(course);
		t.commit();
		session.close();
		
	}
	
	public void testDetachedCriteria() {
		
		DetachedCriteria dc = DetachedCriteria.forClass(Student.class)
			.add( Property.forName("name").eq("Gavin King") )
			.addOrder( Order.asc("studentNumber") )
			.setProjection( Property.forName("studentNumber") );
		
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
		
		List result = dc.getExecutableCriteria(session)
			.setMaxResults(3)
			.list();
		
		assertEquals( result.size(), 2 );
		assertEquals( result.get(0), new Long(232) );
		assertEquals( result.get(1), new Long(666) );
		
		session.delete(gavin);
		session.delete(bizarroGavin);
		t.commit();
		session.close();
	}
	
		public void testProjectionCache() {
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
			
			List list = s.createCriteria(Enrolment.class)
				.createAlias("student", "s")
				.createAlias("course", "c")
				.add( Restrictions.isNotEmpty("s.enrolments") )
				.setProjection( Projections.projectionList()
						.add( Projections.property("s.name") )
						.add( Projections.property("c.description") )
				)
				.setCacheable(true)
				.list();
			
			assertEquals( list.size(), 2 );
			assertEquals( ( (Object[]) list.get(0) ).length, 2 );
			assertEquals( ( (Object[]) list.get(1) ).length, 2 );
			
			t.commit();
			s.close();
	
			s = openSession();
			t = s.beginTransaction();
			
			s.createCriteria(Enrolment.class)
				.createAlias("student", "s")
				.createAlias("course", "c")
				.add( Restrictions.isNotEmpty("s.enrolments") )
				.setProjection( Projections.projectionList()
						.add( Projections.property("s.name") )
						.add( Projections.property("c.description") )
				)
				.setCacheable(true)
				.list();
		
			assertEquals( list.size(), 2 );
			assertEquals( ( (Object[]) list.get(0) ).length, 2 );
			assertEquals( ( (Object[]) list.get(1) ).length, 2 );
			
			t.commit();
			s.close();
	
			s = openSession();
			t = s.beginTransaction();
			
			s.createCriteria(Enrolment.class)
				.createAlias("student", "s")
				.createAlias("course", "c")
				.add( Restrictions.isNotEmpty("s.enrolments") )
				.setProjection( Projections.projectionList()
						.add( Projections.property("s.name") )
						.add( Projections.property("c.description") )
				)
				.setCacheable(true)
				.list();
			
			assertEquals( list.size(), 2 );
			assertEquals( ( (Object[]) list.get(0) ).length, 2 );
			assertEquals( ( (Object[]) list.get(1) ).length, 2 );
			
			s.delete(enrolment1);
			s.delete(enrolment2);
			s.delete(course);
			s.delete(gavin);
			s.delete(xam);
		
			t.commit();
			s.close();
	}
	
	public void testProjections() {
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
		
		//s.flush();
		
		Integer count = (Integer) s.createCriteria(Enrolment.class)
			.setProjection( Projections.count("studentNumber").setDistinct() )
			.uniqueResult();
		assertEquals(count, new Integer(2));
		
		Object object = s.createCriteria(Enrolment.class)
			.setProjection( Projections.projectionList()
					.add( Projections.count("studentNumber") )
					.add( Projections.max("studentNumber") )
					.add( Projections.min("studentNumber") )
					.add( Projections.avg("studentNumber") )
			)
			.uniqueResult();
		Object[] result = (Object[])object; 
		
		assertEquals(new Integer(2),result[0]);
		assertEquals(new Long(667),result[1]);
		assertEquals(new Long(101),result[2]);
		assertEquals( 384.0, ( (Double) result[3] ).doubleValue(), 0.01 );
		
		
		List resultWithMaps = s.createCriteria(Enrolment.class)
			.setProjection( Projections.distinct( Projections.projectionList()
					.add( Projections.property("studentNumber"), "stNumber" )
					.add( Projections.property("courseCode"), "cCode" ) )
			)
		    .add( Restrictions.gt( "studentNumber", new Long(665) ) )
		    .add( Restrictions.lt( "studentNumber", new Long(668) ) )
		    .addOrder( Order.asc("stNumber") )
			.setResultTransformer(Criteria.ALIAS_TO_ENTITY_MAP)
			.list();
		
		assertEquals(1, resultWithMaps.size());
		Map m1 = (Map) resultWithMaps.get(0);
		
		assertEquals(new Long(667), m1.get("stNumber"));
		assertEquals(course.getCourseCode(), m1.get("cCode"));		

		resultWithMaps = s.createCriteria(Enrolment.class)
			.setProjection( Projections.property("studentNumber").as("stNumber") )
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
					.add( Projections.property("st.name"), "studentName" )
					.add( Projections.property("co.description"), "courseDescription" )
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
					.add( Projections.property("this.name") )
					.add( Projections.property("e.year") )
					.add( Projections.property("e.semester") )
					.add( Projections.property("c.courseCode") )
					.add( Projections.property("c.description") )
				)
			.uniqueResult();
			
		Projection p1 = Projections.projectionList()
			.add( Projections.count("studentNumber") )
			.add( Projections.max("studentNumber") )
			.add( Projections.rowCount() );
		
		Projection p2 = Projections.projectionList()
			.add( Projections.min("studentNumber") )
			.add( Projections.avg("studentNumber") )
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
					.add( Projections.groupProperty("co.courseCode") )
					.add( Projections.count("st.studentNumber").setDistinct() )
					.add( Projections.groupProperty("year") )
			)
			.list();
		
		assertEquals( list.size(), 2 );
		
		Object g = s.createCriteria(Student.class)
			.add( Restrictions.idEq( new Long(667) ) )
			.setFetchMode("enrolments", FetchMode.JOIN)
			//.setFetchMode("enrolments.course", FetchMode.JOIN) //TODO: would love to make that work...
			.uniqueResult();
		assertSame(g, gavin);

		s.delete(gavin);
		s.delete(xam);
		s.delete(course);
		
		t.commit();
		s.close();
	}
		
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
		
		Integer count = (Integer) s.createCriteria(Enrolment.class)
			.setProjection( Property.forName("studentNumber").count().setDistinct() )
			.uniqueResult();
		assertEquals(count, new Integer(2));
		
		Object object = s.createCriteria(Enrolment.class)
			.setProjection( Projections.projectionList()
					.add( Property.forName("studentNumber").count() )
					.add( Property.forName("studentNumber").max() )
					.add( Property.forName("studentNumber").min() )
					.add( Property.forName("studentNumber").avg() )
			)
			.uniqueResult();
		Object[] result = (Object[])object; 
		
		assertEquals(new Integer(2),result[0]);
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
}

