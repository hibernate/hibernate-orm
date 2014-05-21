/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2006-2011, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.entitymode.map.basic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.EntityMode;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.FailureExpectedWithNewUnifiedXsd;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

/**
 * @author Gavin King
 */
@FailureExpectedWithNewUnifiedXsd(message = "HbmXmlTransformer does not currently support EntityMode.MAP")
public class DynamicClassTest extends BaseCoreFunctionalTestCase {
	@Override
	public String[] getMappings() {
		return new String[] { "entitymode/map/basic/ProductLine.hbm.xml" };
	}

	@Override
	public void configure(Configuration cfg) {
		cfg.setProperty(Environment.DEFAULT_ENTITY_MODE, EntityMode.MAP.toString());
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

