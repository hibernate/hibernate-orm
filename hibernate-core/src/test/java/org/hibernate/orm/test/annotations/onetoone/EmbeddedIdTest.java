/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.onetoone;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@JiraKey(value = "HHH-15235")
@DomainModel(
		annotatedClasses = {
				EmbeddedIdTest.Bar.class,
				EmbeddedIdTest.Foo.class
		}
)
@SessionFactory
public class EmbeddedIdTest {

	@Test
	public void testMerge(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					FooId fooId = new FooId();
					fooId.id = "foo";
					Foo foo = new Foo();
					foo.id = fooId;
					Bar bar = new Bar();
					BarId barId = new BarId();
					barId.id = 1l;
					bar.id = barId;
					foo.bar = bar;
					bar.foo = foo;
					session.merge( foo );
				}
		);
	}

	@Embeddable
	public static class BarId implements Serializable {
		private Long id;
	}

	@Embeddable
	public static class FooId implements Serializable {
		private String id;
	}

	@Entity(name = "Bar")
	@Table(name = "BAR_TABLE")
	public static class Bar {
		@EmbeddedId
		private BarId id;

		private String name;

		@OneToOne(mappedBy = "bar")
		private Foo foo;
	}

	@Entity(name = "Foo")
	@Table(name = "FOO_TABLE")
	public static class Foo {
		@EmbeddedId
		private FooId id;

		private String name;

		@OneToOne(cascade = CascadeType.ALL)
		@JoinColumn(name = "bar_id")
		private Bar bar;
	}
}
