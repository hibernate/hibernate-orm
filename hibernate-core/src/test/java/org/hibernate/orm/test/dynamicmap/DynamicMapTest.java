/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dynamicmap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;

@JiraKey(value = "HHH-12539")
@DomainModel(
		xmlMappings = "org/hibernate/orm/test/dynamicmap/Test.xml"
)
@SessionFactory
public class DynamicMapTest {

	@Test
	public void bootstrappingTest(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Map<String,Object> item1 = new HashMap<>();
			item1.put( "name", "cup" );
			item1.put( "description", "abc" );

			final Map<String,Object> entity1 = new HashMap<>();
			entity1.put( "name", "first_entity" );
			item1.put( "entity", entity1 );

			session.persist( "Entity1", entity1 );
			session.persist( "Item1", item1 );
		} );

		scope.inTransaction( session -> {
			//noinspection rawtypes
			final List<Map> result = session.createSelectionQuery( "from Item1", Map.class ).list();
			assertThat( result.size(), is( 1 ) );

			//noinspection unchecked
			final Map<String,Object> item1 = (Map<String, Object>) result.get( 0 );
			assertThat( item1.get( "name" ), is( "cup" ) );

			final Object entity1 = item1.get( "entity" );
			assertThat( entity1, notNullValue() );

			//noinspection unchecked
			assertThat( ( (Map<String,Object>) entity1 ).get( "name" ), is( "first_entity" ) );
		} );
	}
}
