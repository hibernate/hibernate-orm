/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.factory.puUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import org.junit.After;
import org.junit.Test;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.hibernate.testing.TestForIssue;

import static org.junit.Assert.assertEquals;

public class CompositeIdDerivedIdWithIdClassTest extends BaseEntityManagerFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				ShoppingCart.class,
				LineItem.class
		};
	}

	@After
	public void cleanup() {
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.createQuery( "delete from LineItem" ).executeUpdate();
		em.createQuery( "delete from Cart" ).executeUpdate();
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-11328")
	public void testMergeTransientIdManyToOne() throws Exception {
		ShoppingCart transientCart = new ShoppingCart( "cart1" );
		transientCart.addLineItem( new LineItem( 0, "description2", transientCart ) );

		// assertion for HHH-11274 - checking for exception
		//final Object identifier = new PersistenceUnitUtilImpl( sessionFactory() ).getIdentifier( transientCart.getLineItems().get( 0 ) );
		final Object identifier = entityManagerFactory().getPersistenceUnitUtil().getIdentifier(
				transientCart.getLineItems()
						.get( 0 )
		);

		// merge ID with transient many-to-one
		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.merge( transientCart );
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		ShoppingCart updatedCart = em.find( ShoppingCart.class, "cart1" );
		// assertion for HHH-11274 - checking for exception
		// new PersistenceUnitUtilImpl( sessionFactory() ).getIdentifier( transientCart.getLineItems().get( 0 ) );
		entityManagerFactory().getPersistenceUnitUtil().getIdentifier( transientCart.getLineItems().get( 0 ) );
		assertEquals( 1, updatedCart.getLineItems().size() );
		assertEquals( "description2", updatedCart.getLineItems().get( 0 ).getDescription() );
		em.getTransaction().commit();
		em.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-10623")
	public void testMergeDetachedIdManyToOne() throws Exception {
		ShoppingCart cart = new ShoppingCart("cart1");

		EntityManager em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.persist( cart );
		em.getTransaction().commit();
		em.close();

		// cart is detached now
		LineItem lineItem = new LineItem( 0, "description2", cart );
		cart.addLineItem( lineItem );

		// merge lineItem with an ID with detached many-to-one
		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		em.merge(lineItem);
		em.getTransaction().commit();
		em.close();

		em = getOrCreateEntityManager();
		em.getTransaction().begin();
		ShoppingCart updatedCart = em.find( ShoppingCart.class, "cart1" );
		assertEquals( 1, updatedCart.getLineItems().size() );
		assertEquals("description2", updatedCart.getLineItems().get( 0 ).getDescription());
		em.getTransaction().commit();
		em.close();
	}

	@Entity(name = "Cart")
	public static class ShoppingCart {
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
			lineItems.add(lineItem);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ShoppingCart that = (ShoppingCart) o;
			return Objects.equals( id, that.id );
		}

		@Override
		public int hashCode() {
			return Objects.hash(id);
		}
	}

	@Entity(name = "LineItem")
	@IdClass(LineItem.Pk.class)
	public static class LineItem {

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
			if (this == o) return true;
			if (!(o instanceof LineItem)) return false;
			LineItem lineItem = (LineItem) o;
			return Objects.equals(getSequenceNumber(), lineItem.getSequenceNumber()) &&
					Objects.equals(getCart(), lineItem.getCart());
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
				if (this == o) return true;
				if (!(o instanceof Pk)) return false;
				Pk pk = (Pk) o;
				return Objects.equals(sequenceNumber, pk.sequenceNumber) &&
						Objects.equals(cart, pk.cart);
			}

			@Override
			public int hashCode() {
				return Objects.hash(sequenceNumber, cart);
			}
		}
	}
}
