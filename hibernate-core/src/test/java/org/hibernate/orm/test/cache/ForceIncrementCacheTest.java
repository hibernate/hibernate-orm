/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.cache;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;
import org.hibernate.LockMode;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		ForceIncrementCacheTest.Person.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-9127")
public class ForceIncrementCacheTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.persist( new Person( 1L, "Marco" ) );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testOptimisticForceIncrementOnLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L, LockModeType.OPTIMISTIC_FORCE_INCREMENT );
			assertThat( entity.getVersion() ).isEqualTo( 0L );
		} );
		// in a different transaction
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L );
			assertThat( entity.getVersion() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testPessimisticForceIncrementOnLoad(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L );
			assertThat( entity.getVersion() ).isEqualTo( 0L );
		} );
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L, LockModeType.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.getVersion() ).isEqualTo( 1L );
		} );
		// in a different transaction
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L );
			assertThat( entity.getVersion() ).isEqualTo( 1L );
		} );
	}

	@Test
	public void testForceIncrementOnLock(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L );
			assertThat( entity.getVersion() ).isEqualTo( 0L );
			session.lock( entity, LockMode.PESSIMISTIC_FORCE_INCREMENT );
			assertThat( entity.getVersion() ).isEqualTo( 1L );
		} );
		// in a different transaction
		scope.inTransaction( session -> {
			Person entity = session.find( Person.class, 1L );
			assertThat( entity.getVersion() ).isEqualTo( 1L );
		} );
	}

	@Entity(name = "Person")
	@Cacheable
	@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
	public static class Person {

		@Id
		private Long id;
		@Version
		private Long version;
		private String name;

		public Person() {
		}

		public Person(final long id, final String name) {
			setId( id );
			setName( name );
		}

		public Long getId() {
			return id;
		}

		public void setId(final Long id) {
			this.id = id;
		}

		public Long getVersion() {
			return version;
		}

		public void setVersion(Long version) {
			this.version = version;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

	}
}
