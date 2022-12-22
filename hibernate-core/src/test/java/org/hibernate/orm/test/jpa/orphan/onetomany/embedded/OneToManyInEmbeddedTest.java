/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.orphan.onetomany.embedded;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Marco Belladelli
 */
@Jpa(annotatedClasses = {
		OneToManyInEmbeddedTest.ChildEntity.class,
		OneToManyInEmbeddedTest.ParentEntity.class
})
@JiraKey("HHH-15864")
public class OneToManyInEmbeddedTest {
	@BeforeEach
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			ParentEntity parentEntity = new ParentEntity( new ChildEntityWrapper( List.of( new ChildEntity() ) ) );
			entityManager.persist( parentEntity );
		} );
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			entityManager.createQuery( "delete from ChildEntity" ).executeUpdate();
			entityManager.createQuery( "delete from ParentEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testOrphanRemovalInEmbedded(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			ParentEntity parentEntity = entityManager.find( ParentEntity.class, 1 );
			parentEntity.getChildEntityWrapper().getChildEntities().clear();
			entityManager.remove( parentEntity );
		} );

		scope.inTransaction( entityManager -> assertTrue(
				entityManager.createQuery( "from ChildEntity" ).getResultList().isEmpty(),
				"Orphan entity was not removed"
		) );
	}

	@Entity(name = "ChildEntity")
	public static class ChildEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}

	@Embeddable
	public static class ChildEntityWrapper {
		@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
		@JoinColumn(name = "parent_entity_id", referencedColumnName = "id")
		private List<ChildEntity> childEntities;

		public ChildEntityWrapper() {
		}

		public ChildEntityWrapper(List<ChildEntity> childEntities) {
			this.childEntities = childEntities;
		}

		public List<ChildEntity> getChildEntities() {
			return childEntities;
		}

		public void setChildEntities(List<ChildEntity> childEntities) {
			this.childEntities = childEntities;
		}
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		@Id
		@GeneratedValue(strategy = GenerationType.IDENTITY)
		private int id;

		@Embedded
		private ChildEntityWrapper childEntityWrapper = new ChildEntityWrapper();

		public ParentEntity() {
		}

		public ParentEntity(ChildEntityWrapper childEntityWrapper) {
			this.childEntityWrapper = childEntityWrapper;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public ChildEntityWrapper getChildEntityWrapper() {
			return childEntityWrapper;
		}

		public void setChildEntityWrapper(ChildEntityWrapper childEntityWrapper) {
			this.childEntityWrapper = childEntityWrapper;
		}
	}
}
