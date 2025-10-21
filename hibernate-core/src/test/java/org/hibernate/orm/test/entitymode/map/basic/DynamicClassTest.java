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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gavin King
 */
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/entitymode/map/basic/ProductLine.hbm.xml"
)
@SessionFactory
public class DynamicClassTest {

	@Test
	public void testLazyDynamicClass(SessionFactoryScope scope) {

		scope.inTransaction(
				session -> {
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

					session.persist( "ProductLine", cars );
				}
		);

		scope.inTransaction(
				session -> {
					Map<String, Object> cars = (Map<String, Object>) session
							.createQuery( "from ProductLine pl order by pl.description" )
							.uniqueResult();
					List<Map<String, Object>> models = (List<Map<String, Object>>) cars.get( "models" );
					assertThat( Hibernate.isInitialized( models ) ).isFalse();
					assertThat( models ).hasSize( 2 );
					assertThat( Hibernate.isInitialized( models ) ).isTrue();

					session.clear();

					List<Map<String, Map<String, Object>>> list = session.createQuery( "from Model m" ).list();
					for ( Map<String, Map<String, Object>> stringObjectMap : list ) {
						assertThat( Hibernate.isInitialized( stringObjectMap.get( "productLine" ) ) ).isFalse();
					}
					Map<String, Map<String, Object>> model = list.get( 0 );
					assertThat( ((((List<Map<String, Object>>) (model.get( "productLine" )).get( "models" )) ).contains( model );
					session.clear();
				}
		);


		scope.inTransaction(
				session -> {
					session.remove(
							session.createQuery( "from ProductLine pl order by pl.description" ).uniqueResult() );
				}
		);
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
