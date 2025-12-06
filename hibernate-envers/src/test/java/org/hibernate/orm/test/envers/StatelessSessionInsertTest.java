/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.envers.Audited;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Jpa(annotatedClasses = StatelessSessionInsertTest.ToDo.class)
public class StatelessSessionInsertTest {

	/**
	 * Reproduces the NullPointerException in Envers when insert() is called
	 * on a stateless session.
	 * <p>
	 * Because of this error, Envers is not currently compatible with Jakarta Data
	 * Repositories as the Hibernate metamodel generator generates Jakarta Data Repository
	 * implementations with stateless sessions.
	 * <p>
	 * This test throws a NullPointerException with the message
	 * "Cannot invoke \"org.hibernate.engine.spi.SessionImplementor.isTransactionInProgress()\"
	 * because \"session\" is null"
	 */
	@Test
	public void test(EntityManagerFactoryScope scope) {
		scope.inEntityManager( entityManager -> {
			Session session = entityManager.unwrap( Session.class );
			SessionFactory sessionFactory = session.getSessionFactory();

			try (StatelessSession statelessSession = sessionFactory.openStatelessSession()) {
				ToDo todo = new ToDo();
				todo.setId( 1L );
				todo.setTask( "Reproduce the Envers NullPointerException error" );

				// insert() triggers the EnversPostInsertEventListenerImpl event listener,
				// which tries to call session.isTransactionInProgress() where 'session' is null.
				statelessSession.insert( todo );
			}
		} );
	}

	@Entity
	@Audited
	public static class ToDo {
		@Id
		private Long id;

		private String task;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getTask() {
			return task;
		}

		public void setTask(String task) {
			this.task = task;
		}
	}
}
