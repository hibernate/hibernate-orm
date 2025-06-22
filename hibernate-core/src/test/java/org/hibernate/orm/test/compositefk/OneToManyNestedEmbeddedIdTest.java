/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {
		OneToManyNestedEmbeddedIdTest.ChildEntity.class,
		OneToManyNestedEmbeddedIdTest.ParentEntity.class,
})
@JiraKey("HHH-15865")
public class OneToManyNestedEmbeddedIdTest {
	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.getEntityManagerFactory().getSchemaManager().truncate();
	}

	@Test
	public void testMergeWithParentEmbeddedId(EntityManagerFactoryScope scope) {
		ParentEntityIdWrapper idWrapper = new ParentEntityIdWrapper( new ParentEntityId( 1 ) );

		scope.inTransaction( em -> {
			ParentEntity parentEntity = new ParentEntity( idWrapper, List.of( new ChildEntity() ) );
			ParentEntity returnedEntity = em.merge( parentEntity );
			assertEquals( 1, returnedEntity.getParentEntityIdWrapper().getParentEntityId().getId() );
			// check if persisted entity behaves correctly
			returnedEntity.getChildEntities().add( new ChildEntity() );
		} );

		scope.inTransaction( em -> {
			ParentEntity foundEntity = em.find( ParentEntity.class, idWrapper );
			assertEquals( 2, foundEntity.getChildEntities().size() );
		} );
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		@Id
		@GeneratedValue
		private int id;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class ParentEntityId {
		private int id;

		public ParentEntityId() {
		}

		public ParentEntityId(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class ParentEntityIdWrapper implements Serializable {
		@Embedded
		private ParentEntityId parentEntityId;

		public ParentEntityIdWrapper() {
		}

		public ParentEntityIdWrapper(ParentEntityId parentEntityId) {
			this.parentEntityId = parentEntityId;
		}

		public ParentEntityId getParentEntityId() {
			return parentEntityId;
		}

		public void setParentEntityId(ParentEntityId parentEntityId) {
			this.parentEntityId = parentEntityId;
		}
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		@EmbeddedId
		private ParentEntityIdWrapper parentEntityIdWrapper;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumn(name = "parent_entity_id", referencedColumnName = "id")
		private List<ChildEntity> childEntities;

		public ParentEntity() {
		}

		public ParentEntity(ParentEntityIdWrapper parentEntityIdWrapper, List<ChildEntity> childEntities) {
			this.parentEntityIdWrapper = parentEntityIdWrapper;
			this.childEntities = childEntities;
		}

		public ParentEntityIdWrapper getParentEntityIdWrapper() {
			return parentEntityIdWrapper;
		}

		public void setParentEntityIdWrapper(ParentEntityIdWrapper parentEntityIdWrapper) {
			this.parentEntityIdWrapper = parentEntityIdWrapper;
		}

		public List<ChildEntity> getChildEntities() {
			return childEntities;
		}

		public void setChildEntities(List<ChildEntity> childEntities) {
			this.childEntities = childEntities;
		}
	}
}
