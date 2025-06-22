/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.bag;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.Test;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Gail Badner
 */
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsBagProvider.class )
)
@DomainModel(annotatedClasses = {
		BagElementNullBasicTest.AnEntity.class,
		BagElementNullBasicTest.NullableElementsEntity.class
})
@SessionFactory
public class BagElementNullBasicTest {

	@Test
	public void testPersistNullValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( null );
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
					e.aCollection.add( null );
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
	@JiraKey(value = "HHH-13651")
	public void addNullValueToNullableCollections(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					NullableElementsEntity e = new NullableElementsEntity();
					e.list.add( null );
					session.persist( e );
					session.flush();
				}
		);
	}

	@Test
	public void testUpdateNonNullValueToNull(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( entityId, scope ).size() );
					e.aCollection.set( 0, null );
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
	public void testUpdateNonNullValueToNullWithExtraValue(SessionFactoryScope scope) {
		int entityId = scope.fromTransaction(
				session -> {
					AnEntity e = new AnEntity();
					e.aCollection.add( "def" );
					e.aCollection.add( "ghi" );
					session.persist( e );
					return e.id;
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 2, e.aCollection.size() );
					assertEquals( 2, getCollectionElementRows( e.id, scope ).size() );
					e.aCollection.set( 0, null );
				}
		);

		scope.inTransaction(
				session -> {
					AnEntity e = session.get( AnEntity.class, entityId );
					assertEquals( 1, e.aCollection.size() );
					assertEquals( 1, getCollectionElementRows( e.id, scope ).size() );
					assertEquals( "ghi", e.aCollection.get( 0 ) );
					session.remove( e );
				}
		);
	}

	private List getCollectionElementRows(int id, SessionFactoryScope scope) {
		return scope.fromTransaction(
				session ->
						session.createNativeQuery(
								"SELECT aCollection FROM AnEntity_aCollection where AnEntity_id = " + id
						).list()
		);
	}

	@Entity( name = "AnEntity" )
	@Table(name = "AnEntity")
	public static class AnEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "AnEntity_aCollection", joinColumns = { @JoinColumn(name = "AnEntity_id") })
		@OrderBy
		private List<String> aCollection = new ArrayList<>();
	}

	@Entity( name = "NullableElementsEntity" )
	@Table(name = "NullableElementsEntity")
	public static class NullableElementsEntity {
		@Id
		@GeneratedValue
		private int id;

		@ElementCollection
		@CollectionTable(name = "e_2_string", joinColumns = @JoinColumn(name = "e_id"))
		@Column(name = "string_value", unique = false, nullable = true, insertable = true, updatable = true)
		private List<String> list = new ArrayList<>();
	}
}
