/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.selectbeforeupdate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Version;

import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.transaction.TransactionUtil;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Chris Cranford
 */
public class UpdateDetachedTest extends BaseCoreFunctionalTestCase{

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Foo.class, Bar.class };
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedUnchanged() {
		final Bar bar = new Bar( 1, "Bar" );
		final Foo foo = new Foo( 1, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this shouldn't generate a new version.
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 0 ), foo.getVersion() );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedChanged() {
		final Bar bar = new Bar( 2, "Bar" );
		final Foo foo = new Foo( 2, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedUnchangedAndChanged() {
		final Bar bar = new Bar( 3, "Bar" );
		final Foo foo = new Foo( 3, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this shouldn't generate a new version.
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.update( foo );
		} );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-5908")
	public void testUpdateDetachedChangedAndUnchanged() {
		final Bar bar = new Bar( 4, "Bar" );
		final Foo foo = new Foo( 4, "Foo", bar );

		// this should generate versions
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.save( bar );
			session.save( foo );
		} );

		// this should generate a new version
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			foo.setName( "FooChanged" );
			session.update( foo );
		} );

		// this shouldn't generate a new version.
		TransactionUtil.doInHibernate( this::sessionFactory, session -> {
			session.update( foo );
		} );

		assertEquals( Integer.valueOf( 0 ), bar.getVersion() );
		assertEquals( Integer.valueOf( 1 ), foo.getVersion() );
	}

	@Entity(name = "Foo")
	@SelectBeforeUpdate
	public static class Foo {
		@Id
		private Integer id;
		private String name;
		@Version
		private Integer version;
		@ManyToOne
		@JoinColumn(updatable = false)
		private Bar bar;

		Foo() {

		}

		Foo(Integer id, String name, Bar bar) {
			this.id = id;
			this.name = name;
			this.bar = bar;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}
	}

	@Entity(name = "Bar")
	public static class Bar {
		@Id
		private Integer id;
		private String name;
		@Version
		private Integer version;

		Bar() {

		}

		Bar(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Integer getVersion() {
			return version;
		}

		public void setVersion(Integer version) {
			this.version = version;
		}
	}
}
