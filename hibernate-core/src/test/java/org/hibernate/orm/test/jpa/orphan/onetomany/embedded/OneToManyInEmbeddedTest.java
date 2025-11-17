/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetomany.embedded;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@Jpa( annotatedClasses = {
		OneToManyInEmbeddedTest.ChildEntity.class,
		OneToManyInEmbeddedTest.ParentEntity.class
} )
public class OneToManyInEmbeddedTest {
	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.persist( new ParentEntity( ChildWrapper.of( new ChildEntity( 1 ) ), null ) );
			entityManager.persist( new ParentEntity( null, EagerChildWrapper.of( new ChildEntity( 2 ) ) ) );
		} );
	}

	@AfterAll
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from ChildEntity" ).executeUpdate();
			entityManager.createQuery( "delete from ParentEntity" ).executeUpdate();
		} );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-15864" )
	public void testOrphanRemovalInEmbedded(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ParentEntity parentEntity = entityManager.find( ParentEntity.class, 1 );
			parentEntity.getChildWrapper().getChildEntities().clear();
			entityManager.remove( parentEntity );
		} );
		scope.inTransaction( entityManager -> assertTrue(
				entityManager.createQuery( "from ChildEntity where id = 1" ).getResultList().isEmpty(),
				"Orphan entity was not removed"
		) );
	}

	@Test
	@Jira( "https://hibernate.atlassian.net/browse/HHH-16970" )
	public void testOrphanRemovalInEmbeddedEager(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ParentEntity parentEntity = entityManager.find( ParentEntity.class, 2 );
			parentEntity.getEagerChildWrapper().getChildEntities().clear();
			entityManager.remove( parentEntity );
		} );
		scope.inTransaction( entityManager -> assertTrue(
				entityManager.createQuery( "from ChildEntity where id = 2" ).getResultList().isEmpty(),
				"Orphan entity was not removed"
		) );
	}

	@Entity( name = "ChildEntity" )
	public static class ChildEntity {
		@Id
		private int id;

		public int getId() {
			return id;
		}

		public ChildEntity() {
		}

		public ChildEntity(int id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class ChildWrapper {
		@OneToMany( cascade = CascadeType.ALL, orphanRemoval = true )
		@JoinColumn( name = "parent_entity_id", referencedColumnName = "id" )
		private List<ChildEntity> childEntities = new ArrayList<>();

		public static ChildWrapper of(ChildEntity... childEntities) {
			final ChildWrapper cw = new ChildWrapper();
			cw.getChildEntities().addAll( List.of( childEntities ) );
			return cw;
		}

		public List<ChildEntity> getChildEntities() {
			return childEntities;
		}
	}

	@Embeddable
	public static class EagerChildWrapper {
		@OneToMany( fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true )
		@JoinColumn( name = "parent_entity_id", referencedColumnName = "id" )
		private List<ChildEntity> childEntities = new ArrayList<>();

		public static EagerChildWrapper of(ChildEntity... childEntities) {
			final EagerChildWrapper cw = new EagerChildWrapper();
			cw.getChildEntities().addAll( List.of( childEntities ) );
			return cw;
		}

		public List<ChildEntity> getChildEntities() {
			return childEntities;
		}
	}

	@Entity( name = "ParentEntity" )
	public static class ParentEntity {
		@Id
		@GeneratedValue
		private int id;

		@Embedded
		private ChildWrapper childWrapper;

		@Embedded
		private EagerChildWrapper eagerChildWrapper;

		public ParentEntity() {
		}

		public ParentEntity(ChildWrapper childWrapper, EagerChildWrapper eagerChildWrapper) {
			this.childWrapper = childWrapper;
			this.eagerChildWrapper = eagerChildWrapper;
		}

		public int getId() {
			return id;
		}

		public ChildWrapper getChildWrapper() {
			return childWrapper;
		}

		public EagerChildWrapper getEagerChildWrapper() {
			return eagerChildWrapper;
		}
	}
}
