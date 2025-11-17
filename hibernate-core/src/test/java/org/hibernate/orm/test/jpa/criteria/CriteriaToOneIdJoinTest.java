/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.criteria;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = { CriteriaToOneIdJoinTest.ParentEntity.class, CriteriaToOneIdJoinTest.ChildEntity.class } )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16926" )
public class CriteriaToOneIdJoinTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ChildEntity child = new ChildEntity( "child_entity" );
			entityManager.persist( child );
			final ParentEntity parentEntity = new ParentEntity( child );
			entityManager.persist( parentEntity );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from ParentEntity" ).executeUpdate();
			entityManager.createQuery( "delete from ChildEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testIdJoin(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
			final CriteriaQuery<ParentEntity> query = cb.createQuery( ParentEntity.class );
			final Root<ParentEntity> root = query.from( ParentEntity.class );
			final Join<Object, Object> secondaryJoin = root.join( "child" );
			query.select( root ).orderBy( cb.asc( secondaryJoin.get( "name" ) ) );
			final ParentEntity result = entityManager.createQuery( query ).getSingleResult();
			assertThat( result.getChild().getName() ).isEqualTo( "child_entity" );
		} );
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		@ManyToOne
		private ChildEntity child;

		public ParentEntity() {
		}

		public ParentEntity(ChildEntity child) {
			this.child = child;
		}

		public ChildEntity getChild() {
			return child;
		}
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public ChildEntity() {
		}

		public ChildEntity(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
