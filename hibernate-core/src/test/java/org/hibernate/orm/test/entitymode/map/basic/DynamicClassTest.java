/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.map.basic;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Gavin King
 */
@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(xmlMappings = "org/hibernate/orm/test/entitymode/map/basic/ProductLine.xml")
@SessionFactory
public class DynamicClassTest {

	@Test
	public void testLazyDynamicClass(SessionFactoryScope factoryScope) {
		factoryScope.inTransaction( (s) -> {
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
		} );

		factoryScope.inTransaction( (s) -> {
			Map cars = (Map) s.createQuery("from ProductLine pl order by pl.description").uniqueResult();
			List models = (List) cars.get("models");
			assertFalse( Hibernate.isInitialized(models) );
			assertEquals( 2, models.size() );
			assertTrue( Hibernate.isInitialized(models) );

		} );

		factoryScope.inTransaction( (s) -> {
			List list = s.createQuery("from Model m").list();
			for ( Iterator i=list.iterator(); i.hasNext(); ) {
				assertFalse( Hibernate.isInitialized( ( (Map) i.next() ).get("productLine") ) );
			}
			Map model = (Map) list.get(0);
			assertTrue( ( (List) ( (Map) model.get("productLine") ).get("models") ).contains(model) );
		} );


		factoryScope.inTransaction( (s) -> {
			Map cars = (Map) s.createQuery("from ProductLine pl order by pl.description").uniqueResult();
			s.remove(cars);
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}
}
