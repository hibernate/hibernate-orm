/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.subselect;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = {
				SubselectAndSingleAttributeIdClassTest.MyEntity.class,
				SubselectAndSingleAttributeIdClassTest.MyChild.class,
				SubselectAndSingleAttributeIdClassTest.MyGrandchild.class,
		}
)
@SessionFactory
@JiraKey( "HHH-17221" )
public class SubselectAndSingleAttributeIdClassTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			MyEntity entity = new MyEntity( 1, List.of(
					new MyChild( 11, List.of( new MyGrandchild( 111 ), new MyGrandchild( 112 ) ) ),
					new MyChild( 12, List.of( new MyGrandchild( 121 ), new MyGrandchild( 122 ) ) )
			) );
			session.persist( entity );
			entity.getChildren().forEach( c -> {
				session.persist( c );
				c.getGrandchildren().forEach( session::persist );
			} );
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope factoryScope) {
		factoryScope.dropData();
	}

	@Test
	public void testSelect(SessionFactoryScope scope) {
		scope.inTransaction(session -> {
			TypedQuery<MyEntity> query = session.createQuery(
					"select e from MyEntity e where id = 1",
					MyEntity.class
			);
			for ( MyEntity entity : query.getResultList() ) {
				assertNotNull( entity.getChildren(), "Children are null" );
				for ( MyChild child : entity.getChildren() ) {
					assertFalse( child.getGrandchildren().isEmpty(), "GRANDCHILDRENDS are empty" );
				}
			}
		} );
	}


	@Entity(name = "MyEntity")
	@IdClass(MyEntityId.class)
	@Table(name = "MY_ENTITY")
	public static class MyEntity {

		@Id
		@Column(name = "ID", nullable = false, precision = 9)
		private Integer id;

		@OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
		private List<MyChild> children = new ArrayList<>();

		public MyEntity() {
		}

		public MyEntity(int id, List<MyChild> children) {
			this.id = id;
			this.children = new ArrayList<>( children );
			this.children.forEach( c -> c.setParent( this ) );
		}

		public List<MyChild> getChildren() {
			return children;
		}
	}

	public static class MyEntityId {

		@Column(name = "ID", nullable = false, precision = 9)
		private Integer id;

		public MyEntityId() {
		}

		public MyEntityId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MyEntityId that = (MyEntityId) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}
	}


	@Entity(name = "MyChild")
	@IdClass(MyChildId.class)
	@Table(name = "MY_CHILD")
	public static class MyChild {

		@Id
		@Column(name = "ID", nullable = false, precision = 9)
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private MyEntity parent;

		@OneToMany(mappedBy = "child")
		@Fetch(FetchMode.SUBSELECT)
		private List<MyGrandchild> grandchildren = new ArrayList<>();

		public MyChild() {
		}

		public MyChild(int id, List<MyGrandchild> grandchildren) {
			this.id = id;
			this.grandchildren = new ArrayList<>( grandchildren );
			this.grandchildren.forEach( g -> g.setChild( this ) );
		}

		public void setParent(MyEntity parent) {
			this.parent = parent;
		}

		public List<MyGrandchild> getGrandchildren() {
			return grandchildren;
		}

	}

	public static class MyChildId {

		@Column(name = "ID", nullable = false, precision = 9)
		private Integer id;

		public MyChildId() {
		}

		public MyChildId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MyChildId that = (MyChildId) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}

	}

	@Entity(name = "MyGrandchild")
	@IdClass(MyGrandchildId.class)
	@Table(name = "MY_GRANDCHILD")
	public static class MyGrandchild {

		@Id
		@Column(name = "ID", nullable = false, precision = 9)
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private MyChild child;

		public MyGrandchild() {
		}

		public MyGrandchild(int id) {
			this.id = id;
		}

		public void setChild(MyChild child) {
			this.child = child;
		}
	}

	public static class MyGrandchildId {

		@Column(name = "ID", nullable = false, precision = 9)
		private Integer id;

		public MyGrandchildId() {
		}

		public MyGrandchildId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			MyGrandchildId that = (MyGrandchildId) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}

	}

}
