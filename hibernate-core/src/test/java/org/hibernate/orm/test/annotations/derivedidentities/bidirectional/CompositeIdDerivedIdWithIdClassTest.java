/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.annotations.derivedidentities.bidirectional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hibernate.jpa.internal.PersistenceUnitUtilImpl;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ImplicitListAsBagProvider;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SettingProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

import static org.hibernate.cfg.AvailableSettings.DEFAULT_LIST_SEMANTICS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Andrea Boriero
 */
@ServiceRegistry(
		settingProviders = @SettingProvider(
				settingName = DEFAULT_LIST_SEMANTICS,
				provider = ImplicitListAsBagProvider.class
		)
)
@DomainModel(
		annotatedClasses = {
				CompositeIdDerivedIdWithIdClassTest.ShoppingCart.class,
				CompositeIdDerivedIdWithIdClassTest.LineItem.class
		}
)
@SessionFactory
public class CompositeIdDerivedIdWithIdClassTest {

	@AfterEach
	public void cleanup(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-11328")
	public void testMergeTransientIdManyToOne(SessionFactoryScope scope) {
		ShoppingCart transientCart = new ShoppingCart( "cart1" );
		transientCart.addLineItem( new LineItem( 0, "description2", transientCart ) );

		// assertion for HHH-11274 - checking for exception
		final Object identifier = new PersistenceUnitUtilImpl( scope.getSessionFactory() ).getIdentifier( transientCart.getLineItems()
																												.get( 0 ) );

		// merge ID with transient many-to-one
		scope.inTransaction(
				session ->
						session.merge( transientCart )
		);

		scope.inTransaction(
				session -> {
					ShoppingCart updatedCart = session.get( ShoppingCart.class, "cart1" );
					// assertion for HHH-11274 - checking for exception
					new PersistenceUnitUtilImpl( scope.getSessionFactory() )
							.getIdentifier( transientCart.getLineItems().get( 0 ) );

					assertEquals( 1, updatedCart.getLineItems().size() );
					assertEquals( "description2", updatedCart.getLineItems().get( 0 ).getDescription() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-10623")
	public void testMergeDetachedIdManyToOne(SessionFactoryScope scope) {
		ShoppingCart cart = new ShoppingCart( "cart1" );

		scope.inTransaction(
				session ->
						session.persist( cart )
		);

		// cart is detached now
		LineItem lineItem = new LineItem( 0, "description2", cart );
		cart.addLineItem( lineItem );

		// merge lineItem with an ID with detached many-to-one
		scope.inTransaction(
				session ->
						session.merge( lineItem )
		);

		scope.inTransaction(
				session -> {
					ShoppingCart updatedCart = session.get( ShoppingCart.class, "cart1" );
					assertEquals( 1, updatedCart.getLineItems().size() );
					assertEquals( "description2", updatedCart.getLineItems().get( 0 ).getDescription() );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12007")
	public void testBindTransientEntityWithTransientKeyManyToOne(SessionFactoryScope scope) {

		ShoppingCart cart = new ShoppingCart( "cart" );
		LineItem item = new LineItem( 0, "desc", cart );

		scope.inTransaction(
				session -> {
					String cartId = session.createQuery(
							"select c.id from Cart c left join c.lineItems i where i = :item",
							String.class
					).setParameter( "item", item ).uniqueResult();

					assertNull( cartId );

					assertFalse( session.contains( item ) );
					assertFalse( session.contains( cart ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12007")
	public void testBindTransientEntityWithPersistentKeyManyToOne(SessionFactoryScope scope) {
		ShoppingCart cart = new ShoppingCart( "cart" );
		LineItem item = new LineItem( 0, "desc", cart );

		scope.inTransaction(
				session -> {
					session.persist( cart );
					String cartId = session.createQuery(
							"select c.id from Cart c left join c.lineItems i where i = :item",
							String.class
					).setParameter( "item", item ).uniqueResult();

					assertNull( cartId );

					assertFalse( session.contains( item ) );
					assertTrue( session.contains( cart ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12007")
	public void testBindTransientEntityWithDetachedKeyManyToOne(SessionFactoryScope scope) {

		ShoppingCart cart = new ShoppingCart( "cart" );
		LineItem item = new LineItem( 0, "desc", cart );

		scope.inTransaction(
				session -> {
					String cartId = session.createQuery(
							"select c.id from Cart c left join c.lineItems i where i = :item",
							String.class
					).setParameter( "item", item ).uniqueResult();

					assertNull( cartId );

					assertFalse( session.contains( item ) );
					assertFalse( session.contains( cart ) );
				}
		);
	}

	@Test
	@JiraKey(value = "HHH-12007")
	public void testBindTransientEntityWithCopiedKeyManyToOne(SessionFactoryScope scope) {

		ShoppingCart cart = new ShoppingCart( "cart" );
		LineItem item = new LineItem( 0, "desc", new ShoppingCart( "cart" ) );

		scope.inTransaction(
				session -> {
					String cartId = session.createQuery(
							"select c.id from Cart c left join c.lineItems i where i = :item",
							String.class
					).setParameter( "item", item ).uniqueResult();

					assertNull( cartId );

					assertFalse( session.contains( item ) );
					assertFalse( session.contains( cart ) );
				}
		);
	}

	@Entity(name = "Cart")
	public static class ShoppingCart implements Serializable {
		@Id
		@Column(name = "id", nullable = false)
		private String id;

		@OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
		private List<LineItem> lineItems = new ArrayList<>();

		protected ShoppingCart() {
		}

		public ShoppingCart(String id) {
			this.id = id;
		}

		public List<LineItem> getLineItems() {
			return lineItems;
		}

		public void addLineItem(LineItem lineItem) {
			lineItems.add( lineItem );
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}
			ShoppingCart that = (ShoppingCart) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash( id );
		}
	}

	@Entity(name = "LineItem")
	@IdClass(LineItem.Pk.class)
	public static class LineItem implements Serializable {

		@Id
		@Column(name = "item_seq_number", nullable = false)
		private Integer sequenceNumber;

		@Column(name = "description")
		private String description;

		@Id
		@ManyToOne
		@JoinColumn(name = "cart_id")
		private ShoppingCart cart;

		protected LineItem() {
		}

		public LineItem(Integer sequenceNumber, String description, ShoppingCart cart) {
			this.sequenceNumber = sequenceNumber;
			this.description = description;
			this.cart = cart;
		}

		public Integer getSequenceNumber() {
			return sequenceNumber;
		}

		public ShoppingCart getCart() {
			return cart;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof LineItem ) ) {
				return false;
			}
			LineItem lineItem = (LineItem) o;
			return Objects.equals( getSequenceNumber(), lineItem.getSequenceNumber() ) &&
					Objects.equals( getCart(), lineItem.getCart() );
		}

		@Override
		public int hashCode() {
			return Objects.hash( getSequenceNumber(), getCart() );
		}

		public String getDescription() {
			return description;
		}

		public static class Pk implements Serializable {
			public Integer sequenceNumber;
			public String cart;

			@Override
			public boolean equals(Object o) {
				if ( this == o ) {
					return true;
				}
				if ( !( o instanceof Pk ) ) {
					return false;
				}
				Pk pk = (Pk) o;
				return Objects.equals( sequenceNumber, pk.sequenceNumber ) &&
						Objects.equals( cart, pk.cart );
			}

			@Override
			public int hashCode() {
				return Objects.hash( sequenceNumber, cart );
			}
		}
	}
}
