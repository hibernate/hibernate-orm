//$Id: OneToOneCacheTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.legacy;

import java.io.Serializable;

import junit.framework.Test;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * Simple testcase to illustrate HB-992
 *
 * @author Wolfgang Voelkl, michael
 */
public class OneToOneCacheTest extends LegacyTestCase {

	private Serializable generatedId;

	public OneToOneCacheTest(String x) {
		super( x );
	}

	public String[] getMappings() {
		return new String[] { "legacy/Object2.hbm.xml", "legacy/MainObject.hbm.xml" };
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( OneToOneCacheTest.class );
	}

	public void testOneToOneCache() throws HibernateException {

		//create a new MainObject
		createMainObject();
		// load the MainObject
		readMainObject();

		//create and add Ojbect2
		addObject2();

		//here the newly created Object2 is written to the database
		//but the MainObject does not know it yet
		MainObject mainObject = readMainObject();

		assertNotNull( mainObject.getObj2() );

		// after evicting, it works.
		getSessions().evict( MainObject.class );

		mainObject = readMainObject();

		assertNotNull( mainObject.getObj2() );

	}

	/**
	 * creates a new MainObject
	 * <p/>
	 * one hibernate transaction !
	 */
	private void createMainObject() throws HibernateException {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		MainObject mo = new MainObject();
		mo.setDescription( "Main Test" );

		generatedId = session.save( mo );

		tx.commit();
		session.close();
	}

	/**
	 * loads the newly created MainObject
	 * and adds a new Object2 to it
	 * <p/>
	 * one hibernate transaction
	 */
	private void addObject2() throws HibernateException {
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		MainObject mo =
				( MainObject ) session.load( MainObject.class, generatedId );

		Object2 toAdd = new Object2();
		toAdd.setDummy( "test" );

		//toAdd should now be saved by cascade
		mo.setObj2( toAdd );

		tx.commit();
		session.close();
	}

	/**
	 * reads the newly created MainObject
	 * and its Object2 if it exists
	 * <p/>
	 * one hibernate transaction
	 */
	private MainObject readMainObject() throws HibernateException {
		Long returnId = null;
		Session session = openSession();
		Transaction tx = session.beginTransaction();

		Serializable id = generatedId;

		MainObject mo = ( MainObject ) session.load( MainObject.class, id );

		tx.commit();
		session.close();

		return mo;
	}
}
