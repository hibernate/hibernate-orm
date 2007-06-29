//$Id: EmbeddedCompositeIdTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.ecid;

import java.util.List;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;
import org.hibernate.proxy.HibernateProxy;

/**
 * @author Gavin King
 */
public class EmbeddedCompositeIdTest extends FunctionalTestCase {
	
	public EmbeddedCompositeIdTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "ecid/Course.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( EmbeddedCompositeIdTest.class );
	}
	
	public void testMerge() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Course uc =  new UniversityCourse("mat2000", "Monash", "second year maths", 0);
		Course c =  new Course("eng5000", "BHS", "grade 5 english");
		s.persist(uc);
		s.persist(c);
		t.commit();
		s.close();
		
		c.setDescription("Grade 5 English");
		uc.setDescription("Second year mathematics");
		
		s = openSession();
		t = s.beginTransaction();
		s.merge(c);
		s.merge(uc);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.delete(c);
		s.delete(uc);
		t.commit();
		s.close();
	}

	public void testMerging() {
		// Test HHH-799
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Course course = new Course( "EN-101", "BA", "preparatory english" );
		s.persist( course );
		t.commit();
		s.close();

		String newDesc = "basic preparatory english";
		course.setDescription( newDesc );

		s = openSession();
		t = s.beginTransaction();
		Course c = (Course) s.merge( course );
		assertEquals( "description not merged", newDesc, c.getDescription() );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		Course cid = new Course( "EN-101", "BA", null );
		course = ( Course ) s.get( Course.class, cid );
		assertEquals( "description not merged", newDesc, course.getDescription() );
		s.delete( course );
		t.commit();
		s.close();
	}

	public void testPolymorphism() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Course uc =  new UniversityCourse("mat2000", "Monash", "second year maths", 0);
		Course c =  new Course("eng5000", "BHS", "grade 5 english");
		s.persist(uc);
		s.persist(c);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		Course ucid = new Course("mat2000", "Monash", null);
		Course cid =  new Course("eng5000", "BHS", null);
		Course luc = (Course) s.load(Course.class, ucid);
		Course lc = (Course) s.load(Course.class, cid);
		assertFalse( Hibernate.isInitialized(luc) );
		assertFalse( Hibernate.isInitialized(lc) );
		assertEquals( UniversityCourse.class, Hibernate.getClass(luc) );
		assertEquals( Course.class, Hibernate.getClass(lc) );
		assertSame( ( (HibernateProxy) lc ).getHibernateLazyInitializer().getImplementation(), cid );
		assertEquals( c.getCourseCode(), "eng5000" );
		assertEquals( uc.getCourseCode(), "mat2000" );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		ucid = new Course("mat2000", "Monash", null);
		cid =  new Course("eng5000", "BHS", null);
		luc = (Course) s.get(Course.class, ucid);
		lc = (Course) s.get(Course.class, cid);
		assertTrue( Hibernate.isInitialized(luc) );
		assertTrue( Hibernate.isInitialized(lc) );
		assertEquals( UniversityCourse.class, Hibernate.getClass(luc) );
		assertEquals( Course.class, Hibernate.getClass(lc) );
		assertSame( lc, cid );
		assertEquals( c.getCourseCode(), "eng5000" );
		assertEquals( uc.getCourseCode(), "mat2000" );
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		List list = s.createQuery("from Course order by courseCode").list();
		assertTrue( list.get(0) instanceof Course );
		assertTrue( list.get(1) instanceof UniversityCourse );
		c = (Course) list.get(0);
		uc = (UniversityCourse) list.get(1);
		assertEquals( c.getCourseCode(), "eng5000" );
		assertEquals( uc.getCourseCode(), "mat2000" );
		t.commit();
		s.close();
		
		c.setDescription("Grade 5 English");
		uc.setDescription("Second year mathematics");
		
		s = openSession();
		t = s.beginTransaction();
		s.saveOrUpdate(c);
		s.saveOrUpdate(uc);
		t.commit();
		s.close();
		
		s = openSession();
		t = s.beginTransaction();
		s.delete(c);
		s.delete(uc);
		t.commit();
		s.close();
		
	}
}

