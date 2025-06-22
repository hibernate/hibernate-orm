/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.callbacks.xml.complete;

import java.util.List;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */

@SuppressWarnings("JUnitMalformedDeclaration")
@DomainModel(
		annotatedClasses = { Product.class, Order.class, LineItemSuper.class, LineItem.class},
		xmlMappings = "mappings/callbacks/complete.xml"
)
@SessionFactory
public class ListenerCompleteXmlTests {
	public static final String[] LISTENER_ABC = { ListenerA.NAME, ListenerB.NAME, ListenerC.NAME };
	public static final String[] LISTENER_BC = { ListenerB.NAME, ListenerC.NAME };

	@Test
	void testSimplePersist(SessionFactoryScope scope) {
	final Product product = new Product( 1, "987654321", 123 );
	scope.inTransaction( (session) -> {
		session.persist( product );
		assertThat( product.getPrePersistCallbacks() ).containsExactly( LISTENER_ABC );
	} );
	assertThat( product.getPostPersistCallbacks() ).containsExactly( LISTENER_ABC );
}

	@Test
	void testCascadingPersist(SessionFactoryScope scope) {
		final Product product = new Product( 1, "987654321", 123 );
		final Order order = new Order( 1, 246 );
		final LineItem lineItem = new LineItem( 1, order, product, 2 );
		order.addLineItem( lineItem );

		scope.inTransaction( (session) -> {
			session.persist( product );
			session.persist( order );

			assertThat( product.getPrePersistCallbacks() ).containsExactly( LISTENER_ABC );
			assertThat( order.getPrePersistCallbacks() ).containsExactly( LISTENER_ABC );
			assertThat( lineItem.getPrePersistCallbacks() ).containsExactly( LISTENER_BC );
		} );

		assertThat( product.getPostPersistCallbacks() ).containsExactly( LISTENER_ABC );
		assertThat( order.getPostPersistCallbacks() ).containsExactly( LISTENER_ABC );
		assertThat( lineItem.getPostPersistCallbacks() ).containsExactly( LISTENER_BC );
	}

	@Test
	void testSimpleRemove(SessionFactoryScope scope) {
		final Product product = new Product( 1, "987654321", 123 );
		scope.inTransaction( (session) -> {
			session.persist( product );
		} );
		scope.inTransaction( (session) -> {
			session.remove( product );
			assertThat( product.getPreRemoveCallbacks() ).containsExactly( LISTENER_ABC );
		} );
		assertThat( product.getPostRemoveCallbacks() ).containsExactly( LISTENER_ABC );
	}

	@Test
	void testCascadingRemove(SessionFactoryScope scope) {
		final Product product = new Product( 1, "987654321", 123 );
		final Order order = new Order( 1, 246 );
		final LineItem lineItem = new LineItem( 1, order, product, 2 );
		order.addLineItem( lineItem );

		scope.inTransaction( (session) -> {
			session.persist( product );
			session.persist( order );
		} );

		scope.inTransaction( (session) -> {
			session.remove( order );
			assertThat( order.getPreRemoveCallbacks() ).containsExactly( LISTENER_ABC );
			assertThat( lineItem.getPreRemoveCallbacks() ).containsExactly( LISTENER_BC );
		} );

		assertThat( order.getPostRemoveCallbacks() ).containsExactly( LISTENER_ABC );
		assertThat( lineItem.getPostRemoveCallbacks() ).containsExactly( LISTENER_BC );
	}

	@Test
	void testSimpleUpdate(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Product product = new Product( 1, "987654321", 123 );
			session.persist( product );
		} );


		final Product updated = scope.fromTransaction( (session) -> {
			final Product product = session.find( Product.class, 1 );

			assertThat( product.getPreUpdateCallbacks() ).isEmpty();
			assertThat( product.getPostUpdateCallbacks() ).isEmpty();

			product.setCost( 789 );

			return product;
		} );

		assertThat( updated.getPreUpdateCallbacks() ).containsExactly( LISTENER_ABC );
		assertThat( updated.getPostUpdateCallbacks() ).containsExactly( LISTENER_ABC );
	}

	@Test
	void testLoading(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			final Product product = new Product( 1, "987654321", 123 );
			final Order order = new Order( 1, 246 );
			final LineItem lineItem = new LineItem( 1, order, product, 2 );
			order.addLineItem( lineItem );

			session.persist( product );
			session.persist( order );
		} );

		scope.inTransaction( (session) -> {
			final Product product = session.find( Product.class, 1 );
			assertThat( product.getPostLoadCallbacks() ).containsExactly( LISTENER_ABC );
		} );

		scope.inTransaction( (session) -> {
			final List<Product> products = session.createSelectionQuery( "from Product", Product.class ).list();
			products.forEach( (product) -> {
				assertThat( product.getPostLoadCallbacks() ).containsExactly( LISTENER_ABC );
			} );
		} );

		scope.inTransaction( (session) -> {
			final LineItem lineItem = session.find( LineItem.class, 1 );
			assertThat( lineItem.getPostLoadCallbacks() ).containsExactly( LISTENER_BC );
			assertThat( lineItem.getOrder().getPostLoadCallbacks() ).containsExactly( LISTENER_ABC );
		} );
	}

	@AfterEach
	void dropData(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}
}
