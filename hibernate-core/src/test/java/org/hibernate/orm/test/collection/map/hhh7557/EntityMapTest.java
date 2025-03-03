/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map.hhh7557;

import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Elizabeth Chatman
 * @author Steve Ebersole
 */
@DomainModel(
		annotatedClasses = {
				MapValue.class, MapKey.class, MapHolder.class
		}
)
@SessionFactory
public class EntityMapTest {

	@Test
	public void testInsertIntoMap(SessionFactoryScope scope) {
		// Session 1: Insert 3 values into the map
		scope.inTransaction(
				session -> {
					MapHolder mapHolder = new MapHolder();
					mapHolder.setMap( new HashMap<>() );
					addMapEntry( session, mapHolder, "A", "1" );
					addMapEntry( session, mapHolder, "B", "2" );
					addMapEntry( session, mapHolder, "C", "3" );
					session.persist( mapHolder );
					// Verify there are 3 entries in the map
					assertEquals( 3, mapHolder.getMap().size() );
				}
		);


		// Session 2: Add a 4th value to the map
		scope.inTransaction(
				session -> {
					MapHolder mapHolder = getMapHolder( session );
					assertEquals( 3, mapHolder.getMap().size() );
					System.out.println( "Got MapHolder; checked map size -----" );
					addMapEntry( session, mapHolder, "D", "4" );
					// Verify there are 4 entries in the map
					assertEquals( 4, mapHolder.getMap().size() );

				}
		);

		// Session 3: Count the entries in the map
		scope.inTransaction(
				session -> {
					MapHolder mapHolder = getMapHolder( session );
					// Fails here (expected:<4> but was:<1>)
					assertEquals( 4, mapHolder.getMap().size() );
				}
		);

	}

	private void addMapEntry(Session session, MapHolder mapHolder, String key, String value) {
		MapValue entityValue = new MapValue( value );
		session.persist( entityValue );
		MapKey entityKey = new MapKey( key, entityValue );
		session.persist( entityKey );
		mapHolder.getMap().put( entityKey, entityValue );
	}

	private MapHolder getMapHolder(Session session) {
		List mapHolders = session.createQuery( "select distinct mh from MapHolder mh" ).list();
		assertEquals( 1, mapHolders.size() );
		return (MapHolder) mapHolders.get( 0 );
	}
}
