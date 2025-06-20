/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.ops;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.stat.spi.StatisticsImplementor;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;


@SessionFactory(generateStatistics = true)
@DomainModel(annotatedClasses = {
		OneToManyMappedByCascadeDeleteTest.Parent.class,
		OneToManyMappedByCascadeDeleteTest.Child.class
})
public class OneToManyMappedByCascadeDeleteTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testRemoveCascadeDelete(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = new Parent();
			parent.setId( 1 );
			s.persist( parent );

			Child child = new Child();
			child.setId( 2 );
			child.setParent( parent );
			s.persist( child );

			parent.children.add( child );
		} );

		getStatistics( scope ).clear();

		scope.inTransaction( s -> {
			Parent parent = s.get( Parent.class, 1 );
			s.remove( parent );
		} );

		int deletes = (int) getStatistics( scope ).getEntityDeleteCount();
		assertThat( "unexpected delete counts", deletes, is( 2 ) );
	}

	private StatisticsImplementor getStatistics(SessionFactoryScope scope) {
		return scope.getSessionFactory().getStatistics();
	}

	@Entity(name = "parent")
	public static class Parent {

		@Id
		private Integer id;
		@OneToMany(targetEntity = Child.class, mappedBy = "parent", fetch = FetchType.LAZY)
		@Cascade(CascadeType.REMOVE)
		private List<Child> children = new ArrayList<>();

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "child")
	public static class Child {

		@Id
		private Integer id;
		@ManyToOne(targetEntity = Parent.class, fetch = FetchType.LAZY)
		private Parent parent;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}
	}
}
