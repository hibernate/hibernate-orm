/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.results;

import java.util.List;

import javax.money.Monetary;
import javax.money.MonetaryAmount;

import jakarta.persistence.Tuple;

import org.hibernate.testing.util.uuid.SafeRandomUUIDGenerator;
import org.hibernate.query.TypedTupleTransformer;

import org.hibernate.testing.orm.domain.StandardDomainModel;
import org.hibernate.testing.orm.domain.retail.CardPayment;
import org.hibernate.testing.orm.domain.retail.LineItem;
import org.hibernate.testing.orm.domain.retail.Name;
import org.hibernate.testing.orm.domain.retail.Order;
import org.hibernate.testing.orm.domain.retail.Product;
import org.hibernate.testing.orm.domain.retail.SalesAssociate;
import org.hibernate.testing.orm.domain.retail.Vendor;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( standardModels = StandardDomainModel.RETAIL )
@SessionFactory
@Jira( "https://hibernate.atlassian.net/browse/HHH-15133" )
public class ResultsShapeTests {
	@Test
	public void testSimpleEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<?> orders = session.createQuery( "select o from Order o" ).list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}

	@Test
	public void testTypedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Order> orders = session.createQuery( "select o from Order o", Order.class ).list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}

	@Test
	public void testArrayTypedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Object[]> orders = session.createQuery( "select o from Order o", Object[].class ).list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Object[].class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Object[].class );
		} );
	}

	@Test
	public void testTupleTypedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final List<Tuple> orders = session.createQuery( "select o from Order o", Tuple.class ).list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Tuple.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Tuple.class );
		} );
	}

	@Test
	public void testDuplicatedTypedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select o from LineItem i join i.order o";
			final List<Order> orders = session.createQuery( hql, Order.class ).list();
			// because we select the entity and specify it as the result type, the results are de-duped
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}

	@Test
	public void testDuplicatedArrayTypedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select o from LineItem i join i.order o";
			final List<Object[]> orders = session.createQuery( hql, Object[].class ).list();
			// because we select the entity again, but here specify the full array as the result type - the results are not de-duped
			assertThat( orders ).hasSize( 3 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Object[].class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Object[].class );
		} );
	}

	@Test
	public void testDuplicatedTupleTypedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select o from LineItem i join i.order o";
			final List<Tuple> orders = session.createQuery( hql, Tuple.class ).list();
			// Tuple is a special case or Object[] - not de-duped
			assertThat( orders ).hasSize( 3 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Tuple.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Tuple.class );
		} );
	}

	@Test
	public void testTupleTransformedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select o from Order o";
			final List<Order> orders = session.createQuery( hql, Order.class )
					.setTupleTransformer( (tuple, aliases) -> (Order) tuple[ 0 ] )
					.list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );

		scope.inTransaction( (session) -> {
			final String hql = "select o from Order o";
			final List<Order> orders = session.createQuery( hql )
					.setTupleTransformer( (tuple, aliases) -> tuple[ 0 ] )
					.list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}

	@Test
	public void testDuplicatedTupleTransformedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select o from LineItem i join i.order o";
			final List<Order> orders = session.createQuery( hql, Order.class )
					.setTupleTransformer( (tuple, aliases) -> (Order) tuple[ 0 ] )
					.list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );

		scope.inTransaction( (session) -> {
			final String hql = "select o from LineItem i join i.order o";
			final List<Order> orders = session.createQuery( hql )
					.setTupleTransformer( (tuple, aliases) -> tuple[ 0 ] )
					.list();
			// only 2 orders
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}


	private final TypedTupleTransformer<Order> ORDER_TUPLE_TRANSFORMER = new TypedTupleTransformer<>() {
		@Override
		public Class<Order> getTransformedType() {
			return Order.class;
		}

		@Override
		public Order transformTuple(Object[] tuple, String[] aliases) {
			return (Order) tuple[0];
		}
	};

	@Test
	public void testTypedTupleTransformedEntitySelection(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final String hql = "select o from Order o";
			final List<Order> orders = session.createQuery( hql, Order.class )
					.setTupleTransformer( ORDER_TUPLE_TRANSFORMER )
					.list();
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );

		scope.inTransaction( (session) -> {
			final String hql = "select o from Order o";
			final List<Order> orders = session.createQuery( hql )
					.setTupleTransformer( ORDER_TUPLE_TRANSFORMER )
					.list();
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}

	@Test
	public void testDuplicatedTypedTupleTransformedEntitySelection(SessionFactoryScope scope) {
		final String hql = "select o from LineItem i join i.order o";

		scope.inTransaction( (session) -> {
			final List<Order> orders = session.createQuery( hql, Order.class )
					.setTupleTransformer( (tuple, aliases) -> (Order) tuple[0] )
					.list();
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );

		scope.inTransaction( (session) -> {
			final List<Order> orders = session.createQuery( hql )
					.setTupleTransformer( (tuple, aliases) -> tuple[ 0 ] )
					.list();
			assertThat( orders ).hasSize( 2 );
			assertThat( orders.get( 0 ) ).isInstanceOf( Order.class );
			assertThat( orders.get( 1 ) ).isInstanceOf( Order.class );
		} );
	}

	@BeforeEach
	public void prepareTestData(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Vendor acme = new Vendor( 1, "Acme, Inc.", null );
			session.persist( acme );

			final Product widget = new Product( 1, SafeRandomUUIDGenerator.safeRandomUUID(), acme );
			session.persist( widget );

			final SalesAssociate associate = new SalesAssociate( 1, new Name( "John", "Doe" ) );
			session.persist( associate );

			final MonetaryAmount oneDollar = Monetary.getDefaultAmountFactory()
					.setNumber( 1 )
					.setCurrency( "USD" )
					.create();

			final CardPayment payment1 = new CardPayment( 1, 123, oneDollar );
			session.persist( payment1 );
			final Order order1 = new Order( 1, payment1, associate );
			session.persist( order1 );
			final LineItem lineItem11 = new LineItem( 11, widget, 1, oneDollar, order1 );
			session.persist( lineItem11 );
			final LineItem lineItem12 = new LineItem( 12, widget, 1, oneDollar, order1 );
			session.persist( lineItem12 );

			final CardPayment payment2 = new CardPayment( 2, 321, oneDollar );
			session.persist( payment2 );
			final Order order2 = new Order( 2, payment2, associate );
			session.persist( order2 );
			final LineItem lineItem2 = new LineItem( 2, widget, 1, oneDollar, order2 );
			session.persist( lineItem2 );
		} );
	}

	@AfterEach
	public void dropTestData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
