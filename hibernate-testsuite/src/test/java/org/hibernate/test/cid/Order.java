//$Id: Order.java 4806 2004-11-25 14:37:00Z steveebersole $
package org.hibernate.test.cid;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

/**
 * @author Gavin King
 */
public class Order {
	public static class Id implements Serializable {
		private String customerId;
		private int orderNumber;

		public Id(String customerId, int orderNumber) {
			this.customerId = customerId;
			this.orderNumber = orderNumber;
		}
		public Id() {}

		/**
		 * @return Returns the customerId.
		 */
		public String getCustomerId() {
			return customerId;
		}
		/**
		 * @param customerId The customerId to set.
		 */
		public void setCustomerId(String customerId) {
			this.customerId = customerId;
		}
		/**
		 * @return Returns the orderNumber.
		 */
		public int getOrderNumber() {
			return orderNumber;
		}
		/**
		 * @param orderNumber The orderNumber to set.
		 */
		public void setOrderNumber(int orderNumber) {
			this.orderNumber = orderNumber;
		}
		public int hashCode() {
			return customerId.hashCode() + orderNumber;
		}
		public boolean equals(Object other) {
			if (other instanceof Id) {
				Id that = (Id) other;
				return that.customerId.equals(this.customerId) &&
					that.orderNumber == this.orderNumber;
			}
			else {
				return false;
			}
		}
	}

	private Id id = new Id();
	private Calendar orderDate;
	private Customer customer;
	private Collection lineItems = new ArrayList();
	private BigDecimal total;

	public Order(Customer customer) {
		this.customer = customer;
		this.id.customerId = customer.getCustomerId();
		this.id.orderNumber = customer.getOrders().size();
		customer.getOrders().add(this);
	}

	public Order() {}

	/**
	 * @return Returns the customer.
	 */
	public Customer getCustomer() {
		return customer;
	}
	/**
	 * @param customer The customer to set.
	 */
	public void setCustomer(Customer customer) {
		this.customer = customer;
	}
	/**
	 * @return Returns the lineItems.
	 */
	public Collection getLineItems() {
		return lineItems;
	}
	/**
	 * @param lineItems The lineItems to set.
	 */
	public void setLineItems(Collection lineItems) {
		this.lineItems = lineItems;
	}
	/**
	 * @return Returns the orderDate.
	 */
	public Calendar getOrderDate() {
		return orderDate;
	}
	/**
	 * @param orderDate The orderDate to set.
	 */
	public void setOrderDate(Calendar orderDate) {
		this.orderDate = orderDate;
	}
	/**
	 * @return Returns the total.
	 */
	public BigDecimal getTotal() {
		return total;
	}
	/**
	 * @param total The total to set.
	 */
	public void setTotal(BigDecimal total) {
		this.total = total;
	}
	/**
	 * @return Returns the id.
	 */
	public Id getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Id id) {
		this.id = id;
	}

	public LineItem generateLineItem( Product product, int quantity ) {
		LineItem li = new LineItem( this, product );
		li.setQuantity( quantity );
		lineItems.add( li );
		return li;
	}
}
