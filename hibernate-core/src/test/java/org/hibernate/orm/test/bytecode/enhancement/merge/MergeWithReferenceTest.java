package org.hibernate.orm.test.bytecode.enhancement.merge;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.assertj.core.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(BytecodeEnhancerRunner.class)
public class MergeWithReferenceTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Foo.class,
				Bar.class
		};
	}

	@After
	public void tearDown() {
		inTransaction(
				session -> {
					session.createMutationQuery( "delete from Foo" ).executeUpdate();
					session.createMutationQuery( "delete from Bar" ).executeUpdate();
				}
		);
	}

	@Test
	public void testMergeReference() {
		Bar bar = new Bar( "unique3" );
		inTransaction( session -> {
			session.persist( bar );
		} );

		inTransaction( session -> {
			Bar reference = session.getReference( Bar.class, bar.getId() );
			Foo merged = session.merge( new Foo( reference ) );
			Assertions.assertThat( merged.getBar().getKey() ).isEqualTo( bar.getKey() );
		} );
	}

	@Test
	public void testMergeReference2() {
		Bar bar = new Bar( "unique3" );
		inTransaction( session -> {
			session.persist( bar );
		} );

		inTransaction( session -> {
			Bar reference = session.getReference( Bar.class, bar.getId() );
			reference.setName( "Davide" );
			session.merge( reference );
		} );

		inTransaction(
				session -> {
					Bar reference = session.getReference( Bar.class, bar.getId() );
					assertThat( reference.getName() ).isEqualTo( "Davide" );
				}
		);
	}

	@Entity(name = "Foo")
	public static class Foo {
		Foo(Bar bar) {
			this.bar = bar;
		}

		public Foo() {
		}

		@Id
		@GeneratedValue
		long id;

		@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
		@Fetch(FetchMode.JOIN)
		@JoinColumn(name = "bar_key", referencedColumnName = "nat_key")
		Bar bar;

		public long getId() {
			return id;
		}

		public void setId(long id) {
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
	public static class Bar {
		Bar(String key) {
			this.key = key;
		}

		public Bar() {
		}

		@Id
		@GeneratedValue
		long id;

		@Column(name = "nat_key", unique = true)
		String key;

		String name;

		public long getId() {
			return id;
		}

		public void setId(long id) {
			this.id = id;
		}

		public String getKey() {
			return key;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}
}
