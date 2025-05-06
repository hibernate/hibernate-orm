/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.map.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Gavin King
 */
public class DynamicClassTest extends BaseCoreFunctionalTestCase {

	@Override
	protected String getBaseForMappings() {
		return "org/hibernate/orm/test/";
	}

	@Override
	public String[] getMappings() {
		return new String[] { "entitymode/map/basic/ProductLine.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
	}

	@Test
	public void testLazyDynamicClass() {
		Session s = openSession();
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
		s.persist("ProductLine", cars);
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
		s.remove(cars);
		t.commit();
		s.close();
	}


}
