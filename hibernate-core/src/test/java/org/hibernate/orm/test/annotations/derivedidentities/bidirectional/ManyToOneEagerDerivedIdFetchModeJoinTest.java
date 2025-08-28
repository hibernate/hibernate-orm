/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DomainModel(
		annotatedClasses = {
				ManyToOneEagerDerivedIdFetchModeJoinTest.Foo.class,
				ManyToOneEagerDerivedIdFetchModeJoinTest.Bar.class,
		}
)
@SessionFactory
public class ManyToOneEagerDerivedIdFetchModeJoinTest {
	private Foo foo;

	@Test
	@JiraKey(value = "HHH-14466")
	public void testQuery(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo.id = :id" )
					.setParameter( "id", foo.getId() )
					.uniqueResult();
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo() ) );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo().getBars() ) );
			assertEquals( 1, newBar.getFoo().getBars().size() );
			assertSame( newBar, newBar.getFoo().getBars().iterator().next() );
			assertEquals( "Some details", newBar.getDetails() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-14466")
	public void testQueryById(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo = :foo" )
					.setParameter( "foo", foo )
					.uniqueResult();
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo() ) );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo().getBars() ) );
			assertEquals( 1, newBar.getFoo().getBars().size() );
			assertSame( newBar, newBar.getFoo().getBars().iterator().next() );
			assertEquals( "Some details", newBar.getDetails() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-14466")
	public void testFindByPrimaryKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Bar newBar = session.find( Bar.class, foo.getId() );
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo() ) );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo().getBars() ) );
			assertEquals( 1, newBar.getFoo().getBars().size() );
			assertSame( newBar, newBar.getFoo().getBars().iterator().next() );
			assertEquals( "Some details", newBar.getDetails() );
		} );
	}

	@Test
	@JiraKey(value = "HHH-14466")
	public void testFindByInversePrimaryKey(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			Foo newFoo = session.find( Foo.class, foo.getId() );
			assertNotNull( newFoo );
			assertNotNull( newFoo.getBars() );
			assertTrue( Hibernate.isInitialized( newFoo.getBars() ) );
			assertEquals( 1, newFoo.getBars().size() );
			assertSame( newFoo, newFoo.getBars().iterator().next().getFoo() );
			assertEquals( "Some details", newFoo.getBars().iterator().next().getDetails() );
		} );

	}

	@BeforeEach
	public void setupData(SessionFactoryScope scope) {
		this.foo = scope.fromTransaction( session -> {
			Foo foo = new Foo();
			foo.id = 1L;
			session.persist( foo );

			Bar bar = new Bar();
			bar.setFoo( foo );
			bar.setDetails( "Some details" );

			foo.getBars().add( bar );

			session.persist( bar );

			session.flush();

			assertNotNull( foo.getId() );
			assertEquals( foo.getId(), bar.getFoo().getId() );

			return foo;
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.remove( session.find( Foo.class, foo.id ) );
		} );
		this.foo = null;
	}

	@Entity(name = "Foo")
	public static class Foo {

		@Id
		private Long id;

		private String name;

		@OneToMany(mappedBy = "foo", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
		private Set<Bar> bars = new HashSet<>();

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<Bar> getBars() {
			return bars;
		}

		public void setBars(Set<Bar> bars) {
			this.bars = bars;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Foo foo = (Foo) o;
			return id.equals( foo.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}
	}

	@Entity(name = "Bar")
	public static class Bar implements Serializable {

		@Id
		@ManyToOne(fetch = FetchType.EAGER)
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

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			Bar bar = (Bar) o;
			return foo.equals( bar.foo );
		}

		@Override
		public int hashCode() {
			return Objects.hash( foo );
		}
	}
}
