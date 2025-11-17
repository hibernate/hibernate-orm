/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-13380")
@DomainModel(annotatedClasses = {
		LazyOneToManySetEqualsTest.ParentEntity.class,
		LazyOneToManySetEqualsTest.ChildEntity.class
})
@SessionFactory
@BytecodeEnhanced
public class LazyOneToManySetEqualsTest {
	@Test
	public void testRetrievalOfOneToMany(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final ParentEntity parent = entityManager.find( ParentEntity.class, 1L );
			assertThat( parent.children ).hasSize( 2 );
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ParentEntity parent = new ParentEntity();
			parent.id = 1L;
			session.persist( parent );
			final ChildEntity child1 = new ChildEntity( "child_1", parent );
			session.persist( child1 );
			final ChildEntity child2 = new ChildEntity( "child_2", parent );
			session.persist( child2 );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "ParentEntity")
	public static class ParentEntity {
		@Id
		private Long id;

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
		private Set<ChildEntity> children = new HashSet<>();
	}

	@Entity(name = "Course")
	public static class ChildEntity {
		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private ParentEntity parent;

		public ChildEntity() {
		}

		public ChildEntity(String name, ParentEntity parent) {
			this.name = name;
			this.parent = parent;
		}

		@Override
		public boolean equals(Object o) {
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			ChildEntity that = (ChildEntity) o;
			return name.equals( that.name ) && parent.equals( that.parent );
		}

		@Override
		public int hashCode() {
			int result = name.hashCode();
			result = 31 * result + parent.hashCode();
			return result;
		}
	}
}
