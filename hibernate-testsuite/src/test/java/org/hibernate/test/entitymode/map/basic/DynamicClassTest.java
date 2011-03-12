//$Id: DynamicClassTest.java 10977 2006-12-12 23:28:04Z steve.ebersole@jboss.com $
package org.hibernate.test.entitymode.map.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.junit.functional.FunctionalTestCase;
import org.hibernate.testing.junit.functional.FunctionalTestClassTestSuite;

/**
 * @author Gavin King
 */
public class DynamicClassTest extends FunctionalTestCase {
	
	public DynamicClassTest(String str) {
		super(str);
	}

	public String[] getMappings() {
		return new String[] { "entitymode/map/basic/ProductLine.hbm.xml" };
	}

	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.DEFAULT_ENTITY_MODE, EntityMode.MAP.toString());
	}

	public static Test suite() {
		return new FunctionalTestClassTestSuite( DynamicClassTest.class );
	}

	public void testLazyDynamicClass() {
		Session s = openSession();
		assertTrue( "Incorrectly handled default_entity_mode", s.getEntityMode() == EntityMode.MAP );
		Session other = s.getSession( EntityMode.MAP );
		assertEquals( "openSession() using same entity-mode returned new session", s, other );

		other = s.getSession( EntityMode.POJO );
		other.close();
		assertTrue( !other.isOpen() );
// this is no longer allowed since the session does much more up-front closed checking
//		assertTrue( other.isConnected() );  // because it is linked to the "root" session's connection

		s.close();

		s = openSession();
		Transaction t = s.beginTransaction();

		Map cars = new HashMap();
		cars.put("description", "Cars");
		Map monaro = new HashMap();
		monaro.put("productLine", cars);
		monaro.put("name", "monaro");
		monaro.put("description", "Holden Monaro");
		Map hsv = new HashMap();
		hsv.put("productLine", cars);
		hsv.put("name", "hsv");
		hsv.put("description", "Holden Commodore HSV");
		List models = new ArrayList();
		cars.put("models", models);
		models.add(hsv);
		models.add(monaro);
		s.save("ProductLine", cars);
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		
		cars = (Map) s.createQuery("from ProductLine pl order by pl.description").uniqueResult();
		models = (List) cars.get("models");
		assertFalse( Hibernate.isInitialized(models) );
		assertEquals( models.size(), 2);
		assertTrue( Hibernate.isInitialized(models) );
		
		s.clear();
		
		List list = s.createQuery("from Model m").list();
		for ( Iterator i=list.iterator(); i.hasNext(); ) {
			assertFalse( Hibernate.isInitialized( ( (Map) i.next() ).get("productLine") ) );
		}
		Map model = (Map) list.get(0);
		assertTrue( ( (List) ( (Map) model.get("productLine") ).get("models") ).contains(model) );
		s.clear();
		
		t.commit();
		s.close();

		s = openSession();
		t = s.beginTransaction();
		cars = (Map) s.createQuery("from ProductLine pl order by pl.description").uniqueResult();
		s.delete(cars);
		t.commit();
		s.close();
	}


}

