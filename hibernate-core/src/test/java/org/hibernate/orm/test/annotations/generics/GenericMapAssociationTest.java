/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.generics;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.query.criteria.JpaPath;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory
@DomainModel( annotatedClasses = {
		GenericMapAssociationTest.AbstractParent.class,
		GenericMapAssociationTest.MapContainerEntity.class,
		GenericMapAssociationTest.MapKeyEntity.class,
		GenericMapAssociationTest.MapValueEntity.class
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16378" )
public class GenericMapAssociationTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final MapContainerEntity container = new MapContainerEntity( 1L );
			final MapKeyEntity key = new MapKeyEntity( 2L, "key" );
			final MapValueEntity value = new MapValueEntity( 3L, "value" );
			container.getMap().put( key, value );
			session.persist( container );
			session.persist( key );
			session.persist( value );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from MapContainerEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MapValueEntity" ).executeUpdate();
			session.createMutationQuery( "delete from MapKeyEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testChildQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select c.id from MapContainerEntity p join p.map c",
				Long.class
		).getSingleResult() ).isEqualTo( 3L ) );
	}

	@Test
	public void testChildCriteriaQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final CriteriaBuilder cb = session.getCriteriaBuilder();
			final CriteriaQuery<Long> query = cb.createQuery( Long.class );
			final Root<MapContainerEntity> root = query.from( MapContainerEntity.class );
			final Join<MapContainerEntity, MapValueEntity> join = root.join( "map" );
			// generic attributes are always reported as Object java type
			assertThat( join.getJavaType() ).isEqualTo( Object.class );
			assertThat( join.getModel() ).isSameAs( root.getModel().getAttribute( "map" ) );
			assertThat( ( (JpaPath<?>) join ).getResolvedModel()
								.getBindableJavaType() ).isEqualTo( MapValueEntity.class );
			query.select( join.get( "id" ) );
			final Long result = session.createQuery( query ).getSingleResult();
			assertThat( result ).isEqualTo( 3L );
		} );
	}

	@Test
	public void testKeyQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select key(p.map).keyName from MapContainerEntity p",
				String.class
		).getSingleResult() ).isEqualTo( "key" ) );
	}

	@Test
	public void testValueQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> assertThat( session.createQuery(
				"select value(p.map).valueName from MapContainerEntity p",
				String.class
		).getSingleResult() ).isEqualTo( "value" ) );
	}

	@MappedSuperclass
	public static abstract class AbstractParent<K, E> {
		@OneToMany
		@JoinTable( name = "map_join_table", joinColumns = @JoinColumn( name = "container_id" ) )
		private Map<K, E> map;

		public AbstractParent() {
			this.map = new HashMap<>();
		}

		public Map<K, E> getMap() {
			return map;
		}
	}

	@Entity( name = "MapContainerEntity" )
	@Table( name = "map_container_entity" )
	public static class MapContainerEntity extends AbstractParent<MapKeyEntity, MapValueEntity> {
		@Id
		private Long id;

		public MapContainerEntity() {
		}

		public MapContainerEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}
	}

	@Entity( name = "MapKeyEntity" )
	@Table( name = "map_key_entity" )
	public static class MapKeyEntity {
		@Id
		private Long id;

		private String keyName;

		public MapKeyEntity() {
		}

		public MapKeyEntity(Long id, String keyName) {
			this.id = id;
			this.keyName = keyName;
		}

		public Long getId() {
			return id;
		}

		public String getKeyName() {
			return keyName;
		}
	}

	@Entity( name = "MapValueEntity" )
	@Table( name = "map_value_entity" )
	public static class MapValueEntity {
		@Id
		private Long id;

		private String valueName;

		public MapValueEntity() {
		}

		public MapValueEntity(Long id, String valueName) {
			this.id = id;
			this.valueName = valueName;
		}

		public Long getId() {
			return id;
		}

		public String getValueName() {
			return valueName;
		}
	}
}
