/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

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
					session.remove( e );
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
					session.remove( e );
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
					session.remove( e );
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
					session.remove( e );
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

	@Entity(name = "AnEntity")
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
