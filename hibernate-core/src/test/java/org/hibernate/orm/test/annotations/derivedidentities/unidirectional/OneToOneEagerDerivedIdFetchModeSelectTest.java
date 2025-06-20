/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.unidirectional;

import java.io.Serializable;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				OneToOneEagerDerivedIdFetchModeSelectTest.Foo.class,
				OneToOneEagerDerivedIdFetchModeSelectTest.Bar.class,
		}
)
@SessionFactory
public class OneToOneEagerDerivedIdFetchModeSelectTest {
	private Foo foo;

	@Test
	@JiraKey(value = "HHH-14390")
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo.id = :id" )
					.setParameter( "id", foo.getId() )
					.uniqueResult();
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertEquals( "Some details", newBar.getDetails() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-14390")
	public void testQueryById(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo = :foo" )
					.setParameter( "foo", foo )
					.uniqueResult();
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertEquals( "Some details", newBar.getDetails() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-14390")
	public void testFindByPrimaryKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar newBar = session.find( Bar.class, foo.getId() );
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertEquals( "Some details", newBar.getDetails() );
		} );
	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		this.foo = scope.fromTransaction( session -> {
			Foo foo = new Foo();
			session.persist( foo );

			Bar bar = new Bar();
			bar.setFoo( foo );
			bar.setDetails( "Some details" );
			session.persist( bar );

			session.flush();

			assertNotNull( foo.getId() );
			assertEquals( foo.getId(), bar.getFoo().getId() );

			return foo;
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		this.foo = null;
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Entity(name = "Foo")
	public static class Foo implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}
	}

	@Entity(name = "Bar")
	public static class Bar implements Serializable {

		@Id
		@OneToOne(fetch = FetchType.EAGER)
		@Fetch(FetchMode.SELECT)
		@JoinColumn(name = "BAR_ID")
		private Foo foo;

		private String details;

		public Foo getFoo() {
			return foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}

		public String getDetails() {
			return details;
		}

		public void setDetails(String details) {
			this.details = details;
		}
	}
}
