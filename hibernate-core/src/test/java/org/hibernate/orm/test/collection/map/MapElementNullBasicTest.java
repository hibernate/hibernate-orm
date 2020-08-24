/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gail Badner
 */
@DomainModel(
		annotatedClasses = MapElementNullBasicTest.AnEntity.class
)
@SessionFactory
public class MapElementNullBasicTest {

	@Test
	public void testPersistNullValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.put( "null", null );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId, scope ).size() );
					session.delete( e );
				}
		);
	}

	@Test
	public void addNullValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId, scope ).size() );
					e.aCollection.put( "null", null );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId, scope ).size() );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.put( "abc", "def" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId, scope ).size() );
					e.aCollection.put( "abc", null );
				}
		);
		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId, scope ).size() );
					session.delete( e );
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNullToNonNull(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.put( "abc", "def" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId, scope ).size() );
					e.aCollection.put( "abc", null );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.aCollection.size() );
					assertEquals( 0, getCollectionElementRows( entityId, scope ).size() );
					e.aCollection.put( "abc", "not null" );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId, scope ).size() );
					assertEquals( "not null", e.aCollection.get( "abc" ) );
					session.delete( e );
				}
		);
	}

	private List<?> getCollectionElementRows(int id, SessionFactoryScope scope) {
		return scope.fromTransaction(
				session -> {
					return session.createNativeQuery(
							"SELECT aCollection FROM AnEntity_aCollection where AnEntity_id = " + id
					).list();
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
