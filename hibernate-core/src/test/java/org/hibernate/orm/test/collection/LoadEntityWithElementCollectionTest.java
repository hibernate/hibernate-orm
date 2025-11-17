/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.dialect.SybaseDialect;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;


@DomainModel(
		annotatedClasses = {
				LoadEntityWithElementCollectionTest.IndexedEntity.class
		}
)
@SessionFactory
@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true)
public class LoadEntityWithElementCollectionTest {

	public static class PostUpdateTestListener implements PostUpdateEventListener {

		boolean isPostUpdateCalled;

		@Override
		public void onPostUpdate(PostUpdateEvent event) {
			isPostUpdateCalled = true;
		}

		public boolean isPostUpdateCalled() {
			return isPostUpdateCalled;
		}
	}

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					IndexedEntity entity = new IndexedEntity();
					entity.setId( 1L );
					entity.setSerializableArray( new boolean[] { true, false } );

					session.persist( entity );

					IndexedEntity entity2 = new IndexedEntity();
					entity2.setId( 2L );
					entity2.setElementCollectionArray( new boolean[] { true, false } );
					final HashSet<IndexedEntity> indexedEntities = new HashSet<>();
					indexedEntities.add( entity );
					entity2.setIndexedEntities( indexedEntities );

					session.persist( entity2 );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void onPostUpdateMethodShouldBeCalledTest(SessionFactoryScope scope) {

		final PostUpdateTestListener listener = new PostUpdateTestListener();
		scope.getSessionFactory().getEventEngine().getListenerRegistry().setListeners(
				EventType.POST_UPDATE,
				listener
		);

		scope.inTransaction(
				session -> {
					final IndexedEntity indexedEntity = session.get( IndexedEntity.class, 1L );
					assertNotNull( indexedEntity.getElementCollectionArray() );
					assertEquals( 0, indexedEntity.getElementCollectionArray().length );
					assertNotNull( indexedEntity.getSerializableArray() );
					indexedEntity.setSerializableArray( new boolean[] { false, true } );
				}
		);

		assertTrue( listener.isPostUpdateCalled() );

	}

	@Test
	public void onPostUpdateMethodShouldBeCalledTest2(SessionFactoryScope scope) {

		final PostUpdateTestListener listener = new PostUpdateTestListener();
		scope.getSessionFactory().getEventEngine().getListenerRegistry().setListeners(
				EventType.POST_UPDATE,
				listener
		);

		scope.inTransaction(
				session -> {
					final IndexedEntity indexedEntity = session.get( IndexedEntity.class, 1L );
					assertNotNull( indexedEntity.getElementCollectionArray() );
					assertEquals( 0, indexedEntity.getElementCollectionArray().length );
					final boolean[] serializableArray = indexedEntity.getSerializableArray();
					assertNotNull( serializableArray );
					serializableArray[0] = !serializableArray[0];
					serializableArray[1] = !serializableArray[1];
				}
		);

		assertTrue( listener.isPostUpdateCalled() );
	}

	@Test
	public void onPostUpdateMethodShouldNotBeCalledTest(SessionFactoryScope scope) {

		final PostUpdateTestListener listener = new PostUpdateTestListener();
		scope.getSessionFactory().getEventEngine().getListenerRegistry().setListeners(
				EventType.POST_UPDATE,
				listener
		);

		scope.inTransaction(
				session -> {
					final IndexedEntity indexedEntity = session.get( IndexedEntity.class, 1L );
					assertNotNull( indexedEntity.getElementCollectionArray() );
					assertEquals( 0, indexedEntity.getElementCollectionArray().length );
					assertNotNull( indexedEntity.getSerializableArray() );
				}
		);

		assertFalse( listener.isPostUpdateCalled() );

		scope.inTransaction(
				session -> {
					final IndexedEntity indexedEntity = session.get( IndexedEntity.class, 2L );
					assertNotNull( indexedEntity.getElementCollectionArray() );
					assertNull( indexedEntity.getSerializableArray() );
				}
		);

		assertFalse( listener.isPostUpdateCalled() );

	}

	@Entity(name = "IndexedEntity")
	@Table( name = "t_entities" )
	public static class IndexedEntity {
		@Id
		public Long id;

		public boolean[] serializableArray;

		@ElementCollection
		@CollectionTable( name = "t_array_items", joinColumns = @JoinColumn( name = "entity_fk") )
		@OrderColumn
		public boolean[] elementCollectionArray;

		@OneToMany(fetch = FetchType.EAGER)
		@JoinTable( name = "t_referenced_entities", joinColumns = @JoinColumn( name = "entity_fk") )
		public Set<IndexedEntity> indexedEntities;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public boolean[] getSerializableArray() {
			return serializableArray;
		}

		public void setSerializableArray(boolean[] serializableArray) {
			this.serializableArray = serializableArray;
		}

		public boolean[] getElementCollectionArray() {
			return elementCollectionArray;
		}

		public void setElementCollectionArray(boolean[] elementCollectionArray) {
			this.elementCollectionArray = elementCollectionArray;
		}

		public Set<IndexedEntity> getIndexedEntities() {
			return indexedEntities;
		}

		public void setIndexedEntities(Set<IndexedEntity> indexedEntities) {
			this.indexedEntities = indexedEntities;
		}

	}

}
