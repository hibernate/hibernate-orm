/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OneToOneEagerDerivedIdFetchModeSelectTest extends BaseCoreFunctionalTestCase {
	private Foo foo;

	@Test
	@TestForIssue(jiraKey = "HHH-14390")
	public void testQuery() {

		doInHibernate( this::sessionFactory, session -> {
			Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo.id = :id" )
					.setParameter( "id", foo.getId() )
					.uniqueResult();
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertEquals( "Some details", newBar.getDetails() );
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14390")
	public void testQueryById() {

		doInHibernate( this::sessionFactory, session -> {
			Bar newBar = (Bar) session.createQuery( "SELECT b FROM Bar b WHERE b.foo = :foo" )
					.setParameter( "foo", foo )
					.uniqueResult();
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertEquals( "Some details", newBar.getDetails() );
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-14390")
	public void testFindByPrimaryKey() {

		doInHibernate( this::sessionFactory, session -> {
			Bar newBar = session.find( Bar.class, foo.getId() );
			assertNotNull( newBar );
			assertNotNull( newBar.getFoo() );
			assertEquals( foo.getId(), newBar.getFoo().getId() );
			assertEquals( "Some details", newBar.getDetails() );
		});
	}

	@Before
	public void setupData() {
		this.foo = doInHibernate( this::sessionFactory, session -> {
			Foo foo = new Foo();
			session.persist( foo );

			Bar bar = new Bar();
			bar.setFoo( foo );
			bar.setDetails( "Some details" );

			foo.setBar( bar );

			session.persist( bar );

			session.flush();

			assertNotNull( foo.getId() );
			assertEquals( foo.getId(), bar.getFoo().getId() );

			return foo;
		});
	}

	@After
	public void cleanupData() {
		this.foo = null;
		doInHibernate( this::sessionFactory, session -> {
			session.createQuery( "delete from Bar" );
			session.createQuery( "delete from Foo" );
		});
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Foo.class,
				Bar.class,
		};
	}

	@Entity(name = "Foo")
	public static class Foo implements Serializable {

		@Id
		@GeneratedValue
		private Long id;

		private String name;

		@OneToOne(mappedBy = "foo", cascade = CascadeType.ALL)
		private Bar bar;

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
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
