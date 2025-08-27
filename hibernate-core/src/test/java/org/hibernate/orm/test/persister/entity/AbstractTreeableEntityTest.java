/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.persister.entity;

import java.util.Collection;
import java.util.List;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = AbstractTreeableEntityTest.TestEntity.class)
@SessionFactory
@JiraKey(value = "HHH-15822")
public class AbstractTreeableEntityTest {

	@Test
	public void testGenericParentQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			TestEntity entity = new TestEntity();
			entity.setName( "test" );
			session.persist( entity );

			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<TestEntity> query = cb.createQuery( TestEntity.class );
			Root<TestEntity> root = query.from( TestEntity.class );

			query.select( root ).where( cb.equal( root.get( "parent" ) , entity ) );
			List<TestEntity> resultList = session.createQuery( query ).getResultList();

			assertEquals( 0, resultList.size() );
		} );
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends AbstractTreeableEntity<TestEntity> {
	}

	@MappedSuperclass
	public abstract static class AbstractTreeableEntity<T extends AbstractTreeableEntity<T>> {
		@Id
		@GeneratedValue
		private Long id;

		protected String name;

		@ManyToOne(fetch = FetchType.LAZY)
		protected T parent;

		@OneToMany(cascade = CascadeType.REMOVE, mappedBy = "parent")
		protected Collection<T> children;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public T getParent() {
			return parent;
		}

		public void setParent(T parent) {
			this.parent = parent;
		}

		public Collection<T> getChildren() {
			return children;
		}

		public void setChildren(Collection<T> children) {
			this.children = children;
		}
	}
}
