/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.Hibernate.isInitialized;
import static org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils.checkDirtyTracking;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@JiraKey("HHH-12260")
@DomainModel(
		annotatedClasses = {
				LazyCollectionDetachTest.Parent.class, LazyCollectionDetachTest.Child.class
		}
)
@SessionFactory
@BytecodeEnhanced
public class LazyCollectionDetachTest {

	private static final int CHILDREN_SIZE = 10;
	private Long parentID;

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = new Parent();
			parent.setChildren( new ArrayList<>() );
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child();
				child.parent = parent;
				s.persist( child );
			}
			s.persist( parent );
			parentID = parent.id;
		} );
	}

	@AfterEach
	void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testDetach(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = s.find( Parent.class, parentID );

			assertThat( parent, notNullValue() );
			assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
			assertFalse( isInitialized( parent.children ) );
			checkDirtyTracking( parent );

			s.detach( parent );

			s.flush();
		} );
	}

	@Test
	public void testDetachProxy(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = s.getReference( Parent.class, parentID );

			checkDirtyTracking( parent );

			s.detach( parent );

			s.flush();
		} );
	}

	@Test
	public void testRefresh(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Parent parent = s.find( Parent.class, parentID );

			assertThat( parent, notNullValue() );
			assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
			assertFalse( isInitialized( parent.children ) );
			checkDirtyTracking( parent );

			s.refresh( parent );

			s.flush();
		} );
	}


	@Entity(name = "Parent")
	@Table(name = "PARENT")
	static class Parent {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
		List<Child> children;

		void setChildren(List<Child> children) {
			this.children = children;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	static class Child {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		Parent parent;

		Child() {
		}
	}
}
