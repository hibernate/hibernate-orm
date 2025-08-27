/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.mappedBy;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel( annotatedClasses = {
		OneToManyMapEntityKeyMappedByTest.MapContainerEntity.class,
		OneToManyMapEntityKeyMappedByTest.MapKeyEntity.class,
		OneToManyMapEntityKeyMappedByTest.MapValueEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16355" )
public class OneToManyMapEntityKeyMappedByTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MapContainerEntity container = new MapContainerEntity( 1L );
			final MapKeyEntity key = new MapKeyEntity( 2L );
			final MapValueEntity value = new MapValueEntity( 3L, container );
			container.getMap().put( key, value );
			session.persist( container );
			session.persist( key );
			session.persist( value );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MapValueEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MapKeyEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MapContainerEntity" ).executeUpdate();
		} );
	}

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MapContainerEntity container = session.createQuery(
					"from MapContainerEntity",
					MapContainerEntity.class
			).getSingleResult();
			assertThat( container.getId() ).isEqualTo( 1L );
			assertThat( container.getMap() ).hasSize( 1 );
			final Map.Entry<MapKeyEntity, MapValueEntity> entry = container.getMap().entrySet().iterator().next();
			assertThat( entry.getKey().getId() ).isEqualTo( 2L );
			assertThat( entry.getValue().getId() ).isEqualTo( 3L );
		} );
	}

	@Entity( name = "MapContainerEntity" )
	@Table( name = "map_container_entity" )
	public static class MapContainerEntity {
		@Id
		private Long id;
		@OneToMany( mappedBy = "container" )
		private Map<MapKeyEntity, MapValueEntity> map;

		public MapContainerEntity() {
		}

		public MapContainerEntity(Long id) {
			this.id = id;
			this.map = new HashMap<>();
		}

		public Long getId() {
			return id;
		}

		public Map<MapKeyEntity, MapValueEntity> getMap() {
			return map;
		}
	}

	@Entity( name = "MapKeyEntity" )
	@Table( name = "map_key_entity" )
	public static class MapKeyEntity {
		@Id
		private Long id;

		public MapKeyEntity() {
		}

		public MapKeyEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "MapValueEntity" )
	@Table( name = "map_value_entity" )
	public static class MapValueEntity {
		@Id
		private Long id;
		@ManyToOne
		private MapContainerEntity container;

		public MapValueEntity() {
		}

		public MapValueEntity(Long id, MapContainerEntity container) {
			this.id = id;
			this.container = container;
		}

		public Long getId() {
			return id;
		}

		public MapContainerEntity getContainer() {
			return container;
		}
	}
}
