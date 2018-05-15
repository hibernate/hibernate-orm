/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.annotations.any;

import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Any;
import org.hibernate.annotations.AnyMetaDef;
import org.hibernate.annotations.MetaValue;
import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;
import org.junit.Test;

public class EmbeddedAnyTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ Foo.class, Bar1.class, Bar2.class };
	}

	@Test
	public void testEmbeddedAny() {
		doInJPA( this::entityManagerFactory, em -> {
			Foo foo1 = new Foo();
			foo1.setId( 1 );

			Bar1 bar1 = new Bar1();
			bar1.setId( 1 );
			bar1.setBar1( "bar 1" );
			bar1.setBarType( "1" );

			FooEmbeddable foo1Embedded = new FooEmbeddable();
			foo1Embedded.setBar( bar1 );

			foo1.setFooEmbedded( foo1Embedded );

			em.persist( bar1 );
			em.persist( foo1 );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			Foo foo2 = new Foo();
			foo2.setId( 2 );

			Bar2 bar2 = new Bar2();
			bar2.setId( 2 );
			bar2.setBar2( "bar 2" );
			bar2.setBarType( "2" );

			FooEmbeddable foo2Embedded = new FooEmbeddable();
			foo2Embedded.setBar( bar2 );

			foo2.setFooEmbedded( foo2Embedded );

			em.persist( bar2 );
			em.persist( foo2 );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			Foo foo1 = em.find( Foo.class, 1 );

			assertTrue( foo1.getFooEmbedded().getBar() instanceof Bar1 );
			assertEquals( "bar 1", ( (Bar1) foo1.getFooEmbedded().getBar() ).getBar1() );
		} );

		doInJPA( this::entityManagerFactory, em -> {
			Foo foo2 = em.find( Foo.class, 2 );

			assertTrue( foo2.getFooEmbedded().getBar() instanceof Bar2 );
			assertEquals( "bar 2", ( (Bar2) foo2.getFooEmbedded().getBar() ).getBar2() );
		} );
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

		@AnyMetaDef(idType = "integer", metaType = "string", metaValues = {
				@MetaValue(value = "1", targetEntity = Bar1.class),
				@MetaValue(value = "2", targetEntity = Bar2.class)
		})
		@Any(metaColumn = @Column(name = "bar_type"))
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
