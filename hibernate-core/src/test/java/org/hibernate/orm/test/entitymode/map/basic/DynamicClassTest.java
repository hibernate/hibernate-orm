/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitymode.map.basic;

import org.hibernate.Hibernate;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Gavin King
 */
@SessionFactory
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/entitymode/map/basic/ProductLine.hbm.xml"
)
class DynamicClassTest {

	@Test
	void testLazyDynamicClass(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Map<String, Object> cars = new HashMap<>();
			cars.put( "description", "Cars" );
			Map<String, Object> monaro = new HashMap<>();
			monaro.put( "productLine", cars );
			monaro.put( "name", "monaro" );
			monaro.put( "description", "Holden Monaro" );
			Map<String, Object> hsv = new HashMap<>();
			hsv.put( "productLine", cars );
			hsv.put( "name", "hsv" );
			hsv.put( "description", "Holden Commodore HSV" );
			List<Map<String, Object>> models = new ArrayList<>();
			cars.put( "models", models );
			models.add( hsv );
			models.add( monaro );
			s.persist( "ProductLine", cars );
		} );

		scope.inTransaction( s -> {
			Map<String, Object> cars = (Map<String, Object>) s.createQuery(
					"from ProductLine pl order by pl.description" ).uniqueResult();
			List<Map<String, Object>> models = (List<Map<String, Object>>) cars.get( "models" );
			assertFalse( Hibernate.isInitialized( models ) );
			assertEquals( 2, models.size() );
			assertTrue( Hibernate.isInitialized( models ) );

			s.clear();

			List<?> list = s.createQuery( "from Model m" ).list();
			for ( Iterator<?> i = list.iterator(); i.hasNext(); ) {
				assertFalse( Hibernate.isInitialized( ((Map<String, Object>) i.next()).get( "productLine" ) ) );
			}
			Map<String, Object> model = (Map<String, Object>) list.get( 0 );
			assertTrue( ((List<Map<String, Object>>) ((Map<String, Object>) model.get( "productLine" )).get(
					"models" )).contains( model ) );
			s.clear();

		} );

		scope.inTransaction( s -> {
			Map<String, Object> cars = (Map<String, Object>) s.createQuery(
					"from ProductLine pl order by pl.description" ).uniqueResult();
			s.remove( cars );
		} );
	}

	@Test
	void multiload(SessionFactoryScope scope) {
		final Object id = scope.fromTransaction( s -> {
			Map<String, Object> cars = new HashMap<>();
			cars.put( "description", "Cars" );
			Map<String, Object> monaro = new HashMap<>();
			monaro.put( "productLine", cars );
			monaro.put( "name", "monaro" );
			monaro.put( "description", "Holden Monaro" );
			Map<String, Object> hsv = new HashMap<>();
			hsv.put( "productLine", cars );
			hsv.put( "name", "hsv" );
			hsv.put( "description", "Holden Commodore HSV" );
			List<Map<String, Object>> models = new ArrayList<>();
			cars.put( "models", models );
			models.add( hsv );
			models.add( monaro );
			s.persist( "ProductLine", cars );

			return cars.get( "id" );
		} );

		scope.inTransaction( s -> {
			var rootGraph = s.getSessionFactory().createGraphForDynamicEntity( "ProductLine" );

			List<Map<String, ?>> found = s.findMultiple( rootGraph, List.of( id ) );

			assertThat( found ).hasSize( 1 );
		} );

	}
}
