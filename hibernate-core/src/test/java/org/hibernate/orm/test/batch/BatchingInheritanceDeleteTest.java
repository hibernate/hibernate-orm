/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.batch;

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
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Vlad Mihalcea
 */
@JiraKey(value = "HHH-12470")
@DomainModel(
		annotatedClasses = {
				BatchingInheritanceDeleteTest.Foo.class,
				BatchingInheritanceDeleteTest.Bar.class,
				BatchingInheritanceDeleteTest.Baz.class
		}
)
@SessionFactory
@ServiceRegistry(
		settings = {
				@Setting(name = AvailableSettings.STATEMENT_BATCH_SIZE, value = "25")
		}
)
public class BatchingInheritanceDeleteTest {

	@Test
	//@FailureExpected( jiraKey = "HHH-12470" )
	public void testDelete(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			Bar bar = new Bar( "bar" );

			Foo foo = new Foo( "foo" );
			foo.setBar( bar );

			s.persist( foo );
			s.persist( bar );

			s.flush();

			s.remove( foo );
			s.remove( bar );

			s.flush();

			assertThat( s.find( Foo.class, foo.getId() ) ).isNull();
			assertThat( s.find( Bar.class, bar.getId() ) ).isNull();
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
			builder.append( "Bar [name=" ).append( name ).append( "]" );
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
			builder.append( "Bar [name=" ).append( name ).append( "]" );
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
