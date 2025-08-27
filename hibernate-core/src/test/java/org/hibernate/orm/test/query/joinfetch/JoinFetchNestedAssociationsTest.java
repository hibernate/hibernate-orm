/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.query.joinfetch;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@DomainModel( annotatedClasses = {
		JoinFetchNestedAssociationsTest.AEntity.class,
		JoinFetchNestedAssociationsTest.BaseBEntity.class,
		JoinFetchNestedAssociationsTest.B1Entity.class,
		JoinFetchNestedAssociationsTest.B2Entity.class,
		JoinFetchNestedAssociationsTest.CEntity.class,
} )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-18469" )
public class JoinFetchNestedAssociationsTest {
	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final AEntity result = session.createQuery(
					"select a from AEntity a " +
							"left join fetch a.b_entity b_join " +
							"left join fetch b_join.c_entities c",
					AEntity.class
			).getSingleResult();
			final B2Entity bEntity = result.getB_entity();
			assertThat( bEntity ).matches( Hibernate::isInitialized );
			assertThat( bEntity.getC_entities() ).matches( Hibernate::isInitialized )
					.hasSize( 1 )
					.allMatch( c -> c.getName().equals( "c_entity" ) );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			// B1 or B2 doesn't matter as they map to the same table
			final B1Entity b = new B1Entity();
			b.setId( 1L );
			session.persist( b );
			session.flush();

			final AEntity a = new AEntity();
			a.setB_entity_id( 1L );
			final CEntity c = new CEntity();
			c.setbEntityId( 1L );
			c.setName( "c_entity" );
			session.persist( a );
			session.persist( c );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity( name = "AEntity" )
	static class AEntity {
		@Id
		@GeneratedValue
		private Long id;

		@OneToOne
		@JoinColumn( name = "b_entity_id", updatable = false, insertable = false )
		private B2Entity b_entity;

		@Column( name = "b_entity_id" )
		private Long b_entity_id;

		public B2Entity getB_entity() {
			return b_entity;
		}

		public Long getB_entity_id() {
			return b_entity_id;
		}

		public void setB_entity_id(Long b_entity_id) {
			this.b_entity_id = b_entity_id;
		}
	}

	@MappedSuperclass
	static class BaseBEntity {
		@Id
		private Long id;

		@OneToMany( mappedBy = "b_entity" )
		private Set<CEntity> c_entities = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<CEntity> getC_entities() {
			return c_entities;
		}
	}

	@Entity( name = "B1Entity" )
	@Table( name = "b_entities" )
	static class B1Entity extends BaseBEntity {
	}

	@Entity( name = "B2Entity" )
	@Table( name = "b_entities" )
	static class B2Entity extends BaseBEntity {
	}

	@Entity( name = "CEntity" )
	static class CEntity {
		@Id
		@GeneratedValue
		private Long id;

		@Column( name = "b_id" )
		private Long bEntityId;

		@ManyToOne
		@JoinColumn( name = "b_id", updatable = false, insertable = false )
		private B1Entity b_entity;

		private String name;

		public Long getbEntityId() {
			return bEntityId;
		}

		public void setbEntityId(Long bEntityId) {
			this.bEntityId = bEntityId;
		}

		public B1Entity getB_entity() {
			return b_entity;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
