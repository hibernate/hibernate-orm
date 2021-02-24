/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.hibernate.Hibernate;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class ManyToOneEagerDerivedIdFetchModeJoinTest extends BaseCoreFunctionalTestCase {
	private Foo foo;

	@Test
	@TestForIssue(jiraKey = "HHH-14466")
	public void testQuery() {
		doInHibernate( this::sessionFactory, session -> {
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
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14466")
	public void testQueryById() {

		doInHibernate( this::sessionFactory, session -> {
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
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14466")
	public void testFindByPrimaryKey() {

		doInHibernate( this::sessionFactory, session -> {
			Bar newBar = session.find( Bar.class, foo.getId() );
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo() ) );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertTrue( Hibernate.isInitialized( newBar.getFoo().getBars() ) );
			assertEquals( 1, newBar.getFoo().getBars().size() );
			assertSame( newBar, newBar.getFoo().getBars().iterator().next() );
			assertEquals( "Some details", newBar.getDetails() );
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14466")
	public void testFindByInversePrimaryKey() {

		doInHibernate( this::sessionFactory, session -> {
			Foo newFoo = session.find( Foo.class, foo.getId() );
			assertNotNull( newFoo );
			assertNotNull( newFoo.getBars() );
			assertTrue( Hibernate.isInitialized( newFoo.getBars() ) );
			assertEquals( 1, newFoo.getBars().size() );
			assertSame( newFoo, newFoo.getBars().iterator().next().getFoo() );
			assertEquals( "Some details", newFoo.getBars().iterator().next().getDetails() );
		});

	}

	@Before
	public void setupData() {
		this.foo = doInHibernate( this::sessionFactory, session -> {
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
		});
	}

	@After
	public void cleanupData() {
		doInHibernate( this::sessionFactory, session -> {
			session.delete( session.find( Foo.class, foo.id ) );
		});
		this.foo = null;
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Foo.class,
				Bar.class,
		};
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
