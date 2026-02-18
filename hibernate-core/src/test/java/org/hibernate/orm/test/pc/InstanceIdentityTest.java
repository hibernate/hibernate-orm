/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.pc;

import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import org.hibernate.annotations.Immutable;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(annotatedClasses = {
		InstanceIdentityTest.ImmutableEntity.class,
		InstanceIdentityTest.EntityWithCollections.class,
})
@SessionFactory
@BytecodeEnhanced
public class InstanceIdentityTest {
	@Test
	public void testEnhancedImmutableEntity(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ImmutableEntity entity1 = new ImmutableEntity( 1, "entity_1" );
			session.persist( entity1 );

			// false warning, bytecode enhancement of the test class will make the cast work
			assertThat( ((ManagedEntity) entity1).$$_hibernate_getInstanceId() ).isGreaterThanOrEqualTo( 0 );
			assertThat( session.contains( entity1 ) ).isTrue();

			final ImmutableEntity entity2 = new ImmutableEntity( 2, "entity_2" );
			session.persist( entity2 );
			final ImmutableEntity entity3 = new ImmutableEntity( 3, "entity_3" );
			session.persist( entity3 );
		} );

		scope.inSession( session -> {
			assertThat( session.find( ImmutableEntity.class, 1 ) ).isNotNull().extracting( ImmutableEntity::getName )
					.isEqualTo( "entity_1" );

			final List<ImmutableEntity> immutableEntities = session.createQuery(
					"from ImmutableEntity",
					ImmutableEntity.class
			).getResultList();

			assertThat( immutableEntities ).hasSize( 3 );

			// test find again, this time from 1st level cache
			final ImmutableEntity entity2 = session.find( ImmutableEntity.class, 2 );
			assertThat( entity2 ).isNotNull().extracting( ImmutableEntity::getName )
					.isEqualTo( "entity_2" );

			session.detach( entity2 );
			assertThat( session.contains( entity2 ) ).isFalse();
		} );
	}

	@Test
	public void testPersistentCollections(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ImmutableEntity immutableEntity = new ImmutableEntity( 4, "entity_4" );
			session.persist( immutableEntity );
			final EntityWithCollections entity = new EntityWithCollections( 1 );
			entity.getStringList().addAll( List.of( "one", "two" ) );
			entity.getEntityMap().put( "4", immutableEntity );
			session.persist( entity );

			assertThat( entity.getStringList() ).isInstanceOf( PersistentCollection.class );
			final PersistentCollection<?> persistentList = (PersistentCollection<?>) entity.getStringList();
			assertThat( persistentList.$$_hibernate_getInstanceId() ).isGreaterThanOrEqualTo( 0 );

			assertThat( entity.getEntityMap() ).isInstanceOf( PersistentCollection.class );
			final PersistentCollection<?> persistentMap = (PersistentCollection<?>) entity.getEntityMap();
			assertThat( persistentMap.$$_hibernate_getInstanceId() ).isGreaterThanOrEqualTo( 0 );

			assertThat( session.getPersistenceContextInternal().getCollectionEntries() ).isNotNull()
					.containsKeys( persistentList, persistentMap );
		} );

		scope.inTransaction( session -> {
			final EntityWithCollections entity = session.find( EntityWithCollections.class, 1 );
			entity.getStringList().add( "three" );
			entity.getEntityMap().clear();
		} );

		scope.inSession( session -> {
			final EntityWithCollections entity = session.find( EntityWithCollections.class, 1 );
			assertThat( entity.getStringList() ).hasSize( 3 ).containsExactlyInAnyOrder( "one", "two", "three" );
			assertThat( entity.getEntityMap() ).isEmpty();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Immutable
	@Entity(name = "ImmutableEntity")
	static class ImmutableEntity {
		@Id
		private Integer id;

		private String name;

		public ImmutableEntity() {
		}

		public ImmutableEntity(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "EntityWithCollections")
	static class EntityWithCollections {
		@Id
		private Integer id;

		@ElementCollection
		private List<String> stringList = new ArrayList<>();

		@OneToMany
		private Map<String, ImmutableEntity> entityMap = new HashMap<>();

		public EntityWithCollections() {
		}

		public EntityWithCollections(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<String> getStringList() {
			return stringList;
		}

		public void setStringList(List<String> stringList) {
			this.stringList = stringList;
		}

		public Map<String, ImmutableEntity> getEntityMap() {
			return entityMap;
		}

		public void setEntityMap(Map<String, ImmutableEntity> entityMap) {
			this.entityMap = entityMap;
		}
	}
}
