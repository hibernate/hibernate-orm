/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.compositefk;

import java.io.Serializable;
import java.util.List;

import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Marco Belladelli
 */
@DomainModel(annotatedClasses = {
		OneToManyEmbeddedIdFKNotNullableTest.ChildEntity.class,
		OneToManyEmbeddedIdFKNotNullableTest.ParentEntity.class
})
@SessionFactory
@JiraKey("HHH-15866")
public class OneToManyEmbeddedIdFKNotNullableTest {
	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from ChildEntity" ).executeUpdate();
			session.createMutationQuery( "delete from ParentEntity" ).executeUpdate();
		} );
	}

	@Test
	public void testEmbeddedIdNotNullable(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ChildEntity c1 = new ChildEntity();
			ChildEntity c2 = new ChildEntity();
			ParentEntity parentEntity = new ParentEntity(
					new ParentEntityId( 1, new NestedEmbeddable( "parent_1" ) ),
					List.of( c1, c2 )
			);
			session.persist( parentEntity );
			assertEquals( 2, parentEntity.getChildEntities().size() );
		} );
	}

	@Test
	public void testNullFKShouldThrowException(SessionFactoryScope scope) {
		try {
			scope.inTransaction( session -> {
				ChildEntity childEntity = new ChildEntity();
				session.persist( childEntity );
			} );
			fail( "Inserting child entity without FK should not be allowed." );
		}
		catch (PersistenceException e) {
			assertEquals( ConstraintViolationException.class, e.getClass() );
		}
	}

	@Entity(name = "ChildEntity")
	@Table(name = "child_entity")
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
	public static class NestedEmbeddable {
		private String name;

		public NestedEmbeddable() {
		}

		public NestedEmbeddable(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Embeddable
	public static class ParentEntityId implements Serializable {
		private int id;

		private NestedEmbeddable nestedEmbeddable;

		public ParentEntityId() {
		}

		public ParentEntityId(int id, NestedEmbeddable nestedEmbeddable) {
			this.id = id;
			this.nestedEmbeddable = nestedEmbeddable;
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public NestedEmbeddable getNestedEmbeddable() {
			return nestedEmbeddable;
		}

		public void setNestedEmbeddable(NestedEmbeddable nestedEmbeddable) {
			this.nestedEmbeddable = nestedEmbeddable;
		}
	}

	@Entity(name = "ParentEntity")
	@Table(name = "parent_entity")
	public static class ParentEntity {
		@EmbeddedId
		private ParentEntityId id;

		@OneToMany(cascade = CascadeType.ALL)
		@JoinColumns({
				@JoinColumn(name = "parent_entity_id", referencedColumnName = "id", nullable = false),
				@JoinColumn(name = "parent_entity_name", referencedColumnName = "name", nullable = false)
		})
		private List<ChildEntity> childEntities;

		public ParentEntity() {
		}

		public ParentEntity(ParentEntityId id, List<ChildEntity> childEntities) {
			this.id = id;
			this.childEntities = childEntities;
		}

		public ParentEntityId getId() {
			return id;
		}

		public void setId(ParentEntityId id) {
			this.id = id;
		}

		public List<ChildEntity> getChildEntities() {
			return childEntities;
		}

		public void setChildEntities(List<ChildEntity> childEntities) {
			this.childEntities = childEntities;
		}
	}
}
