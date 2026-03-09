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
import org.hibernate.testing.orm.junit.DialectFeatureChecks;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.RequiresDialectFeature;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SessionFactory
@DomainModel(
		annotatedClasses = {
				SelectLockingInDifferentThreadsTest.Parent.class,
				SelectLockingInDifferentThreadsTest.Child.class
		}
)
@RequiresDialectFeature(feature = DialectFeatureChecks.SupportsConcurrentTransactions.class)
public class SelectLockingInDifferentThreadsTest {

	@Test
	public void test(SessionFactoryScope scope) {
		createEntities( scope );
		for ( int i = 0; i < 3; i++ ) {
			selectEntities( scope );
		}
	}

	private void selectEntities(SessionFactoryScope scope) {
		SessionImplementor session1 = scope.getSessionFactory().openSession();
		SessionImplementor session2 = scope.getSessionFactory().openSession();

		SelectParentPessimisticLock selectParent1 = new SelectParentPessimisticLock( 1L, session1 );
		SelectParentPessimisticLock selectParent2 = new SelectParentPessimisticLock( 2L, session2 );

		try {
			selectParent1.start();
			selectParent2.start();

			selectParent1.join();
			selectParent2.join();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		finally {
			session1.close();
			session2.close();
		}

		assertThat( selectParent1.getE() ).isNull();
		assertThat( selectParent2.getE() ).isNull();

		assertThat( selectParent1.getParent() ).isNotNull();
		assertThat( selectParent1.getParent().getId() ).isEqualTo( 1L );
		assertThat( selectParent2.getParent() ).isNotNull();
		assertThat( selectParent2.getParent().getId() ).isEqualTo( 2L );
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

	private static class SelectParentPessimisticLock extends Thread {

		private final Long parentId;
		private final Session session;
		private Parent parent;
		private Exception e;

		public SelectParentPessimisticLock(Long parentId, Session session) {
			this.parentId = parentId;
			this.session = session;
		}

		@Override
		public void run() {
			session.inTransaction( transaction -> {
				try {
					this.parent = session.find( Parent.class, parentId, LockModeType.PESSIMISTIC_WRITE );
				}
				catch (Exception e) {
					this.e = e;
				}
			} );
		}

		public Exception getE() {
			return e;
		}

		public Parent getParent() {
			return parent;
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

		public Long getId() {
			return id;
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
