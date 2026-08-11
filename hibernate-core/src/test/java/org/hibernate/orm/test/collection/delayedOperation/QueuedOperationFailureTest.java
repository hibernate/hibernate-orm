/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.collection.delayedOperation;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.cfg.FlushSettings;
import org.hibernate.collection.spi.AbstractPersistentCollection;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Verifies failure handling for queued inverse-collection work whose foreign-key mutation is
/// physically owned by an entity action.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = {
		QueuedOperationFailureTest.Parent.class,
		QueuedOperationFailureTest.Child.class
})
@ServiceRegistry(settings = @Setting(name = FlushSettings.FLUSH_QUEUE_TYPE, value = "graph"))
@SessionFactory
public class QueuedOperationFailureTest {
	private Long parentId;

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		final var parent = new Parent();
		parent.addChild( new Child( "Sherman" ) );
		parent.addChild( new Child( "Yogi" ) );
		scope.inTransaction( session -> session.persist( parent ) );
		parentId = parent.id;
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	void failedEntityInsertDoesNotFinalizeQueuedCollectionWork(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final Transaction transaction = session.beginTransaction();
			try {
				final var parent = session.find( Parent.class, parentId );
				assertFalse( Hibernate.isInitialized( parent.children ) );

				parent.addChild( new Child( "Sherman" ) );
				final var collection = (AbstractPersistentCollection<?>) parent.children;
				assertFalse( Hibernate.isInitialized( collection ) );
				assertTrue( collection.hasQueuedOperations() );

				assertThrows( RuntimeException.class, session::flush );

				assertTrue(
						collection.hasQueuedOperations(),
						"a failed entity-owned mutation must not successfully finalize queued collection work"
				);
			}
			finally {
				transaction.rollback();
			}
		} );
	}

	@Entity(name = "QueuedOperationFailureParent")
	public static class Parent {
		@Id
		@GeneratedValue
		private Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
		private List<Child> children = new ArrayList<>();

		void addChild(Child child) {
			children.add( child );
			child.parent = this;
		}
	}

	@Entity(name = "QueuedOperationFailureChild")
	public static class Child {
		@Id
		@GeneratedValue
		private Long id;

		@Column(nullable = false, unique = true)
		private String name;

		@ManyToOne(optional = false)
		private Parent parent;

		protected Child() {
		}

		Child(String name) {
			this.name = name;
		}
	}
}
