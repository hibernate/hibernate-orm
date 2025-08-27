/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.compliance.callback.listener;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Query;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Jpa(
		annotatedClasses = MultiListenersAppliedTest.Person.class,
		xmlMappings = "org/hibernate/orm/test/jpa/compliance/callback/listener/orm.xml"
)
public class MultiListenersAppliedTest {

	@Test
	public void postLoadMultiTest(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Person person = new Person( 1, "Fab" );
					entityManager.persist( person );
					entityManager.flush();
					entityManager.refresh( person );
					final Query query = entityManager
							.createQuery( "select p from Person p" );
					query.getResultList();

					final List<String> actual = person.getPostLoadCalls();

					assertThat( actual.size(), is( 3 ) );
					assertTrue(
							actual.contains( "AnotherPersonListener" ),
							"AnotherPersonListener#postLoad has not been called"
					);
					assertTrue(
							actual.contains( "LastPersonListener" ),
							"LastPersonListener#postLoad has not been called"
					);
					assertTrue(
							actual.contains( "PersonListener" ),
							"PersonListener#postLoad has not been called"
					);
				}
		);
	}

	public static class PersonCallback {

		boolean isPostPersistCalled;

		private List<String> postLoadCalls = new ArrayList();

		public void setPostPersistCalled() {
			this.isPostPersistCalled = true;
		}

		public boolean isPostLoadCalled() {
			return isPostPersistCalled;
		}

		public List<String> getPostLoadCalls() {
			return postLoadCalls;
		}

		public void addPostLoadCall(String name) {
			postLoadCalls.add( name );
		}
	}

	@Entity(name = "Person")
	@EntityListeners({ AnotherPersonListener.class, LastPersonListener.class })
	public static class Person extends PersonCallback {

		Person() {
		}

		public Person(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		@Id
		private Integer id;

		private String name;
	}

	public static class AnotherPersonListener {

		@PostLoad
		protected void postLoad(PersonCallback callback) {
			callback.addPostLoadCall( "AnotherPersonListener" );

			callback.setPostPersistCalled();
		}

	}

	public static class LastPersonListener {

		@PostLoad
		protected void postLoad(PersonCallback callback) {
			callback.addPostLoadCall( "LastPersonListener" );
			callback.setPostPersistCalled();
		}

	}

}
