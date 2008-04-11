//$Id: $

package org.hibernate.test.cascade;

import java.util.Collections;

import junit.framework.Test;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.junit.functional.FunctionalTestCase;
import org.hibernate.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @author Gail Badner
 *
 */

public class MultiPathCascadeTest extends FunctionalTestCase {

	public MultiPathCascadeTest(String name) {
		super( name );
	}

	public String[] getMappings() {
		return new String[] {
				"cascade/MultiPathCascade.hbm.xml"
		};
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( MultiPathCascadeTest.class );
	}

	public void testMultiPathMergeDetachedFailureExpected() throws Exception
	{
		// persist a simple A in the database

		Session s = openSession();
		s.beginTransaction();
		A a = new A();
		a.setData("Anna");
		s.save(a);
		s.getTransaction().commit();
		s.close();

		// modify detached entity
		modifyEntity( a );

		s = openSession();
		s.beginTransaction();
		s.merge(a);
		s.getTransaction().commit();
		s.close();

		verifyModifications( a.getId() );
	}

	public void testMultiPathUpdateDetached() throws Exception
	{
		// persist a simple A in the database

		Session s = openSession();
		s.beginTransaction();
		A a = new A();
		a.setData("Anna");
		s.save(a);
		s.getTransaction().commit();
		s.close();

		// modify detached entity
		modifyEntity( a );

		s = openSession();
		s.beginTransaction();
		s.update(a);
		s.getTransaction().commit();
		s.close();

		verifyModifications( a.getId() );
	}

	public void testMultiPathGetAndModify() throws Exception
	{
		// persist a simple A in the database

		Session s = openSession();
		s.beginTransaction();
		A a = new A();
		a.setData("Anna");
		s.save(a);
		s.getTransaction().commit();
		s.close();

		s = openSession();
		s.beginTransaction();
		// retrieve the previously saved instance from the database, and update it
		a = ( A ) s.get( A.class, new Long( a.getId() ) );
		modifyEntity( a );
		s.getTransaction().commit();
		s.close();

		verifyModifications( a.getId() );
	}

	private void modifyEntity(A a) {
		// create a *circular* graph in detached entity
		a.setData("Anthony");

		G g = new G();
		g.setData("Giovanni");

		H h = new H();
		h.setData("Hellen");

		a.setG(g);
		g.setA(a);

		a.getHs().add(h);
		h.setA(a);

		g.getHs().add(h);
		h.getGs().add(g);
	}

	private void verifyModifications(long aId) {
		Session s = openSession();
		s.beginTransaction();

		// retrieve the A object and check it
		A a = ( A ) s.get( A.class, new Long( aId ) );
		assertEquals( aId, a.getId() );
		assertEquals( "Anthony", a.getData() );
		assertNotNull( a.getG() );
		assertNotNull( a.getHs() );
		assertEquals( 1, a.getHs().size() );

		G gFromA = a.getG();
		H hFromA = ( H ) a.getHs().iterator().next();

		// check the G object
		assertEquals( "Giovanni", gFromA.getData() );
		assertSame( a, gFromA.getA() );
		assertNotNull( gFromA.getHs() );
		assertEquals( a.getHs(), gFromA.getHs() );
		assertSame( hFromA, gFromA.getHs().iterator().next() );

		// check the H object
		assertEquals( "Hellen", hFromA.getData() );
		assertSame( a, hFromA.getA() );
		assertNotNull( hFromA.getGs() );
		assertEquals( 1, hFromA.getGs().size() );
		assertSame( gFromA, hFromA.getGs().iterator().next() );

		s.getTransaction().commit();
		s.close();
	}

}