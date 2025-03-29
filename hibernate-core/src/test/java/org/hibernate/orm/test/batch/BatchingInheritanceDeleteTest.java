/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToMany;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;

/**
 * @author Vlad Mihalcea
 */
@JiraKey( value = "HHH-12470" )
public class BatchingInheritanceDeleteTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[]{
				Foo.class,
				Bar.class,
				Baz.class
		};
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, 25 );
	}

	@Test
	//@FailureExpected( jiraKey = "HHH-12470" )
	public void testDelete() {
		doInHibernate( this::sessionFactory, s -> {
			Bar bar = new Bar("bar");

			Foo foo = new Foo("foo");
			foo.setBar(bar);

			s.persist(foo);
			s.persist(bar);

			s.flush();

			s.remove(foo);
			s.remove(bar);

			s.flush();

			assertThat(s.find(Foo.class, foo.getId()), nullValue());
			assertThat(s.find(Bar.class, bar.getId()), nullValue());
		} );
	}

	@Entity(name = "Bar")
	public static class Bar extends AbstractBar {

		@Column(nullable = false)
		private String name;

		@OneToMany(cascade = CascadeType.ALL)
		List<Baz> bazList = new ArrayList<>();

		public Bar() {
			super();
		}

		public Bar(final String name) {
			super();
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public List<Baz> getBazList() {
			return bazList;
		}

		public void setBazList(final List<Baz> bazList) {
			this.bazList = bazList;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("Bar [name=").append(name).append("]");
			return builder.toString();
		}
	}

	@Entity(name = "Baz")
	public static class Baz {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;

		@Column(nullable = false)
		private String name;

		public Baz() {
			super();
		}

		public Baz(final String name) {
			super();
			this.name = name;
		}

		public long getId() {
			return id;
		}

		public void setId(final long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			final StringBuilder builder = new StringBuilder();
			builder.append("Bar [name=").append(name).append("]");
			return builder.toString();
		}
	}

	@Entity(name = "Foo")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class Foo extends AbstractFoo {

		public Foo() {
			super();
		}

		public Foo(final String name) {
			super();
			this.name = name;
		}

		@Column(name = "NAME")
		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "BAR_ID")
		private Bar bar;

		public Bar getBar() {
			return bar;
		}

		public void setBar(final Bar bar) {
			this.bar = bar;
		}

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}
	}

	@Entity(name = "AbstractBar")
	@Inheritance(strategy = InheritanceType.JOINED)
	public static class AbstractBar {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;

		public long getId() {
			return id;
		}

		public void setId(final long id) {
			this.id = id;
		}
	}

	@MappedSuperclass
	public static class AbstractFoo {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		private long id;

		public long getId() {
			return id;
		}

		public void setId(final long id) {
			this.id = id;
		}
	}
}
