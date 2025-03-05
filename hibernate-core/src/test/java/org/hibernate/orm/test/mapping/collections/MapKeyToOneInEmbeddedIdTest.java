/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		MapKeyToOneInEmbeddedIdTest.EntityA.class,
		MapKeyToOneInEmbeddedIdTest.EntityBID.class,
		MapKeyToOneInEmbeddedIdTest.EntityB.class,
		MapKeyToOneInEmbeddedIdTest.EntityC.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-17285" )
public class MapKeyToOneInEmbeddedIdTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityC entityC = new EntityC( "entity_c" );
			session.persist( entityC );
			final EntityB entityB = new EntityB( new EntityBID( entityC, 1 ), "entity_b" );
			session.persist( entityB );
			final EntityA entityA = new EntityA( 1 );
			entityA.getbEntities().put( entityC, entityB );
			session.persist( entityA );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from EntityA" ).executeUpdate();
			session.createMutationQuery( "delete from EntityB" ).executeUpdate();
			session.createMutationQuery( "delete from EntityC" ).executeUpdate();
		} );
	}

	@Test
	public void testMapping(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final EntityA entityA = session.find( EntityA.class, 1 );
			assertThat( entityA.getbEntities() ).hasSize( 1 );
			final Map.Entry<EntityC, EntityB> entry = entityA.getbEntities().entrySet().iterator().next();
			assertThat( entry.getKey().getName() ).isEqualTo( "entity_c" );
			assertThat( entry.getValue().getName() ).isEqualTo( "entity_b" );
			assertThat( entry.getValue().getId().getEntity() ).isSameAs( entry.getKey() );
		} );
	}

	@Entity( name = "EntityA" )
	public static class EntityA {
		@Id
		private Integer id;

		@ManyToMany
		@MapKey( name = "id.entity" )
		private Map<EntityC, EntityB> bEntities = new HashMap<>();

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

		public Map<EntityC, EntityB> getbEntities() {
			return bEntities;
		}
	}

	@Embeddable
	public static class EntityBID implements Serializable {
		@ManyToOne
		private EntityC entity;

		private Integer code;

		public EntityBID() {
		}

		public EntityBID(EntityC entity, Integer code) {
			this.entity = entity;
			this.code = code;
		}

		public EntityC getEntity() {
			return entity;
		}

		public Integer getCode() {
			return code;
		}
	}

	@Entity( name = "EntityB" )
	public static class EntityB {
		@EmbeddedId
		private EntityBID id;

		private String name;

		public EntityB() {
		}

		public EntityB(EntityBID id, String name) {
			this.id = id;
			this.name = name;
		}

		public EntityBID getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	@Entity( name = "EntityC" )
	public static class EntityC {
		@Id
		@GeneratedValue
		private Integer id;

		private String name;

		public EntityC() {
		}

		public EntityC(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
