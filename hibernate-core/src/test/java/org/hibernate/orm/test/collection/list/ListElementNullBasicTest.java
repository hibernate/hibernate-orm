/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.list;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
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
		annotatedClasses = ListElementNullBasicTest.AnEntity.class
)
@SessionFactory
public class ListElementNullBasicTest {

	@Test
	public void testPersistNullValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.theStrings.add( null );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.theStrings.size() );
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
					assertEquals( 0, e.theStrings.size() );
					assertEquals( 0, getCollectionElementRows( entityId, scope ).size() );
					e.theStrings.add( null );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.theStrings.size() );
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
					e.theStrings.add( "def" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.theStrings.size() );
					assertEquals( 1, getCollectionElementRows( entityId, scope ).size() );
					e.theStrings.set( 0, null );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 0, e.theStrings.size() );
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
					e.theStrings.add( "def" );
					e.theStrings.add( "ghi" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 2, e.theStrings.size() );
					assertEquals( 2, getCollectionElementRows( e.id, scope ).size() );
					e.theStrings.set( 0, null );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 2, e.theStrings.size() );
					assertEquals( 1, getCollectionElementRows( e.id, scope ).size() );
					e.theStrings.set( 0, "not null" );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 2, e.theStrings.size() );
					assertEquals( 2, getCollectionElementRows( e.id, scope ).size() );
					assertEquals( "not null", e.theStrings.get( 0 ) );
					assertEquals( "ghi", e.theStrings.get( 1 ) );
					session.remove( e );
				}
		);
	}

	private List<String> getCollectionElementRows(int id, SessionFactoryScope scope) {
		return scope.fromTransaction(
				session -> session.createNativeQuery(
						"select val from the_strings where entity_fk = " + id,
						String.class
				).list()
		);
	}

	@Entity(name = "AnEntity")
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "the_strings", joinColumns = @JoinColumn(name = "entity_fk") )
		@OrderColumn(name = "position")
		@Column(name = "val")
		private List<String> theStrings = new ArrayList<>();
	}
}
