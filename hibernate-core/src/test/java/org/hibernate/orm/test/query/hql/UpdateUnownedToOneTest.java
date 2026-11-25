package org.hibernate.orm.test.query.hql;

import org.hibernate.query.SemanticException;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A bulk update cannot assign the unowned side of a one-to-one association,
 * since it has no foreign key.
 */
@DomainModel(annotatedClasses = {
		UpdateUnownedToOneTest.Cart.class,
		UpdateUnownedToOneTest.PurchasedOrder.class
})
@SessionFactory
@JiraKey("HHH-14459")
class UpdateUnownedToOneTest {

	@Entity(name = "Cart")
	@Table(name = "carts")
	static class Cart {
		@Id
		Long id;
		String description;
		@OneToOne(mappedBy = "cart", fetch = FetchType.LAZY)
		PurchasedOrder purchasedOrder;
	}

	@Entity(name = "PurchasedOrder")
	@Table(name = "purchased_orders")
	static class PurchasedOrder {
		@Id
		Long id;
		@OneToOne(fetch = FetchType.LAZY)
		@JoinColumn(name = "cart_id")
		Cart cart;
		@Embedded
		Details details;
	}

	@Embeddable
	static class Details {
		String description;
	}

	@BeforeEach
	void createData(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final var cart = new Cart();
			cart.id = 1L;
			cart.description = "cart";
			session.persist( cart );
			final var order = new PurchasedOrder();
			order.id = 1L;
			order.cart = cart;
			session.persist( order );
		} );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.dropData();
	}

	@Test
	void assignUnownedSide(SessionFactoryScope scope) {
		assertThatThrownBy( () -> scope.inTransaction( session -> session
				.createMutationQuery( "update Cart c set c.purchasedOrder = :order where c.id = 1" )
				.setParameter( "order", null )
				.executeUpdate() ) )
				.hasRootCauseInstanceOf( SemanticException.class )
				.hasStackTraceContaining( "purchasedOrder" );
		assertUnchanged( scope );
	}

	@Test
	void assignThroughUnownedSide(SessionFactoryScope scope) {
		assertThatThrownBy( () -> scope.inTransaction( session -> session
				.createMutationQuery( "update Cart c set c.purchasedOrder.id = 2 where c.id = 1" )
				.executeUpdate() ) )
				.hasRootCauseInstanceOf( SemanticException.class )
				.hasStackTraceContaining( "purchasedOrder" );
		assertUnchanged( scope );
	}

	@Test
	void assignThroughUnownedSideAndEmbeddable(SessionFactoryScope scope) {
		assertThatThrownBy( () -> scope.inTransaction( session -> session
				.createMutationQuery( "update Cart c set c.purchasedOrder.details.description = 'wrong' where c.id = 1" )
				.executeUpdate() ) )
				.hasRootCauseInstanceOf( SemanticException.class )
				.hasStackTraceContaining( "purchasedOrder" );
		assertUnchanged( scope );
	}

	@Test
	void assignOwningSide(SessionFactoryScope scope) {
		final int count = scope.fromTransaction( session -> session
				.createMutationQuery( "update PurchasedOrder o set o.cart = null where o.id = 1" )
				.executeUpdate() );
		assertThat( count ).isEqualTo( 1 );
		scope.inTransaction( session -> assertThat( session.find( PurchasedOrder.class, 1L ).cart ).isNull() );
	}

	private static void assertUnchanged(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			assertThat( session.find( Cart.class, 1L ).description ).isEqualTo( "cart" );
			assertThat( session.find( PurchasedOrder.class, 1L ).cart.id ).isEqualTo( 1L );
		} );
	}
}
