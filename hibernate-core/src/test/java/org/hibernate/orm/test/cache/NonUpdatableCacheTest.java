/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SessionFactory
@DomainModel(annotatedClasses = {
		NonUpdatableCacheTest.Person.class
})
public class NonUpdatableCacheTest {
	private static final long PERSON_ID = 1L;
	private static final String SSN = "1234";

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( PERSON_ID, SSN, "p1" ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Person" ).executeUpdate();
		} );
	}

	@Test
	@Jira("https://hibernate.atlassian.net/browse/HHH-2781")
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Person person = session.find( Person.class, PERSON_ID );
			person.ssn = "4321";
			person.name += " updated";
		} );
		scope.inTransaction( session -> {
			final Person person = session.find( Person.class, PERSON_ID );
			assertEquals( "p1 updated", person.getName() );
			assertEquals( SSN, person.getSsn() );
		} );
	}

	@Entity(name = "Person")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Person {

		@Id
		Long id;

		@Column(unique = true, updatable = false)
		String ssn;
		String name;


		public Person() {
		}

		public Person(final long id, String ssn, final String name) {
			this.id = id;
			this.ssn = ssn;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getSsn() {
			return ssn;
		}

	}
}
