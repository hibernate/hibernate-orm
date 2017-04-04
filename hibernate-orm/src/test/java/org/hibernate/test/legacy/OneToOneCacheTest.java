/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.legacy;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Simple testcase to illustrate HB-992
 *
 * @author Wolfgang Voelkl, michael
 */
public class OneToOneCacheTest extends LegacyTestCase {
	private Serializable generatedId;

	@Override
	public String[] getMappings() {
		return new String[] { "legacy/Object2.hbm.xml", "legacy/MainObject.hbm.xml" };
	}

	@Test
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

		// afterQuery evicting, it works.
		sessionFactory().getCache().evictEntityRegion( MainObject.class );

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

		MainObject mo = ( MainObject ) session.get( MainObject.class, id );

		tx.commit();
		session.close();

		return mo;
	}
}
