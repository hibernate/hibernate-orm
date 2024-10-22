/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.inheritance;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marco Belladelli
 */
@SessionFactory( useCollectingStatementInspector = true )
@DomainModel( annotatedClasses = {
		JoinedInheritanceTreatQueryTest.Product.class,
		JoinedInheritanceTreatQueryTest.ProductOwner.class,
		JoinedInheritanceTreatQueryTest.ProductOwner1.class,
		JoinedInheritanceTreatQueryTest.ProductOwner2.class,
		JoinedInheritanceTreatQueryTest.Description.class,
} )
@Jira( "https://hibernate.atlassian.net/browse/HHH-16574" )
@Jira( "https://hibernate.atlassian.net/browse/HHH-18745" )
public class JoinedInheritanceTreatQueryTest {
	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final Description description = new Description( "description" );
			session.persist( description );
			final ProductOwner1 own1 = new ProductOwner1( description );
			session.persist( own1 );
			session.persist( new Product( own1 ) );
			final ProductOwner2 own2 = new ProductOwner2( "basic_prop" );
			session.persist( own2 );
			session.persist( new Product( own2 ) );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			session.createMutationQuery( "delete from Product" ).executeUpdate();
			session.createMutationQuery( "delete from ProductOwner" ).executeUpdate();
			session.createMutationQuery( "delete from Description" ).executeUpdate();
		} );
	}

	@Test
	public void testTreatedJoin(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Product result = session.createQuery(
					"from Product p " +
							"join treat(p.owner AS ProductOwner1) as own1 " +
							"join own1.description",
					Product.class
			).getSingleResult();
			assertThat( result.getOwner() ).isInstanceOf( ProductOwner1.class );
			assertThat( ( (ProductOwner1) result.getOwner() ).getDescription().getText() ).isEqualTo( "description" );
			inspector.assertNumberOfJoins( 0, 3 );
		} );
	}

	@Test
	public void testImplicitTreatedJoin(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Product result = session.createQuery(
					"from Product p " +
							"join treat(p.owner as ProductOwner1).description",
					Product.class
			).getSingleResult();
			assertThat( result.getOwner() ).isInstanceOf( ProductOwner1.class );
			assertThat( ( (ProductOwner1) result.getOwner() ).getDescription().getText() ).isEqualTo( "description" );
			inspector.assertNumberOfJoins( 0, 3 );
		} );
	}

	@Test
	public void testTreatedRoot(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final ProductOwner result = session.createQuery(
					"from ProductOwner owner " +
							"join treat(owner as ProductOwner1).description",
					ProductOwner.class
			).getSingleResult();
			assertThat( result ).isInstanceOf( ProductOwner1.class );
			assertThat( ( (ProductOwner1) result ).getDescription().getText() ).isEqualTo( "description" );
			inspector.assertNumberOfJoins( 0, 3 );
		} );
	}

	@Test
	public void testTreatedEntityJoin(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Product result = session.createQuery(
					"from Product p " +
							"join ProductOwner owner on p.ownerId = owner.id " +
							"join treat(owner as ProductOwner1).description",
					Product.class
			).getSingleResult();
			assertThat( result.getOwner() ).isInstanceOf( ProductOwner1.class );
			assertThat( ( (ProductOwner1) result.getOwner() ).getDescription().getText() ).isEqualTo( "description" );
			inspector.assertNumberOfJoins( 0, 3 );
		} );
	}

	@Test
	public void testBasicProperty(SessionFactoryScope scope) {
		final SQLStatementInspector inspector = scope.getCollectingStatementInspector();
		inspector.clear();
		scope.inTransaction( session -> {
			final Product result = session.createQuery(
					"from Product p " +
							"join treat(p.owner AS ProductOwner2) as own2 " +
							"where own2.basicProp = 'basic_prop'",
					Product.class
			).getSingleResult();
			assertThat( result.getOwner() ).isInstanceOf( ProductOwner2.class );
			assertThat( ( (ProductOwner2) result.getOwner() ).getBasicProp() ).isEqualTo( "basic_prop" );
			inspector.assertNumberOfJoins( 0, 2 );
		} );
	}

	@Entity( name = "Product" )
	public static class Product {
		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne
		@JoinColumn( name = "owner_id" )
		private ProductOwner owner;

		@Column( name = "owner_id", insertable = false, updatable = false )
		private Integer ownerId;

		public Product() {
		}

		public Product(ProductOwner owner) {
			this.owner = owner;
		}

		public ProductOwner getOwner() {
			return owner;
		}
	}

	@Entity( name = "ProductOwner" )
	@Inheritance( strategy = InheritanceType.JOINED )
	public static class ProductOwner {
		@Id
		@GeneratedValue
		private Integer id;
	}

	@Entity( name = "ProductOwner1" )
	public static class ProductOwner1 extends ProductOwner {
		@ManyToOne
		private Description description;

		public ProductOwner1() {
		}

		public ProductOwner1(Description description) {
			this.description = description;
		}

		public Description getDescription() {
			return description;
		}
	}

	@Entity( name = "ProductOwner2" )
	public static class ProductOwner2 extends ProductOwner {
		@ManyToOne
		private Description description;

		private String basicProp;

		public ProductOwner2() {
		}

		public ProductOwner2(String basicProp) {
			this.basicProp = basicProp;
		}

		public String getBasicProp() {
			return basicProp;
		}
	}

	@Entity( name = "Description" )
	public static class Description {
		@Id
		@GeneratedValue
		public Integer id;

		public String text;

		public Description() {
		}

		public Description(String text) {
			this.text = text;
		}

		public String getText() {
			return text;
		}
	}
}
