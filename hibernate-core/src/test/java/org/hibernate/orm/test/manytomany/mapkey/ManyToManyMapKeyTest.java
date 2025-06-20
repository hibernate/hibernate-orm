/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.manytomany.mapkey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.Table;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				ManyToManyMapKeyTest.MapContainer.class,
				ManyToManyMapKeyTest.MapKeyEntity.class,
				ManyToManyMapKeyTest.MapValueEntity.class
		}
)
@SessionFactory
@JiraKey("HHH-16370")
public class ManyToManyMapKeyTest {


	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testInsert(SessionFactoryScope scope) {
		MapKeyEntity keyEntity = new MapKeyEntity( 2l, "first entity" );
		scope.inTransaction(
				session -> {
					MapContainer container = new MapContainer( 1l, "container" );

					MapValueEntity valueEntity = new MapValueEntity( 3l, container, keyEntity, "1" );

					container.add( keyEntity, valueEntity );

					session.persist( container );
					session.persist( keyEntity );
					session.persist( valueEntity );
				}
		);

		scope.inTransaction(
				session -> {
					List<MapContainer> mapContainers = session.createQuery(
							"select mc from MapContainer mc",
							MapContainer.class
					).list();
					assertThat( mapContainers.size() ).isEqualTo( 1 );
					MapContainer container = mapContainers.get( 0 );
					Map<MapKeyEntity, MapValueEntity> map = container.getMap();
					assertThat( map.size() ).isEqualTo( 1 );

					MapValueEntity valueEntity = map.get( keyEntity );
					assertThat( valueEntity ).isNotNull();
					assertThat( valueEntity.getMapContainer() ).isSameAs( container );
					assertThat( map.keySet().size() ).isEqualTo( 1 );
					assertThat( valueEntity.getMapKey() ).isSameAs( map.keySet().iterator().next() );
				}
		);

		scope.inTransaction(
				session -> {
					List<MapContainer> mapContainers = session.createQuery(
							"select mc from MapContainer mc join fetch mc.map",
							MapContainer.class
					).list();
					assertThat( mapContainers.size() ).isEqualTo( 1 );
					MapContainer container = mapContainers.get( 0 );
					Map<MapKeyEntity, MapValueEntity> map = container.getMap();
					assertThat( map.size() ).isEqualTo( 1 );

					MapValueEntity valueEntity = map.get( keyEntity );
					assertThat( valueEntity ).isNotNull();
					assertThat( valueEntity.getMapContainer() ).isSameAs( container );
					assertThat( map.keySet().size() ).isEqualTo( 1 );
					assertThat( valueEntity.getMapKey() ).isSameAs( map.keySet().iterator().next() );
				}
		);
	}

	@Test
	public void testUpdate(SessionFactoryScope scope) {
		MapKeyEntity keyEntity = new MapKeyEntity( 2l, "first entity" );
		scope.inTransaction(
				session -> {
					MapContainer container = new MapContainer( 1l, "container" );

					MapValueEntity valueEntity = new MapValueEntity( 3l, container, keyEntity, "1" );

					container.add( keyEntity, valueEntity );

					session.persist( container );
					session.persist( keyEntity );
					session.persist( valueEntity );
				}
		);

		scope.inTransaction(
				session -> {
					List<MapContainer> mapContainers = session.createQuery(
							"select mc from MapContainer mc",
							MapContainer.class
					).list();
					assertThat( mapContainers.size() ).isEqualTo( 1 );
					MapContainer container = mapContainers.get( 0 );

					MapValueEntity toRemove = container.getMap().get( keyEntity );
					session.remove( toRemove );

					container.getMap().remove( keyEntity );

					session.flush();

					MapValueEntity valueEntity = new MapValueEntity( 4l, container, keyEntity, "2" );
					session.persist( valueEntity );

					container.add( keyEntity, valueEntity );
				}
		);
	}

	@Entity(name = "MapContainer")
	@Table(name = "map_container")
	public static class MapContainer {
		@Id
		private Long id;

		private String name;

		@ManyToMany(cascade = CascadeType.REMOVE)
		@MapKey(name = "mapKey")
		private Map<MapKeyEntity, MapValueEntity> map = new HashMap<>();

		public MapContainer() {
		}

		public MapContainer(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Map<MapKeyEntity, MapValueEntity> getMap() {
			return map;
		}

		public void add(MapKeyEntity key, MapValueEntity entity) {
			this.map.put( key, entity );
		}
	}

	@Entity(name = "MapKeyEntity")
	@Table(name = "map_key_entity")
	public static class MapKeyEntity {
		@Id
		private Long id;

		private String name;

		public MapKeyEntity() {
		}

		public MapKeyEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MapKeyEntity keyEntity = (MapKeyEntity) o;
			return Objects.equals( id, keyEntity.id ) && Objects.equals( name, keyEntity.name );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id, name );
		}
	}

	@Entity(name = "MapValueEntity")
	@Table(name = "map_value_entity")
	public static class MapValueEntity {
		@Id
		private Long id;

		@ManyToOne(optional = false)
		private MapContainer mapContainer;

		@ManyToOne(optional = false)
		@JoinColumn(unique = true)
		private MapKeyEntity mapKey;

		private String name;

		public MapValueEntity() {
		}

		public MapValueEntity(Long id, MapContainer mapContainer, MapKeyEntity mapKey, String name) {
			this.id = id;
			this.mapContainer = mapContainer;
			this.mapKey = mapKey;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public MapContainer getMapContainer() {
			return mapContainer;
		}

		public MapKeyEntity getMapKey() {
			return mapKey;
		}
	}
}
