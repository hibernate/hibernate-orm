/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.map;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.testing.junit5.SessionFactoryBasedFunctionalTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Gail Badner
 */
public class MapElementNullBasicTest extends SessionFactoryBasedFunctionalTest {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				AnEntity.class
		};
	}

	@Test
	public void testPersistNullValue() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.put( "null", null );
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void addNullValue() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					e.aCollection.put( "null", null );
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.put( "abc", "def" );
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId ) );
					e.aCollection.put( "abc", null );
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNullToNonNull() {
		int entityId = inTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.put( "abc", "def" );
					session.persist( e );
					return e.id;
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId ) );
					e.aCollection.put( "abc", null );
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId ) );
					e.aCollection.put( "abc", "not null" );
				}
		);

		inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId ) );
					assertEquals( "not null", e.aCollection.get( "abc" ) );
					session.delete( e );
				}
		);
	}

	private int getCollectionElementRows(int id) {
		return inTransaction(
				session -> {
					return session.doReturningWork(
							// todo (6.0) : use native query when native queries will be implemented
							work -> {
								PreparedStatement statement = null;
								ResultSet resultSet = null;

								try {
									statement = work.prepareStatement(
											"SELECT count(aCollection) as numberOfRows FROM AnEntity_aCollection where AnEntity_id = " + id );
									statement.execute();
									resultSet = statement.getResultSet();
									if ( resultSet.next() ) {
										return resultSet.getInt( "numberOfRows" );
									}
									return 0;
								}
								finally {
									if ( resultSet != null && !resultSet.isClosed() ) {
										resultSet.close();
									}
									if ( statement != null && !statement.isClosed() ) {
										statement.close();
									}
								}
							}
					);
				}
		);
	}

	@Entity
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aCollection", joinColumns = { @JoinColumn(name = "AnEntity_id") })
		private Map<String, String> aCollection = new HashMap<>();
	}
}
