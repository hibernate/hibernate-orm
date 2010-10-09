//$Id: BackrefTest.java 10976 2006-12-12 23:22:26Z steve.ebersole@jboss.com $
package org.hibernate.test.unidir;

import junit.framework.Test;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class BackrefTest extends FunctionalTestCase {

	public BackrefTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "unidir/ParentChild.hbm.xml" };
	}

	public String getCacheConcurrencyStrategy() {
		return null;
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( BackrefTest.class );
	}

	public void testBackRef() {
		Session s = openSession();
		Transaction t = s.beginTransaction();
		Parent p = new Parent("Marc");
		Parent p2 = new Parent("Nathalie");
		Child c = new Child("Elvira");
		Child c2 = new Child("Blase");
		p.getChildren().add(c);
		p.getChildren().add(c2);
		s.persist(p);
		s.persist(p2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		c = (Child) s.get(Child.class, "Elvira");
		c.setAge(2);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Parent) s.get(Parent.class, "Marc");
		c = (Child) s.get(Child.class, "Elvira");
		c.setAge(18);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		p = (Parent) s.get(Parent.class, "Marc");
		p2 = (Parent) s.get(Parent.class, "Nathalie");
		c = (Child) s.get(Child.class, "Elvira");
		assertEquals( p.getChildren().indexOf(c), 0 );
		p.getChildren().remove(c);
		p2.getChildren().add(c);
		t.commit();

		s.close();
		s = openSession();
		t = s.beginTransaction();
		Parent p3 = new Parent("Marion");
		p3.getChildren().add( new Child("Gavin") );
		s.merge(p3);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		s.createQuery( "delete from Child" ).executeUpdate();
		s.createQuery( "delete from Parent" ).executeUpdate();
		t.commit();
		s.close();
	}

	public void testBackRefToProxiedEntityOnMerge() {
		Session s = openSession();
		s.beginTransaction();
		Parent me = new Parent( "Steve" );
		me.getChildren().add( new Child( "Joe" ) );
  		s.persist( me );
		s.getTransaction().commit();
		s.close();

		// while detached, add a new element
		me.getChildren().add( new Child( "Cece" ) );
		me.getChildren().add( new Child( "Austin" ) );

		s = openSession();
		s.beginTransaction();
		// load 'me' to associate it with the new session as a proxy (this may have occurred as 'prior work'
		// to the reattachment below)...
		Object meProxy = s.load( Parent.class, me.getName() );
		assertFalse( Hibernate.isInitialized( meProxy ) );
		// now, do the reattchment...
		s.merge( me );
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		s.createQuery( "delete from Child" ).executeUpdate();
		s.createQuery( "delete from Parent" ).executeUpdate();
		s.getTransaction().commit();
		s.close();
	}
}

