/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.any.annotations;

import java.util.List;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyDiscriminator;
import org.hibernate.annotations.AnyDiscriminatorValue;
import org.hibernate.annotations.AnyKeyJavaClass;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@DomainModel( annotatedClasses = { EmbeddedAnyTest.Foo.class, EmbeddedAnyTest.Bar1.class, EmbeddedAnyTest.Bar2.class } )
@SessionFactory
public class EmbeddedAnyTest {

	@BeforeEach
	public void createTestData(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Foo foo1 = new Foo();
					foo1.setId( 1 );

					final Bar1 bar1 = new Bar1();
					bar1.setId( 1 );
					bar1.setBar1( "bar 1" );
					bar1.setBarType( "1" );

					final FooEmbeddable foo1Embedded = new FooEmbeddable();
					foo1Embedded.setBar( bar1 );

					foo1.setFooEmbedded( foo1Embedded );

					session.persist( bar1 );
					session.persist( foo1 );
				}
		);
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testEmbeddedAny(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final Foo foo = session.find( Foo.class, 1 );
					assertTrue( foo.getFooEmbedded().getBar() instanceof Bar1 );
					assertEquals( "bar 1", ( (Bar1) foo.getFooEmbedded().getBar() ).getBar1() );
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-16732")
	public void testEmbeddedIdHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<Foo> foos = session.createQuery( "from Foo f where id(f.fooEmbedded.bar) = 1", Foo.class )
							.list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15323")
	public void testEmbeddedTypeHql(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<Foo> foos = session.createQuery( "from Foo f where type(f.fooEmbedded.bar) = Bar1", Foo.class )
							.list();
				}
		);
	}

	@Test
	@JiraKey( value = "HHH-15323")
	public void testEmbeddedTypeHql2(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					final List<Foo> foos = session.createQuery( "from Foo f where Bar1 = type(f.fooEmbedded.bar)", Foo.class )
							.list();
				}
		);
	}

	@Entity(name = "Foo")
	public static class Foo {

		@Id
		private Integer id;

		@Embedded
		private FooEmbeddable fooEmbedded;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public FooEmbeddable getFooEmbedded() {
			return fooEmbedded;
		}

		public void setFooEmbedded(FooEmbeddable fooEmbedded) {
			this.fooEmbedded = fooEmbedded;
		}
	}

	@Embeddable
	public static class FooEmbeddable {

		@Any
		@AnyDiscriminator( DiscriminatorType.STRING )
		@AnyDiscriminatorValue( discriminator = "1", entity = Bar1.class )
		@AnyDiscriminatorValue( discriminator = "2", entity = Bar2.class )
		@Column(name = "bar_type")
		@AnyKeyJavaClass( Integer.class )
		@JoinColumn(name = "bar_id")
		private BarInt bar;

		public BarInt getBar() {
			return bar;
		}

		public void setBar(BarInt bar) {
			this.bar = bar;
		}
	}

	public interface BarInt {

		String getBarType();
	}

	@Entity(name = "Bar1")
	@Table(name = "bar")
	public static class Bar1 implements BarInt {

		@Id
		private Integer id;

		private String bar1;

		@Column(name = "bar_type")
		private String barType;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getBar1() {
			return bar1;
		}

		public void setBar1(String bar1) {
			this.bar1 = bar1;
		}

		@Override
		public String getBarType() {
			return barType;
		}

		public void setBarType(String barType) {
			this.barType = barType;
		}
	}

	@Entity(name = "Bar2")
	@Table(name = "bar")
	public static class Bar2 implements BarInt {

		@Id
		private Integer id;

		private String bar2;

		@Column(name = "bar_type")
		private String barType;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getBar2() {
			return bar2;
		}

		public void setBar2(String bar2) {
			this.bar2 = bar2;
		}

		@Override
		public String getBarType() {
			return barType;
		}

		public void setBarType(String barType) {
			this.barType = barType;
		}
	}
}
