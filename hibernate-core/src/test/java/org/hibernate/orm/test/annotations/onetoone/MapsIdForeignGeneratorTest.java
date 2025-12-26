/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DomainModel(
		annotatedClasses = {
				MapsIdForeignGeneratorTest.Foo.class,
				MapsIdForeignGeneratorTest.Bar.class,
		}
)
@SessionFactory
@Jira("https://hibernate.atlassian.net/browse/HHH-13063")
public class MapsIdForeignGeneratorTest {

	@Test
	public void testOneToOneWithIdNamedId(SessionFactoryScope scope) {
		final Long fooId = 500L;
		scope.inTransaction( session -> {
			Foo foo = new Foo();
			foo.setId(fooId);
			foo.setName("Foo1");
			session.persist(foo);
		} );
		scope.inTransaction( session -> {
				Foo foo = session.find(Foo.class, fooId);

				Bar bar = foo.getBar();
				if (bar == null) {
					bar = new Bar(foo);
					foo.setBar(bar);
				}
				bar.setName("Bar2");
				session.merge(foo);
		} );
		scope.inTransaction( session -> {
				Foo foo = session.find(Foo.class, fooId);
				assertNotNull(foo.getBar());
		} );
	}

	@AfterEach
	public void cleanupData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}


	@Entity
	@Table(name = "TEST_FOO")
	public static class Foo {

		private Long id;
		private String name;
		private Bar bar;

		@Id
		@Column(name = "ID")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@Column(name = "name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "foo_id")
		public Bar getBar() {
			return bar;
		}

		public void setBar(Bar bar) {
			this.bar = bar;
		}
	}

	@Entity
	@Table(name = "TEST_BAR")
	public static class Bar {

		private Long id;
		private Foo foo;
		private String name;

		public Bar() {
		}

		Bar(Foo foo) {
			this.foo = foo;
		}

		@Id
		@GeneratedValue(generator = "foreign")
		@GenericGenerator(name = "foreign", strategy = "foreign", parameters = {
				@org.hibernate.annotations.Parameter(name = "property", value = "foo")
		})
		@Column(name = "foo_id")
		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		@OneToOne(mappedBy = "bar")
		@MapsId("foo_id")
		public Foo getFoo() {
			return foo;
		}

		public void setFoo(Foo foo) {
			this.foo = foo;
		}

		@Column(name = "name")
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
