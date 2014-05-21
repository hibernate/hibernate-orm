//$Id: LineItem.java 4806 2004-11-25 14:37:00Z steveebersole $
package org.hibernate.test.cid;
import java.io.Serializable;

/**
 * @author Gavin King
 */
public class LineItem {
	public static class Id implements Serializable {
		private String customerId;
		private int orderNumber;
		private String productId;

		public Id(String customerId, int orderNumber, String productId) {
			this.customerId = customerId;
			this.orderNumber = orderNumber;
			this.productId = productId;
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
		 * @return Returns the productId.
		 */
		public String getProductId() {
			return productId;
		}
		/**
		 * @param productId The productId to set.
		 */
		public void setProductId(String productId) {
			this.productId = productId;
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
			return customerId.hashCode() + orderNumber + productId.hashCode();
		}
		public boolean equals(Object other) {
			if (other instanceof Id) {
				Id that = (Id) other;
				return that.customerId.equals(this.customerId) &&
					that.productId.equals(this.productId) &&
					that.orderNumber == this.orderNumber;
			}
			else {
				return false;
			}
		}
	}

	private Id id = new Id();
	private int quantity;
	private Order order;
	private Product product;

	public LineItem(Order o, Product p) {
		this.order = o;
		this.product = p;
		this.id.orderNumber = o.getId().getOrderNumber();
		this.id.customerId = o.getId().getCustomerId();
		this.id.productId = p.getProductId();
		o.getLineItems().add(this);
	}

	public LineItem() {}

	/**
	 * @return Returns the order.
	 */
	public Order getOrder() {
		return order;
	}
	/**
	 * @param order The order to set.
	 */
	public void setOrder(Order order) {
		this.order = order;
	}
	/**
	 * @return Returns the product.
	 */
	public Product getProduct() {
		return product;
	}
	/**
	 * @param product The product to set.
	 */
	public void setProduct(Product product) {
		this.product = product;
	}
	/**
	 * @return Returns the quantity.
	 */
	public int getQuantity() {
		return quantity;
	}
	/**
	 * @param quantity The quantity to set.
	 */
	public void setQuantity(int quantity) {
		this.quantity = quantity;
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
}
