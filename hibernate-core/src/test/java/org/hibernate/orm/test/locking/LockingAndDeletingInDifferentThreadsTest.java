/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.locking;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToMany;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SessionFactory
@DomainModel(
		annotatedClasses = {
				LockingAndDeletingInDifferentThreadsTest.Parent.class,
				LockingAndDeletingInDifferentThreadsTest.Child.class
		}
)
public class LockingAndDeletingInDifferentThreadsTest {


	@Test
	public void test(SessionFactoryScope scope) {
		for ( int i = 0; i < 3; i++ ) {
			createEntities( scope );
			deleteEntities( scope );
		}
	}

	private void deleteEntities(SessionFactoryScope scope) {
		SessionImplementor session1 = scope.getSessionFactory().openSession();
		SessionImplementor session2 = scope.getSessionFactory().openSession();

		DeleteParent deleteParent1 = new DeleteParent( 1L, session1 );
		DeleteParent deleteParent2 = new DeleteParent( 2L, session2 );

		try {
			deleteParent1.start();
			deleteParent2.start();

			deleteParent1.join();
			deleteParent2.join();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		finally {
			session1.close();
			session2.close();
		}

		assertThat( deleteParent1.getE() ).isNull();
		assertThat( deleteParent2.getE() ).isNull();
	}

	private static void createEntities(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Parent parent = new Parent( 1L, "parent" );
					parent.addChild( new Child( 4L, "child1" ) );
					session.persist( parent );

					Parent parent2 = new Parent( 2L, "parent2" );
					parent2.addChild( new Child( 5L, "child2" ) );
					session.persist( parent2 );
				}
		);
	}

	private class DeleteParent extends Thread {

		private final Long parentId;
		private final Session session;
		private Exception e;

		public DeleteParent(Long parentId, Session session) {
			this.parentId = parentId;
			this.session = session;
		}

		@Override
		public void run() {
			session.inTransaction( transaction -> {
				try {
					Parent parent = session.find( Parent.class, parentId, LockModeType.PESSIMISTIC_WRITE );
					session.remove( parent );
				}
				catch (Exception e) {
					this.e = e;
				}
			} );
		}

		public Exception getE() {
			return e;
		}
	}

	@Entity(name = "Parent")
	public static class Parent {

		@Id
		protected Long id;

		protected String name;

		@OneToMany(cascade = {CascadeType.REMOVE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
		Collection<Child> children = new ArrayList<>();

		public Parent() {

		}

		public Parent(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public Collection<Child> getChildren() {
			return children;
		}

		public void addChild(Child child) {
			children.add( child );
		}
	}

	@Entity(name = "Child")
	public static class Child {

		@Id
		protected Long id;

		protected String name;

		public Child() {
		}

		public Child(Long id, String name) {
			this.id = id;
			this.name = name;
		}

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

	}
}
