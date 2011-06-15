package org.hibernate.test.optlock;

import org.hibernate.Session;
import org.hibernate.test.annotations.TestCase;

/**
 * @author Strong Liu
 */
public class IssueHibernateTest extends TestCase {
	public IssueHibernateTest() {
	}

	public IssueHibernateTest(String name) {
		super( name );
	}

	public void testCheckDBVersion() {
		Session s = openSession();
		s.beginTransaction();
		A a1 = new A( 1, "test1" );
		s.persist( a1 );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		A a=(A)s.get( A.class,1 );
		assertEquals( 0,a.getVersion() );
		s.clear();

		Session s2 = openSession();
		s2.beginTransaction();
		A a2 = (A)s2.get( A.class,1 );
		a2.setDescr( "changed 2" );
		s2.merge( a2 );
		s2.getTransaction().commit();


	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { A.class };
	}
}
