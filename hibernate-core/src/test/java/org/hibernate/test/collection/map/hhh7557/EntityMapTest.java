/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
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
package org.hibernate.test.collection.map.hhh7557;

import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;

import org.junit.Assert;
import org.junit.Test;

import org.hibernate.testing.FailureExpectedWithNewMetamodel;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Elizabeth Chatman
 * @author Steve Ebersole
 */
@FailureExpectedWithNewMetamodel // missing unique-constraint default naming
public class EntityMapTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MapValue.class, MapKey.class, MapHolder.class};
	}

	@Test
	public void testInsertIntoMap() throws Exception {
		{
			// Session 1: Insert 3 values into the map
			Session session = openSession();
			session.beginTransaction();
			MapHolder mapHolder = new MapHolder();
			mapHolder.setMap( new HashMap<MapKey, MapValue>() );
			addMapEntry( session, mapHolder, "A", "1" );
			addMapEntry( session, mapHolder, "B", "2" );
			addMapEntry( session, mapHolder, "C", "3" );
			session.save( mapHolder );
			// Verify there are 3 entries in the map
			Assert.assertEquals( 3, mapHolder.getMap().size() );
			session.getTransaction().commit();
			session.close();
		}

		{
			// Session 2: Add a 4th value to the map
			Session session = openSession();
			session.beginTransaction();
			MapHolder mapHolder = getMapHolder( session );
			System.out.println( "Got MapHolder; checking map size -----" );
			Assert.assertEquals( 3, mapHolder.getMap().size() );
			System.out.println( "Got MapHolder; checked map size -----" );
			addMapEntry( session, mapHolder, "D", "4" );
			// Verify there are 4 entries in the map
			Assert.assertEquals( 4, mapHolder.getMap().size() );
			session.getTransaction().commit();
			session.close();
		}

		{
			// Session 3: Count the entries in the map
			Session session = openSession();
			session.beginTransaction();
			MapHolder mapHolder = getMapHolder( session );
			// Fails here (expected:<4> but was:<1>)
			Assert.assertEquals( 4, mapHolder.getMap().size() );
			session.getTransaction().commit();
			session.close();
		}
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
		Assert.assertEquals( 1, mapHolders.size() );
		return (MapHolder) mapHolders.get( 0 );
	}
}
