/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.loading.multiLoad;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.CacheMode;
import org.hibernate.Hibernate;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import static jakarta.persistence.GenerationType.AUTO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Steve Ebersole
 * @author Gail Badner
 */
@DomainModel(annotatedClasses = {
		MultiLoadSubSelectCollectionTest.Parent.class,
		MultiLoadSubSelectCollectionTest.Child.class
})
@SessionFactory(generateStatistics = true)
public class MultiLoadSubSelectCollectionTest {

	@BeforeEach
	public void before(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.setCacheMode( CacheMode.IGNORE );
			for ( int i = 1; i <= 60; i++ ) {
				final Parent p = new Parent( i, "Entity #" + i );
				for ( int j = 0; j < i; j++ ) {
					Child child = new Child();
					child.setParent( p );
					p.getChildren().add( child );
				}
				session.persist( p );
			}
		} );
	}

	@AfterEach
	public void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-12740")
	public void testSubselect(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					List<Parent> list = session.byMultipleIds( Parent.class ).multiLoad( ids( 56 ) );
					assertEquals( 56, list.size() );

					// None of the collections should be loaded yet
					for ( Parent p : list ) {
						assertFalse( Hibernate.isInitialized( p.children ) );
					}

					// When the first collection is loaded, the full collection
					// should be loaded.
					Hibernate.initialize( list.get( 0 ).children );

					for ( int i = 0; i < 56; i++ ) {
						assertTrue( Hibernate.isInitialized( list.get( i ).children ) );
						assertEquals( i + 1, list.get( i ).children.size() );
					}
				}
		);
	}

	private Integer[] ids(int count) {
		Integer[] ids = new Integer[count];
		for ( int i = 1; i <= count; i++ ) {
			ids[i - 1] = i;
		}
		return ids;
	}

	@Entity(name = "Parent")
	@Table(name = "Parent")
	@BatchSize(size = 15)
	public static class Parent {
		Integer id;
		String text;
		private List<Child> children = new ArrayList<>();

		public Parent() {
		}

		public Parent(Integer id, String text) {
			this.id = id;
			this.text = text;
		}

		@Id
		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
		@Fetch(FetchMode.SUBSELECT)
		public List<Child> getChildren() {
			return children;
		}

		public void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		@GeneratedValue(strategy = AUTO)
		private int id;

		@ManyToOne(fetch = FetchType.LAZY, optional = true)
		private Parent parent;

		public Child() {
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public int getId() {
			return id;
		}
	}
}
