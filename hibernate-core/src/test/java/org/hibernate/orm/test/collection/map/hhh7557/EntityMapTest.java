/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.map.hhh7557;

import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Elizabeth Chatman
 * @author Steve Ebersole
 */
public class EntityMapTest extends SessionFactoryBasedFunctionalTest {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { MapValue.class, MapKey.class, MapHolder.class };
	}

	@Test
	public void testInsertIntoMap() {
		// Session 1: Insert 3 values into the map
		inTransaction(
				session -> {
					MapHolder mapHolder = new MapHolder();
					mapHolder.setMap( new HashMap<>() );
					addMapEntry( session, mapHolder, "A", "1" );
					addMapEntry( session, mapHolder, "B", "2" );
					addMapEntry( session, mapHolder, "C", "3" );
					session.save( mapHolder );
					// Verify there are 3 entries in the map
					assertEquals( 3, mapHolder.getMap().size() );
				}
		);

		// Session 2: Add a 4th value to the map
		inTransaction(
				session -> {
					MapHolder mapHolder = getMapHolder( session );
					System.out.println( "Got MapHolder; checking map size -----" );
					assertEquals( 3, mapHolder.getMap().size() );
					System.out.println( "Got MapHolder; checked map size -----" );
					addMapEntry( session, mapHolder, "D", "4" );
					// Verify there are 4 entries in the map
					assertEquals( 4, mapHolder.getMap().size() );
				}
		);

		// Session 3: Count the entries in the map
		inTransaction(
				session -> {
					MapHolder mapHolder = getMapHolder( session );
					// Fails here (expected:<4> but was:<1>)
					assertEquals( 4, mapHolder.getMap().size() );

				}
		);
	}

	private void addMapEntry(Session session, MapHolder mapHolder, String key, String value) {
		System.out.println( "Inserting (" + key + "," + value + ") into map" );
		MapValue entityValue = new MapValue( value );
		session.save( entityValue );
		MapKey entityKey = new MapKey( key, entityValue );
		session.save( entityKey );
		mapHolder.getMap().put( entityKey, entityValue );
	}

	private MapHolder getMapHolder(Session session) {
		List mapHolders = session.createQuery( "select distinct mh from MapHolder mh" ).list();
		assertEquals( 1, mapHolders.size() );
		return (MapHolder) mapHolders.get( 0 );
	}
}
