/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.onetoone;

import java.io.Serializable;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.junit.Test;

@TestForIssue(jiraKey = "HHH-15235")
public class EmbeddedIdTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Bar.class, Foo.class };
	}

	@Test
	public void testMerge() {
		inTransaction(
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
